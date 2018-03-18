/**
 * 
 */
package fr.ema.dedal.componentinspector.inspector;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import dedal.Configuration;
import dedal.DedalDiagram;
import dedal.Interface;
import dedal.InterfaceType;
import dedal.Parameter;
import dedal.Repository;
import dedal.Signature;
import dedal.impl.DedalFactoryImpl;

/**
 * @author Alexandre Le Borgne
 *
 */
public class InterfaceInspector {

	static final Logger logger = Logger.getLogger(InterfaceInspector.class);

	private Class<?> objectToInspect;
	private DedalDiagram dedaldiagram;
	private Configuration configuration;
	private Repository repository;
	private List<Method> methods = null;
	private List<Method> exploredMethods = null;

	/**
	 * @param config 
	 * @param repo 
	 * 
	 */
	public InterfaceInspector(Class<?> object, DedalDiagram dd, Configuration config, Repository repo) {
		this.objectToInspect = object;
		this.methods = new ArrayList<>();
		this.exploredMethods = new ArrayList<>();
		this.dedaldiagram = dd;
		this.configuration = config;
		this.repository = repo;
	}

	/**
	 * 
	 * @return
	 */
	public DedalDiagram getDedaldiagram() {
		return dedaldiagram;
	}

	/**
	 * 
	 * @param dedaldiagram
	 */
	public void setDedaldiagram(DedalDiagram dedaldiagram) {
		this.dedaldiagram = dedaldiagram;
	}

	/**
	 * 
	 * @return
	 */
	public Configuration getConfiguration() {
		return configuration;
	}

	/**
	 * 
	 * @param configuration
	 */
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * 
	 * @return
	 */
	public Repository getRepository() {
		return repository;
	}

	/**
	 * 
	 * @param repository
	 */
	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	/**
	 * 
	 * @param object
	 */
	public void setObjectToInspect(Class<?> object) {
		this.objectToInspect = object;
	}

	/**
	 * 
	 * @param methods
	 */
	public void setMethods(List<Method> methods) {
		this.methods = methods;
	}

	/**
	 * 
	 * @return
	 */
	public String getName() {
		return this.objectToInspect.getName();
	}

	/**
	 * 
	 * @return
	 */
	public String getSimpleName() {
		return this.objectToInspect.getSimpleName();
	}

	/**
	 * 
	 * @return
	 */
	public Class<?> getObjectToInspect() {
		return objectToInspect;
	}

	/**
	 * 
	 * @return
	 */
	public List<Method> getMethods() {
		return methods;
	}

	/**
	 * 
	 * @return
	 */
	public List<Method> getExploredMethods() {
		return exploredMethods;
	}

	public void resetExploredMethods() {
		exploredMethods = new ArrayList<>();
	}

	/**
	 * 
	 * @return
	 */
	public List<Interface> calculateInterfaces()
	{
		return this.calculateInterfaces(objectToInspect);
	}

	/**
	 * 
	 * @param objectToInspect
	 * @return
	 */
	public List<Interface> calculateInterfaces(Class<?> objectToInspect)
	{
		List<Interface> result = new ArrayList<>();
		if(!(Object.class).equals(objectToInspect.getSuperclass()) && 
				//				!(Comparable.class).equals(objectToInspect.getSuperclass()) &&
				//				!(IEnum.class).equals(objectToInspect.getSuperclass()) &&
				objectToInspect.getSuperclass()!=null)
		{
			List<Interface> superClassInterfaces = calculateInterfaces(objectToInspect.getSuperclass());
			result.addAll(superClassInterfaces);
		}
		if(objectToInspect.getInterfaces().length > 0)
		{
			Class<?>[] interfaces = objectToInspect.getInterfaces();
			for (Class<?> i : interfaces) {
				List<Interface> tempList = this.getDedalInterfaces(i);
				result.addAll(tempList);
			}
		}
		if(objectToInspect.isInterface())
		{
			result.addAll(this.getDedalInterfaces(objectToInspect));
			return result;
		}
		for (Method m : objectToInspect.getDeclaredMethods())
		{
			if(!methods.contains(m))
				methods.add(m);
		}
		this.removeExploredMethods();
		if(!methods.isEmpty())
		{
			Interface derivedInterface = this.deriveInterface("I" + objectToInspect.getSimpleName(), "I" + objectToInspect.getSimpleName() + "_Type",methods);
			result.add(derivedInterface);
		}
		return result;
	}

	/**
	 * 
	 * @param inter
	 * @return
	 */
	private List<Interface> getDedalInterfaces(Class<?> inter)
	{
		List<Interface> result = new ArrayList<>();	
		if(inter.getInterfaces().length > 0)
		{
			Class<?>[] interfaces = inter.getInterfaces();
			for (Class<?> i : interfaces) {
				result.addAll(this.getDedalInterfaces(i));
			}
		}
		if(inter.getMethods().length > 0)
		{
			List<Method> tempMethods = new ArrayList<>();
			for (Method method : inter.getMethods()) {
				tempMethods.add(method);
			}
			result.add(deriveInterface("I" + inter.getSimpleName(), inter.getSimpleName(),tempMethods));
		}
		return result;
	}

	/**
	 * 
	 * @param name
	 * @param typeName
	 * @param tempMethods
	 * @return
	 */
	public Interface deriveInterface(String name, String typeName,List<Method> tempMethods)
	{
		Interface tempInterface = new DedalFactoryImpl().createInterface();
		tempInterface.setName(name);
		InterfaceType tempInterfaceType = new DedalFactoryImpl().createInterfaceType();
		tempInterfaceType.setName(typeName);
		tempInterfaceType.getSignatures().addAll(this.getSignatures(tempMethods));


		if(!existsInRepo(tempInterfaceType))
		{
			if(!existsInConfig(tempInterfaceType))
			{
				configuration.getInterfaceTypes().add(tempInterfaceType);
				tempInterface.setType(tempInterfaceType);
			}
			else {
				tempInterface.setType(this.getInterfaceTypeFromConfig(tempInterfaceType));
			}
			repository.getInterfaceTypes().add(tempInterfaceType);
		}
		else {
			tempInterface.setType(this.getInterfaceTypeFromRepo(tempInterfaceType));
		}

		return tempInterface;
	}

	/**
	 * 
	 * @param tempMethods
	 * @return
	 */
	private List<Signature> getSignatures(List<Method> tempMethods)
	{
		List<Signature> result = new ArrayList<>();
		for (Method m : tempMethods)
		{
			exploredMethods.add(m);
			Signature tempSignature = new DedalFactoryImpl().createSignature();
			tempSignature.setName(m.getName());
			tempSignature.setType(m.getReturnType().getCanonicalName());
			for (int i = 0; i < m.getParameters().length; i++) {
				Parameter tempParameter = new DedalFactoryImpl().createParameter();
				java.lang.reflect.Parameter p = m.getParameters()[i];
				tempParameter.setName(p.getName());
				tempParameter.setType(p.getType().getCanonicalName());
				tempSignature.getParameters().add(tempParameter);
			}
			result.add(tempSignature);
		}
		return result;
	}

	/**
	 * 
	 */
	private void removeExploredMethods()
	{
		List<Method> methodsToRemove = new ArrayList<>();
		for(Method m : methods)
		{
			for(Method em : exploredMethods)
			{
				java.lang.reflect.Parameter[] params1 = m.getParameters();
				java.lang.reflect.Parameter[] params2 = em.getParameters();

				if(m.getName().equals(em.getName()) 
						&& m.getReturnType().equals(em.getReturnType()) 
						&& parameterAreEquals(params1, params2))
				{
					methodsToRemove.add(m);
				}
			}
		}
		methods.removeAll(methodsToRemove);
	}

	/**
	 * 
	 * @param params1
	 * @param params2
	 * @return
	 */
	private boolean parameterAreEquals(java.lang.reflect.Parameter[] params1, java.lang.reflect.Parameter[] params2) {
		if(params1.length == params2.length)
		{
			for(int i = 0; i<params1.length; i++)
			{
				if(!((params1[i].getType().equals(params2[i].getType()))
						&& (params1[i].getName().equals(params2[i].getName()))))
					return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	private boolean existsInConfig(InterfaceType type)
	{
		for(InterfaceType it : configuration.getInterfaceTypes())
		{
			if(it.getName().equals(type.getName()))
				return true;
		}
		return false;
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	private boolean existsInRepo(InterfaceType type)
	{
		for(InterfaceType it : repository.getInterfaceTypes())
		{
			if(it.getName().equals(type.getName()))
				return true;
		}
		return false;
	}

	/**
	 * 
	 * @param interfaceType
	 * @return
	 */
	private InterfaceType getInterfaceTypeFromConfig(InterfaceType interfaceType)
	{
		for(InterfaceType it : configuration.getInterfaceTypes())
		{
			if(it.getName().equals(interfaceType.getName()))
				return it;
		}
		return null;
	}

	/**
	 * 
	 * @param interfaceType
	 * @return
	 */
	private InterfaceType getInterfaceTypeFromRepo(InterfaceType interfaceType) {
		for(InterfaceType it : repository.getInterfaceTypes())
		{
			if(it.getName().equals(interfaceType.getName()))
				return it;
		}
		return null;
	}
}
