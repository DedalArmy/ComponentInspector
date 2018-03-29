/**
 * 
 */
package fr.ema.dedal.componentinspector.main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import com.google.inject.Injector;
import dedal.DedalDiagram;
import fr.ema.dedal.componentinspector.explorer.Explorer;
import fr.ema.dedal.xtext.DedalADLStandaloneSetup;

/**
 * @author Alexandre Le Borgne
 *
 */
public class Main {

	static final Logger logger = Logger.getLogger(Main.class);

	private static final String DEFAULT_LIB = "I:\\MiniSandBox";
	private static final String LIB = "-lib";
	private static final String PATH = "-path";
	private static final String SDSL = "-sdsl";
	
	/**
	 * Main program for loading and inspecting java components
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) {

		/*
		 * CONFIGURATION OF THE LIBRARY PATH
		 */
		String libPath = "";
		String singlePath = "";
		String sdslPath = "";
		
		switch (args.length) {
		case 0:
			libPath = DEFAULT_LIB;
			break;
		case 2:
			libPath = (LIB.equals(args[0]))? args[1] : "";
			break;
		case 4:
			List<String> tempArgs = new ArrayList<>();
			for(String arg : args)
				tempArgs.add(arg);
			if(tempArgs.contains(PATH) && tempArgs.contains(SDSL))
			{
				singlePath = tempArgs.get(tempArgs.indexOf(PATH)+1);
				sdslPath = tempArgs.get(tempArgs.indexOf(SDSL)+1);
			}
			break;

		default:
			break;
		}

		if(logger.isInfoEnabled())
		{
			logger.info("libPath = " + libPath);
			logger.info("singlePath = " + singlePath);
			logger.info("sdslPath = " + sdslPath);
		}		

		List<DedalDiagram> reconstructedArchitectures = new ArrayList<>();
		if(!"".equals(libPath)) {
			reconstructedArchitectures.addAll(Explorer.generateAll(libPath));
		} else 
			if(!("".equals(singlePath) || "".equals(sdslPath)))
			reconstructedArchitectures.add(Explorer.generate(singlePath, sdslPath));
		else
			logger.error("The Dedal diagram could not be generated due to path issues.");

		if(!reconstructedArchitectures.isEmpty())
			for(DedalDiagram dd : reconstructedArchitectures)
			{
				saveDiagram(dd);
			}
		else
			logger.error("No architecture were reconstructed.");
	}

	/**
	 * @param dedalDiagram
	 */
	private static void saveDiagram(DedalDiagram dedalDiagram) {
		Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;

		Injector injector = new DedalADLStandaloneSetup().createInjectorAndDoEMFRegistration();
		ResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);
		resourceSet.setResourceFactoryRegistry(reg);
		
			URI uri = URI.createURI("generated/"+ dedalDiagram.getName() +".dedaladl");
			Resource resource = resourceSet.createResource(uri);
		

		URI uri2 = URI.createURI("generated/"+ dedalDiagram.getName() +".dedal");
		Resource resource2 = new XMIResourceImpl(uri2);

		// Get the first model element and cast it to the right type, in my
		// example everything is hierarchical included in this first node
		resource.getContents().add(dedalDiagram);
		resource2.getContents().add(EcoreUtil.copy(dedalDiagram));

		// now save the content.
		try {
			Map<Object,Object> options = new HashMap<>();
			options.put(XtextResource.OPTION_ENCODING, "UTF-8");
			options.put(XtextResource.OPTION_SAVE_ONLY_IF_CHANGED, Boolean.TRUE);
			options.put(XtextResource.OPTION_LINE_DELIMITER, ";");
			Map<Object,Object> options2 = new HashMap<>();
			options.put(XtextResource.OPTION_ENCODING, "UTF-8");
			options2.put(XtextResource.OPTION_SAVE_ONLY_IF_CHANGED, Boolean.TRUE);
			/*
			 * We need to save resource2 before resource because in case of errors in 
			 * model, resource throws an exception since it verifies the correctness
			 * of the DedalADL syntax.
			 */
			resource2.save(options2);
			try {
				resource.save(options);
			} catch (Exception e) {
				logger.error("could not generate " + dedalDiagram.getName() +".dedaladl");
			}
			
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		if(logger.isInfoEnabled())
			logger.info("The end");
	}
}