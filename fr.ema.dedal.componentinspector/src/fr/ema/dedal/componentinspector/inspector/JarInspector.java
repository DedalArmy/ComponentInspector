package fr.ema.dedal.componentinspector.inspector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;

import dedal.Assembly;
import dedal.Attribute;
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
	Map<CompRole, Class<?>> roleToClass = null;
	Map<CompClass, Class<?>> compToClass = null;
	Map<Interface, Class<?>> intToClass = null;
	Map<Interface, List<Interface>> candidateInterfaces = null;
	Map<CompClass, List<CompRole>> typeHierarchy = null;
	Map<CompClass, Map<Interface, Class<?>>> compIntToType = null;
	Map<CompRole, Map<Interface, Class<?>>> roleIntToType = null;
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
		this.compToClass = new HashMap<>();
		this.roleToClass = new HashMap<>();
		this.compIntToType = new HashMap<>();
		this.roleIntToType = new HashMap<>();
		this.typeHierarchy = new HashMap<>();
		this.intToClass = new HashMap<>();
		this.candidateInterfaces = new HashMap<>();
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

			mapComponentClasses(dedalDiagram, repo, classList, config);
			setConfigConnections(config);
			
			instantiateInteractions(asm);
			setAssmConnections(asm, config.getConfigConnections());

			setSpecificationFromConfiguration(repo, spec, config);
			dedalDiagram.getArchitectureDescriptions().add(spec);
			config.getImplements().add(spec);

		});		
	}

	/**
	 * 
	 * @param repo 
	 * @param dedalDiagram 
	 * @param spec
	 * @param config
	 */
	private void setSpecificationFromConfiguration(Repository repo, Specification spec, Configuration config) {
		this.compToClass.forEach((key,value) -> {
			RoleExtractor re = new RoleExtractor(value, key, this.intToClass, repo);
			List<CompRole> extractedRoles = re.calculateSuperTypes();
			this.roleIntToType.putAll(re.getRoleToIntToType());
			this.intToClass.putAll(re.getIntToType());
			spec.getSpecComponents().addAll(extractedRoles);
			key.getRealizes().addAll(extractedRoles);
			
		});
		setSpecConnections(spec, config);
	}

	/**
	 * @param spec
	 * @param config
	 */
	private void setSpecConnections(Specification spec, Configuration config) {
		config.getConfigConnections().forEach(ccon -> {
			RoleConnection tempRoleConnection = new DedalFactoryImpl().createRoleConnection();
			CompClass cclient = ccon.getClientClassElem();
			CompClass cserver = ccon.getServerClassElem();
			List<CompRole> realizedByClient = cclient.getRealizes();
			List<CompRole> realizedByServer = cserver.getRealizes();
			tempRoleConnection = findClientRole(tempRoleConnection, ccon.getClientIntElem(), realizedByClient);
			tempRoleConnection = findServerRole(tempRoleConnection, cserver, realizedByServer);
			spec.getSpecConnections().add(tempRoleConnection);
		});
	}

	/**
	 * @param tempRoleConnection
	 * @param cserver
	 * @param realizedByServer
	 * @return 
	 */
	private RoleConnection findServerRole(RoleConnection tempRoleConnection, CompClass cserver, List<CompRole> realizedByServer) {
		for(CompRole rserver : realizedByServer)
		{
			for(Interaction inter : cserver.getCompInterfaces())
			{
				for(Interaction inter2 : rserver.getCompInterfaces())
				{
					Class<?> icli = this.roleIntToType.get(tempRoleConnection.getClientCompElem()).get(tempRoleConnection.getClientIntElem());
					Class<?> iserv = this.roleIntToType.get(rserver).get(inter2);
					if(this.roleIntToType.get(rserver).get(inter2).isAssignableFrom(this.compIntToType.get(cserver).get(inter))
							&& icli.isAssignableFrom(iserv))
					{
						tempRoleConnection.setServerCompElem(rserver);
						tempRoleConnection.setServerIntElem(inter2);
						return tempRoleConnection;
					}
				}
			}
		}
		return tempRoleConnection;
	}

	/**
	 * @param tempRoleConnection
	 * @param cclient
	 * @param realizedByClient
	 * @return 
	 */
	private RoleConnection findClientRole(RoleConnection tempRoleConnection, Interaction iclient, List<CompRole> realizedByClient) {
		for(CompRole rclient: realizedByClient)
		{
				for(Interaction inter2 : rclient.getCompInterfaces())
				{
					if(this.intToClass.get(iclient).equals(this.roleIntToType.get(rclient).get(inter2)))
					{
						tempRoleConnection.setClientCompElem(rclient);
						tempRoleConnection.setClientIntElem(inter2);
						return tempRoleConnection;
					}
				}
		}
		return tempRoleConnection;
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
	private void setConfigConnections(Configuration config) {
		/**
		 * Setting configurations
		 */
		Map<Interaction, List<Interaction>> mapServerToClients = new HashMap<>();
		Map<Interface, ClassConnection> intServToCon = new HashMap<>();
		config.getConfigConnections().forEach(con -> {
			CompClass client = con.getClientClassElem();
			CompClass server = con.getServerClassElem();
			
			/**
			 * calculate which is the corresponding required interface
			 */
			String attrName = con.getProperty().replaceAll("\"", "");
			Attribute tempAttr = new DedalFactoryImpl().createAttribute();
			for(Attribute a : client.getAttributes()) {
				if(a.getName().equals(attrName))
				{
					tempAttr = a;
					break;
				}
			}
			final Attribute attr = tempAttr; //to be used in the following loop, attr must be final.
			
			Map<Interface, Class<?>> interfaceToClassMapServer = this.compIntToType.get(client);
			interfaceToClassMapServer.forEach((key, clazz)  -> {
				if(clazz.getCanonicalName().equals(attr.getType()))
				{
					con.setClientIntElem(key);
				}
			});
			final Class<?> clientClass = (this.compIntToType.get(client)).get(con.getClientIntElem());
			
			/**
			 * matching the corresponding server interface
			 */
			server.getCompInterfaces().forEach(ci -> {
				Class<?> ciClass = (this.compIntToType.get(server)).get(ci);
				if(clientClass.isAssignableFrom(ciClass))
				{
					con.setServerIntElem(ci);
				}
			});
			mapServerToClients.put(con.getServerIntElem(), new ArrayList<>());
			intServToCon.put((Interface)con.getServerIntElem(),con);
		});
		
		/**
		 * Connect clients to the most abstract provided interface as possible
		 */
		config.getConfigConnections().forEach(con -> mapServerToClients.get(con.getServerIntElem()).add(con.getClientIntElem()));
		Map<ClassConnection, Interface> transform = new HashMap<>();
		mapServerToClients.forEach((key,value) -> transform.putAll(getMostAbstractProvidedInterfaces(config, key, value)));
		
		intServToCon.forEach((initServ, con) -> {
			CompClass server = con.getServerClassElem();
			Interaction finalServ = transform.get(con);
			server.getCompInterfaces().add(finalServ);
			con.setServerIntElem(finalServ);
			if(!server.getCompInterfaces().contains(finalServ))
				server.getCompInterfaces().add(finalServ);
			if(!connected(initServ, config.getConfigConnections()))
			{
				server.getCompInterfaces().remove(initServ);
			}
		});
	}

	/**
	 * @param config
	 * @param server
	 * @param client
	 * @return 
	 */
	private Map<ClassConnection, Interface> getMostAbstractProvidedInterfaces(Configuration config, Interaction server, List<Interaction> client) {
		Map<ClassConnection,Interface> result = new HashMap<>();
		if(client.size()==1) //if a server is connected to a single client
		{
			if( (server instanceof Interface) && (client.get(0) instanceof Interface))
			{
				Interface iserv = (Interface) server;
				Interface icli = (Interface) client.get(0);
				if((this.intToClass.get(icli)).isAssignableFrom(this.intToClass.get(iserv)) 
						&& !(this.intToClass.get(icli)).equals(this.intToClass.get(iserv)))
				{
					result.put(this.findConnection(config,iserv,icli),assignNewServerInterface(iserv, icli));
				}
			}
		}
		else if (client.size()>1)
		{
			result.putAll(buildAbstractInterfacesMultiplyConnected(config, server, client));
		}
		else // a server interface of a connection cannot be connected to 0 client interface
			logger.error("Something went terribly wrong while assembling connections");
		return result;
	}

	/**
	 * @param config
	 * @param server
	 * @param client
	 * @param result
	 * @return 
	 */
	private Map<? extends ClassConnection, ? extends Interface> buildAbstractInterfacesMultiplyConnected(Configuration config, Interaction server,
			List<Interaction> client) {
		Map<ClassConnection,Interface> result = new HashMap<>();
		Interface iserv = (Interface) server;
		if(comparable(client))
		{
			for(Interaction icli : client)
			{
				result.put(this.findConnection(config, iserv, (Interface) icli),assignNewServerInterface(iserv, (Interface) icli));
			}
		}
		else
		{
			result.putAll(this.decoupleInterfaces(iserv, client, config.getConfigConnections()));
		}
		return result;
	}

	/**
	 * 
	 * @param config
	 * @param iserv
	 * @param icli
	 * @return
	 */
	private ClassConnection findConnection(Configuration config, Interface iserv, Interface icli) {
		ClassConnection result = new DedalFactoryImpl().createClassConnection();
		for(ClassConnection cc : config.getConfigConnections())
		{
			if(iserv.equals(cc.getServerIntElem()) && icli.equals(cc.getClientIntElem()))
			{
				return cc;
			}
		}
		return result;
	}

	/**
	 * @param iserv
	 * @param icli
	 */
	private Interface assignNewServerInterface(Interface iserv, Interface icli) {
		Interface intToAssign = this.getMostSatisfyingInterface(iserv, icli);
		if(!intToAssign.equals(iserv))
		{
			return intToAssign;
		}
		return iserv;
	}

	/**
	 * 
	 * @param iserv
	 * @param value
	 * @param configConnections
	 * @return 
	 */
	private Map<ClassConnection, Interface> decoupleInterfaces(Interface iserv, List<Interaction> value,
			List<ClassConnection> configConnections) {
		Map<ClassConnection, Interface> result = new HashMap<>();
		List<ClassConnection> targetedConnections = new ArrayList<>();
		for(ClassConnection cc : configConnections)
		{
			if(cc.getServerIntElem().equals(iserv))
			{
				targetedConnections.add(cc);
			}
		}
		for(ClassConnection cc : targetedConnections)
		{
			List<Interaction> comparables =  assembleComparable((Interface) cc.getClientClassElem(), value);
			Interface intToAssign = this.getMostSatisfyingInterface(iserv, comparables);
			if(!intToAssign.equals(iserv))
				result.put(cc,intToAssign);
		}
		return result;
	}

	private boolean connected(Interface iserv, List<ClassConnection> configConnections) {
		for(ClassConnection cc : configConnections)
		{
			if(cc.getServerIntElem().equals(iserv))
				return true;
		}
		return false;
	}

	private List<Interaction> assembleComparable(Interface inter, List<Interaction> interfaces) {
		List<Interaction> result = new ArrayList<>();
		for(Interaction i : interfaces)
		{
			Interface tempI = (Interface) i;
			if((this.intToClass.get(tempI).isAssignableFrom(this.intToClass.get(inter)))
					|| (this.intToClass.get(inter).isAssignableFrom(this.intToClass.get(tempI))))
				result.add(tempI);
		}
		return result;
	}

	/**
	 * 
	 * @param iserv
	 * @param value
	 * @return
	 */
	private Interface getMostSatisfyingInterface(Interface iserv, List<Interaction> value) {
		List<Interface> interfaces = new ArrayList<>();
		for(Interaction i : value)
		{
			interfaces.add(this.getMostSatisfyingInterface(iserv, (Interface) i));
		}
		Interface result = interfaces.get(0);
		for(Interface i1 : interfaces)
		{
			if(this.intToClass.get(result).isAssignableFrom(this.intToClass.get(i1)))
				result = i1;
		}
		return result;
	}

	private Boolean comparable(List<Interaction> value) {
		for(Interaction i1 : value)
		{
			for(Interaction i2 : value)
			{
				if(!((this.intToClass.get((Interface) i1).isAssignableFrom(this.intToClass.get((Interface) i2)))
						||(this.intToClass.get((Interface) i2).isAssignableFrom(this.intToClass.get((Interface) i1)))))
					return Boolean.FALSE;
			}
		}
		return Boolean.TRUE;
	}

	private Interface getMostSatisfyingInterface(Interface iserv, Interface icli) {
		Class<?> baseCliClass = this.intToClass.get(icli);
		Interface result = iserv;
		for(Interface i : this.candidateInterfaces.get(iserv))
		{
			if(this.intToClass.get(i).isAssignableFrom(this.intToClass.get(result)) && baseCliClass.isAssignableFrom(this.intToClass.get(i)))
			{
				if(this.intToClass.get(i).equals(baseCliClass))
					return i;
				result = i;	
			}
		}
		return result;
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
	private void mapComponentClasses(DedalDiagram dedalDiagram, Repository repo, List<Class<?>> classList, Configuration config) {
		for(CompClass tempCompClass : config.getConfigComponents())
		{
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
					ci.mapComponentClass(tempCompClass);
					this.compToClass.put(tempCompClass, c);
					this.compIntToType.put(tempCompClass, ci.getInterfaceToClassMap());
					this.intToClass.putAll(ci.getInterfaceToClassMap());
					this.candidateInterfaces.putAll(ci.getCandidateInterfaces());
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