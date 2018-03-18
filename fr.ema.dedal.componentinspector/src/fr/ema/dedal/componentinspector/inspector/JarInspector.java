package fr.ema.dedal.componentinspector.inspector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;

import dedal.Assembly;
import dedal.ClassConnection;
import dedal.CompClass;
import dedal.CompInstance;
import dedal.CompRole;
import dedal.Configuration;
import dedal.DedalDiagram;
import dedal.DedalFactory;
import dedal.InstConnection;
import dedal.Interaction;
import dedal.Interface;
import dedal.Repository;
import dedal.RoleConnection;
import dedal.Specification;
import dedal.impl.ClassConnectionImpl;
import dedal.impl.DedalFactoryImpl;
import fr.ema.dedal.componentinspector.classloader.JarLoader;

/**
 * @author Alexandre Le Borgne
 *
 */
public class JarInspector {

	static final Logger logger = Logger.getLogger(JarInspector.class);
	JarLoader jarLoader = null;
	Map<URI, List<Class<?>>> jarMapping = null;
	Map<CompClass, List<CompRole>> typeHierarchy = null;
	DedalFactory factory;

	/**
	 * @param jarLoader
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws FileNotFoundException 
	 */
	public JarInspector(JarLoader jarLoader) {
		super();
		this.jarLoader = jarLoader;
		this.jarMapping = this.jarLoader.loadClasses();
		factory = DedalFactoryImpl.init();
	}

	/**
	 * @return the _JarLoader
	 */
	public JarLoader getJarLoader() {
		return jarLoader;
	}

	/**
	 * @param jarLoader the _JarLoader to set
	 */
	public void setJarLoader(JarLoader jarLoader) {
		this.jarLoader = jarLoader;
	}

	/**
	 * @return the _JarMapping
	 */
	public Map<URI, List<Class<?>>> getJarMapping() {
		return jarMapping;
	}

	/**
	 * @param jarMapping the _JarMapping to set
	 */
	public void setJarMapping(Map<URI, List<Class<?>>> jarMapping) {
		this.jarMapping = jarMapping;
	}

	public void generate(DedalDiagram dedalDiagram) {

		Repository repo = new DedalFactoryImpl().createRepository();
		repo.setName("genRepo");
		dedalDiagram.getRepositories().add(repo);
		this.jarMapping.forEach((uriKey, classList) -> 
		{
			Configuration config = factory.createConfiguration();
			config.setName(new File(uriKey).getName().replace('.', '_')+"_config");
			dedalDiagram.getArchitectureDescriptions().add(config);

			if(logger.isInfoEnabled())
				logger.info("URL : " + uriKey);

			classList.forEach(c -> 
			{
				if(!(c.isInterface()||c.isEnum()||Modifier.isAbstract(c.getModifiers())))
				{
					ClassInspector ci = new ClassInspector(c, dedalDiagram, config, repo);
					ci.generateFromScratch();
				}			
			});
		});
	}

	/**
	 * 
	 * @param dedalDiagram
	 * @param sdslPath
	 */
	public void generate(DedalDiagram dedalDiagram, String sdslPath){

		Repository repo = new DedalFactoryImpl().createRepository();
		repo.setName("genRepo");
		dedalDiagram.getRepositories().add(repo);
		List<EObject> extractedFromSpring = SdslInspector.extractDedalArtifacts(sdslPath);

		this.jarMapping.forEach((uriKey, classList) -> 
		{
			typeHierarchy = new HashMap<>();
			Specification spec = factory.createSpecification();
			spec.setName(new File(uriKey).getName().replace('.', '_')+"_spec");
			Configuration config = factory.createConfiguration();
			config.setName(new File(uriKey).getName().replace('.', '_')+"_config");
			dedalDiagram.getArchitectureDescriptions().add(config);
			Assembly asm = factory.createAssembly();
			asm.setInstantiates(config);
			asm.setName(new File(uriKey).getName().replace('.', '_')+"_asm");
			dedalDiagram.getArchitectureDescriptions().add(asm);

			copyInDiagram(extractedFromSpring, config, asm);

			standardizeNames(config, asm);

			if(logger.isInfoEnabled())
				logger.info("URL : " + uriKey);

			fillDiagram(dedalDiagram, repo, classList, config);
			instantiateInteractions(asm);


			setConfigConnectionsFromExisting(config);
			setAssmConnections(asm, config.getConfigConnections());

			setSpecificationFromConfiguration(spec, config);
			dedalDiagram.getArchitectureDescriptions().add(spec);
			config.getImplements().add(spec);

		});		
	}

	/**
	 * 
	 * @param spec
	 * @param config
	 */
	private void setSpecificationFromConfiguration(Specification spec, Configuration config) {
		Map<CompClass, List<Interface>> mandatoryInterfaces = new HashMap<>();
		config.getConfigConnections().forEach(ccon -> {
			CompClass cclient = ccon.getClientClassElem();
			CompClass cserver = ccon.getServerClassElem();
			if(ccon.getClientIntElem() instanceof Interface && ccon.getServerIntElem() instanceof Interface)
			{
				if(mandatoryInterfaces.containsKey(cclient))
					mandatoryInterfaces.get(cclient).add(((Interface) ccon.getClientIntElem()));
				else {
					List<Interface> list = new ArrayList<>();
					list.add((Interface) ccon.getClientIntElem());
					mandatoryInterfaces.put(cclient, list);
				}
				if(mandatoryInterfaces.containsKey(cserver))
					mandatoryInterfaces.get(cserver).add((Interface) ccon.getServerIntElem());
				else {
					List<Interface> list = new ArrayList<>();
					list.add((Interface) ccon.getServerIntElem());
					mandatoryInterfaces.put(cserver, list);
				}
			}
		});
		setMostSatifyingRoles(spec, mandatoryInterfaces);
		setSpecConnections(spec, config);
	}

	/**
	 * @param spec
	 * @param config
	 */
	private void setSpecConnections(Specification spec, Configuration config) {
		config.getConfigConnections().forEach(ccon -> {
			RoleConnection tempRoleConnection = factory.createRoleConnection();
			CompClass cclient = ccon.getClientClassElem();
			CompClass cserver = ccon.getServerClassElem();
			CompRole rclient = cclient.getRealizes().get(0);
			CompRole rserver = cserver.getRealizes().get(0);
			tempRoleConnection.setClientCompElem(rclient);
			tempRoleConnection.setServerCompElem(rserver);
			for(Interaction inter : cclient.getCompInterfaces())
			{
				for(Interaction inter2 : rclient.getCompInterfaces())
				{
					if((inter instanceof Interface) && (inter2 instanceof Interface)
							&& (((Interface) inter).getType().equals(((Interface) ccon.getClientIntElem()).getType())) && ((Interface) inter).getType().equals(((Interface) inter2).getType()))
					{
						tempRoleConnection.setClientIntElem(inter2);
						break;
					}
				}
			}
			for(Interaction inter : cserver.getCompInterfaces())
			{
				for(Interaction inter2 : rserver.getCompInterfaces())
				{
					if((inter instanceof Interface) && (inter2 instanceof Interface)
							&& ((Interface) inter).getType().equals(((Interface) inter2).getType()))
					{
						tempRoleConnection.setServerIntElem(inter2);
					}
				}
			}
			spec.getSpecConnections().add(tempRoleConnection);
		});
	}

	/**
	 * @param spec
	 * @param mandatoryInterfaces
	 */
	private void setMostSatifyingRoles(Specification spec, Map<CompClass, List<Interface>> mandatoryInterfaces) {
		mandatoryInterfaces.forEach((cclass, list) -> {
			Collection<Interaction> interfaces = EcoreUtil.copyAll(cclass.getCompInterfaces());
			interfaces.forEach(i ->	i.setName(i.getName() + "_role"));
			CompRole tempRole = factory.createCompRole();
			tempRole.getCompInterfaces().addAll(interfaces);
			for(CompRole role : typeHierarchy.get(cclass)) {
				if(isSatisfyingForCompRole(role, list))
				{
					tempRole = role;
				}
			}
			spec.getSpecComponents().add(tempRole);
			cclass.getRealizes().add(tempRole);	
		});
	}

	private Boolean isSatisfyingForCompRole(CompRole role, List<Interface> interfaces)
	{
		for(Interface i : interfaces)
		{
			if(!contains(role.getCompInterfaces(), i))
				return Boolean.FALSE;
		}
		return Boolean.TRUE;
	}

	private boolean contains(EList<Interaction> compInterfaces, Interface i) {
		for(Interaction inter : compInterfaces)
		{
			if(inter instanceof Interface && 
					((Interface) inter).getDirection().equals(i.getDirection()) &&
					((Interface) inter).getType().equals(i.getType()))
			{
				return Boolean.TRUE;
			}
		}
		return Boolean.FALSE;
	}
	/**
	 * 
	 * @param asm
	 * @param eList
	 */
	private void setAssmConnections(Assembly asm, EList<ClassConnection> eList) {
		asm.getAssemblyConnections().forEach(con -> {
			CompInstance client = con.getClientInstElem();
			CompInstance server = con.getServerInstElem();
			eList.forEach(e -> {
				CompClass cclient = e.getClientClassElem();
				CompClass cserver = e.getServerClassElem();
				setIntElem(con, client, server, e, cclient, cserver);
			});
		});
	}

	/**
	 * @param con
	 * @param client
	 * @param server
	 * @param e
	 * @param cclient
	 * @param cserver
	 */
	private void setIntElem(InstConnection con, CompInstance client, CompInstance server, ClassConnection e,
			CompClass cclient, CompClass cserver) {
		if(client.getInstantiates().equals(cclient)
				&& server.getInstantiates().equals(cserver))
		{
			client.getCompInterfaces().forEach(ci -> {
				if(ci instanceof Interface
						&& ((Interface) ci).getInstantiates().equals(e.getClientIntElem()))
					con.setClientIntElem(ci);
			});
			server.getCompInterfaces().forEach(ci -> {
				if(ci instanceof Interface
						&& ((Interface) ci).getInstantiates().equals(e.getServerIntElem()))
					con.setServerIntElem(ci);
			});
		}
	}

	/**
	 * @param config
	 */
	private void setConfigConnectionsFromExisting(Configuration config) {
		config.getConfigConnections().forEach(con -> {
			CompClass client = con.getClientClassElem();
			CompClass server = con.getServerClassElem();
			Map<Interaction, Interaction> interactionMatching = new HashMap<>();
			client.getCompInterfaces().forEach(ci1 ->
			server.getCompInterfaces().forEach(ci2 -> {
				if(ci1 instanceof Interface && ci2 instanceof Interface)
				{
					Interface tmpInt1 = (Interface) ci1;
					Interface tmpInt2 = (Interface) ci2;
					if(tmpInt1.getType().getName().equals(tmpInt2.getType().getName()))
					{
						interactionMatching.put(ci1, ci2);
					}
				}
			})
					);
			connectConfigInteractions(config, con, client, server, interactionMatching);
		});
	}

	/**
	 * @param config
	 * @param con
	 * @param client
	 * @param server
	 * @param interactionMatching
	 */
	private void connectConfigInteractions(Configuration config, ClassConnection con, CompClass client, CompClass server,
			Map<Interaction, Interaction> interactionMatching) {
		Boolean multiple = (interactionMatching.size()>1)? true : false;
		int remaining = interactionMatching.size();
		interactionMatching.forEach((i1,i2) -> {
			if(multiple && remaining > 1)
			{
				ClassConnection cc = new ClassConnectionImpl();
				cc.setClientClassElem(client);
				cc.setServerClassElem(server);
				cc.setClientIntElem(i1);
				cc.setClientIntElem(i2);
				config.getConfigConnections().add(cc);
			}
			else
			{
				con.setClientIntElem(i1);
				con.setServerIntElem(i2);
			}
		});
	}

	/**
	 * @param asm
	 */
	private void instantiateInteractions(Assembly asm) {
		asm.getAssmComponents().forEach(c ->
		c.getInstantiates().getCompInterfaces().forEach(ci -> {
			Interaction tempInteraction = EcoreUtil.copy(ci);
			tempInteraction.setName(ci.getName() + "_" + c.getName());
			if(ci instanceof Interface)
				((Interface) tempInteraction).setInstantiates(((Interface) ci));
			c.getCompInterfaces().add(tempInteraction);
		})
				);
	}

	/**
	 * @param dedalDiagram
	 * @param repo
	 * @param classList
	 * @param config
	 */
	private void fillDiagram(DedalDiagram dedalDiagram, Repository repo, List<Class<?>> classList, Configuration config) {
//		while(!config.getConfigComponents().isEmpty())
		for(CompClass tempCompClass : config.getConfigComponents())
		{
			/*
			 * During the process, the components are pushed into the repository
			 * and thus, they are removed from the configuration
			 */
//			CompClass tempCompClass = config.getConfigComponents().get(0);
			if(logger.isInfoEnabled())
			{
				logger.info("compName : " + tempCompClass.getName());
			}
			for(Class<?> c : classList)
			{
				String compClassName = tempCompClass.getName().replace("\"", "");
				String className = c.getSimpleName();
				if(!(c.isInterface()||c.isEnum()||Modifier.isAbstract(c.getModifiers())) 
						&&  className.equals(compClassName)
						)
				{
					if(logger.isInfoEnabled())
					{
						logger.info("className : " + className);
					}
					ClassInspector ci = new ClassInspector(c, dedalDiagram, config, repo);
					ci.generateFromExistingDeployment(tempCompClass);
					typeHierarchy.put(tempCompClass, ci.calculateSuperTypes());
					logger.info(typeHierarchy);
				}			
			}
		}
	}

	/**
	 * @param extractedFromSpring
	 * @param config
	 * @param asm
	 */
	private void copyInDiagram(List<EObject> extractedFromSpring, Configuration config, Assembly asm) {
		extractedFromSpring.forEach(artefact -> {
			if(artefact instanceof DedalDiagram)
			{
				((DedalDiagram) artefact).getArchitectureDescriptions().forEach(ad -> {
					if(ad instanceof Assembly)
					{
						asm.getAssmComponents().addAll(((Assembly) ad).getAssmComponents());
						asm.getAssemblyConnections().addAll(((Assembly) ad).getAssemblyConnections());
					}
					else if(ad instanceof Configuration)
					{
						config.getConfigComponents().addAll(((Configuration) ad).getConfigComponents());
						config.getConfigConnections().addAll(((Configuration) ad).getConfigConnections());
					}
				});
			}
		});
	}

	/**
	 * @param config
	 * @param asm
	 */
	private void standardizeNames(Configuration config, Assembly asm) {
		asm.getAssmComponents().forEach(c -> 
		c.setName(c.getName().replace("\"", ""))
				);
		config.getConfigComponents().forEach(c -> 
		c.setName(c.getName().replace("\"", ""))
				);
	}

}