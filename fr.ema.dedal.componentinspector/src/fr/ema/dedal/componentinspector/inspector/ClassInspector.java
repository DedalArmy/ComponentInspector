/**
 * 
 */
package fr.ema.dedal.componentinspector.inspector;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.util.EcoreUtil;
import dedal.Attribute;
import dedal.CompClass;
import dedal.CompRole;
import dedal.CompType;
import dedal.Configuration;
import dedal.DIRECTION;
import dedal.DedalDiagram;
import dedal.Interface;
import dedal.Repository;
import dedal.impl.DedalFactoryImpl;

/**
 * @author Alexandre Le Borgne
 *
 */
public class ClassInspector extends InterfaceInspector {

	static final Logger logger = Logger.getLogger(ClassInspector.class);

	/**
	 * Constructor
	 * @param object
	 * @param config 
	 * @param repo 
	 */
	public ClassInspector(Class<?> object, DedalDiagram dd, Configuration config, Repository repo) {
		super(object, dd, config, repo);
	}

	/**
	 * Generates the dedal CompClass artefacts 
	 * @return true if no problem occurred 
	 */
	public Boolean generateFromScratch()
	{
		if(logger.isInfoEnabled())
			logger.info("\t" + this.getObjectToInspect().getName() + " -- " + this.getObjectToInspect().getTypeName());

		CompType tempCompType = new DedalFactoryImpl().createCompType();
		tempCompType.setName(this.getObjectToInspect().getTypeName().replace('.', '_')+"_Type");
		CompClass tempCompClass = new DedalFactoryImpl().createCompClass();
		tempCompClass.setName(this.getObjectToInspect().getSimpleName());
		this.getConfiguration().getComptypes().add(tempCompType);
		this.getConfiguration().getConfigComponents().add(tempCompClass);

		try {
			fillConfigComponent(tempCompType, tempCompClass);
		} 
		catch (SecurityException | NoClassDefFoundError | TypeNotPresentException e) {
			e.getMessage();
			return Boolean.FALSE;
		}
		this.getRepository().getComponents().add((tempCompType));
		this.getRepository().getComponents().add((tempCompClass));
		return Boolean.TRUE;
	}

	/**
	 * 
	 * @param compClass 
	 * @return
	 */
	public Boolean generateFromExistingDeployment(CompClass compClass) {
		if(logger.isInfoEnabled())
			logger.info("\t" + this.getObjectToInspect().getName() + " -- " + this.getObjectToInspect().getTypeName());

			CompType tempCompType = new DedalFactoryImpl().createCompType();
			tempCompType.setName(this.getObjectToInspect().getTypeName().replace('.', '_')+"_Type");
			this.getConfiguration().getComptypes().add(tempCompType);

			try {
				fillConfigComponent(tempCompType, compClass);
			} 
			catch (SecurityException | NoClassDefFoundError | TypeNotPresentException e) {
				logger.error(e.getMessage(), e);
				return Boolean.FALSE;
			}
			this.getRepository().getComponents().add((tempCompType));
//			this.getRepository().getComponents().add((compClass));
			return Boolean.TRUE;
	}

	/**
	 * @param tempCompType
	 * @param tempCompClass
	 * @throws SecurityException
	 */
	private void fillConfigComponent(CompType tempCompType, CompClass tempCompClass) {
		getFields(tempCompClass, this.getObjectToInspect());
		List<Interface> providedInterfaces = this.calculateProvidedInterfaces();
		tempCompClass.getCompInterfaces().addAll(providedInterfaces);
		this.resetExploredMethods();
		List<Interface> requiredInterfaces = this.calculateRequiredInterfaces();
		tempCompClass.getCompInterfaces().addAll(requiredInterfaces);
		tempCompType.getCompInterfaces().addAll(EcoreUtil.copyAll(providedInterfaces));
		tempCompType.getCompInterfaces().addAll(EcoreUtil.copyAll(requiredInterfaces));
		tempCompType.getCompInterfaces().forEach(ci -> ci.setName(ci.getName()+"_"+tempCompType.getName()));
		tempCompClass.setImplements(tempCompType);
	}

	/**
	 * @param tempCompClass
	 * @throws SecurityException
	 */
	private void getFields(CompClass tempCompClass, Class<?> objectToInspect) {
		Field[] fields = objectToInspect.getDeclaredFields().length>0? objectToInspect.getDeclaredFields() : null;
		if (fields != null) {
			for (Field field : fields) {
				if(logger.isInfoEnabled())
					logger.info("\t\t" + field.toGenericString());
				Attribute tempAttribute = new DedalFactoryImpl().createAttribute();
				tempAttribute.setName(field.getName());
				tempAttribute.setType(field.getType().getCanonicalName());
				tempCompClass.getAttributes().add(tempAttribute);
			}
		}
		if(!objectToInspect.getSuperclass().equals(Object.class) &&
				objectToInspect.getSuperclass()!=null)
			getFields(tempCompClass, objectToInspect.getSuperclass());
	}

	/**
	 * This method intends to calculate provided interfaces with a satisfying granularity.
	 * @param tempCompClass
	 */
	public List<Interface> calculateProvidedInterfaces() {
		List<Interface> result = this.calculateProvidedInterfaces(this.getObjectToInspect());
		result.forEach(pi -> {
			String piName = pi.getName();
			String objName = "I" + this.getObjectToInspect().getSimpleName();
			String adId = (piName.equals(objName))?"":"_"+this.getObjectToInspect().getSimpleName();
			pi.setName(piName+adId);
		});
		return result;
	}

	/**
	 * This method intends to calculate provided interfaces with a satisfying granularity.
	 * @param tempCompClass
	 */
	public List<Interface> calculateProvidedInterfaces(Class<?> objectToInspect) {
		List<Interface> result = new ArrayList<>();
		result.addAll(calculateInterfaces(objectToInspect));
		result.forEach(i -> i.setDirection(DIRECTION.PROVIDED));
		return result;
	}

	/**
	 * This method intends to calculate required interfaces with a satisfying granularity.
	 */
	public List<Interface> calculateRequiredInterfaces() {
		this.resetExploredMethods();
		List<Interface> result = calculateRequiredInterfaces(this.getObjectToInspect());
		result.forEach(ri -> {
			String riName = ri.getName();
			String objName = this.getObjectToInspect().getSimpleName();
			String adId = (riName.equals("I" + objName))?"":"_"+this.getObjectToInspect().getSimpleName();
			ri.setName(ri.getName()+adId);
		});
		return result;
	}
	
	/**
	 * 
	 * @param objectToInspect
	 * @return
	 */
	public List<Interface> calculateRequiredInterfaces(Class<?> objectToInspect) {
		List<Interface> result = new ArrayList<>();

		if(objectToInspect.getDeclaredFields().length>0)
		{
			for(int i = 0; i<objectToInspect.getDeclaredFields().length; i++)
			{
				Field f = objectToInspect.getDeclaredFields()[i];
				List<Interface> interfaces;
				Class<?> type = f.getType();
				if(!(type.isEnum() || type.isPrimitive()))
				{
					if(type.isArray())
						interfaces = calculateInterfaces(type.getComponentType());
					else
						interfaces = calculateInterfaces(type);
					result.addAll(interfaces);
				}
			}
		}
		if(!(Object.class).equals(objectToInspect.getSuperclass()) && 
				objectToInspect.getSuperclass()!=null)
		{
			result.addAll(calculateRequiredInterfaces(objectToInspect.getSuperclass()));
		}
		result.forEach(i -> i.setDirection(DIRECTION.REQUIRED));
		return result;
	}

	public List<CompRole> calculateSuperTypes() {
		this.resetExploredMethods();
		return calculateSuperTypes(this.getObjectToInspect());
	}
	
	public List<CompRole> calculateSuperTypes(Class<?> objectToInspect) {

//		this.resetExploredMethods();
		List<CompRole> result = new ArrayList<>();
		this.setObjectToInspect(objectToInspect);
		CompRole tempRole = new DedalFactoryImpl().createCompRole();
		tempRole.setName(objectToInspect.getSimpleName() + "_role");
		tempRole.getCompInterfaces().addAll(this.calculateProvidedInterfaces());
		tempRole.getCompInterfaces().addAll(this.calculateRequiredInterfaces());
		tempRole.getCompInterfaces().forEach(ci -> ci.setName(ci.getName() + "_role"));
		result.add(tempRole);
		if(!getObjectToInspect().getSuperclass().equals(Object.class) &&
				getObjectToInspect().getSuperclass() != null)
		{
			result.addAll(calculateSuperTypes(getObjectToInspect().getSuperclass()));
		}
		return result;
	}

}
