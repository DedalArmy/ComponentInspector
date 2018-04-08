/**
 * 
 */
package fr.ema.dedal.componentinspector.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
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

import dedal.ArchitectureDescription;
import dedal.Assembly;
import dedal.Configuration;
import dedal.DedalDiagram;
import dedal.Specification;
import fr.ema.dedal.componentinspector.explorer.Explorer;
import fr.ema.dedal.componentinspector.metrics.Metrics;
import fr.ema.dedal.xtext.DedalADLStandaloneSetup;

/**
 * @author Alexandre Le Borgne
 *
 */
public class Main {

	private static final String UTF_8 = "UTF-8";

	static final Logger logger = Logger.getLogger(Main.class);

	private static final String DEFAULT_LIB = "I:\\MiniSandBox";
	private static final String LIB = "-lib";
	private static final String PATH = "-path";
	private static final String SDSL = "-sdsl";
	private static final String OUT = "-out";
	
	/**
	 * Main program for loading and inspecting java components
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) {

		Date date = new Date();
		
		/*
		 * CONFIGURATION OF THE LIBRARY PATH
		 */
		String libPath = "";
		String singlePath = "";
		String sdslPath = "";
		String out = "./generated_metrics_results/metrics"+date.toString().replaceAll(":", "_").replaceAll(" ", "")+".csv";
		
		laucnhReconstruction(args, libPath, singlePath, sdslPath, out);
	}

	private static void laucnhReconstruction(String[] args, String libPath, String singlePath, String sdslPath,
			String out) {
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
			if(tempArgs.contains(LIB) && tempArgs.contains(OUT))
			{
				libPath = tempArgs.get(tempArgs.indexOf(LIB)+1);
				out = tempArgs.get(tempArgs.indexOf(OUT)+1);
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

		try {
			exportMetrics(out);
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			logger.error("Failed to write in output file " + out);
		}
		
		if(logger.isInfoEnabled())
		{
			logger.info("The end");
		}
	}

	private static void exportMetrics(String out) throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter(out, UTF_8);
		writer.println("Metric,Value");
		writer.println("NbSpringXMLFiles," + Metrics.getNbSpringXML());
		writer.println("NbEmptySpringXMLFiles," + Metrics.getNbEmptySpringXML());
		writer.println("NbClasses," + Metrics.getNbClasses());
		writer.println("NbFailedClasses," + Metrics.getNbFailedClass());
		writer.println("NbSpecs," + Metrics.getNbSpecs());
		writer.println("NbCompRoles," + Metrics.getNbCompsRoles());
		writer.println("NbConfigs," + Metrics.getNbConfs());
		writer.println("NbCompClasses," + Metrics.getNbCompsClasses());
		writer.println("NbClassMultiRole," + Metrics.getNbCompClassMultiRoles());
		writer.println("NbAssms," + Metrics.getNbAssembs());
		writer.println("NbCompInsts," + Metrics.getNbCompsInst());
		writer.println("NbSpecEqualConfigs," + Metrics.getNbSpecsEqualsConf());
		writer.println("NbInterfaces," + Metrics.getNbInterfaces());
		writer.println("NbSplitInterface," + Metrics.getNbSplitInterfaces());
		writer.println("NbAbstractedInterface," + Metrics.getNbAbstractedInterfaceType());
		writer.println("NbConnections," + Metrics.getNbConnexions());
		writer.println("NbUnconnectedArch," + Metrics.getNbConnexionlessArchis());
		writer.close();
	}

	/**
	 * @param dedalDiagram
	 */
	private static void saveDiagram(DedalDiagram dedalDiagram) {
		Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;

		Injector injector = new DedalADLStandaloneSetup().createInjectorAndDoEMFRegistration();
		ResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);
		resourceSet.setResourceFactoryRegistry(reg);
		

		List<ArchitectureDescription> toRemoveFromDiagram = new ArrayList<>();
		dedalDiagram.getArchitectureDescriptions().forEach(ad -> {
			if(ad instanceof Specification && ((Specification) ad).getSpecComponents().isEmpty() && ((Specification) ad).getSpecConnections().isEmpty())
				toRemoveFromDiagram.add(ad);
			if(ad instanceof Configuration && ((Configuration) ad).getConfigComponents().isEmpty() && ((Configuration) ad).getConfigConnections().isEmpty())
				toRemoveFromDiagram.add(ad);
			if(ad instanceof Assembly && ((Assembly) ad).getAssmComponents().isEmpty() && ((Assembly) ad).getAssemblyConnections().isEmpty())
				toRemoveFromDiagram.add(ad);
		});
		dedalDiagram.getArchitectureDescriptions().removeAll(toRemoveFromDiagram);
		
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
			/*
			 * We need to save resource2 before resource because in case of errors in 
			 * model, resource throws an exception since it verifies the correctness
			 * of the DedalADL syntax.
			 */
			Map<Object,Object> options = new HashMap<>();
			options.put(XtextResource.OPTION_ENCODING, UTF_8);
			options.put(XtextResource.OPTION_SAVE_ONLY_IF_CHANGED, Boolean.TRUE);
			options.put(XtextResource.OPTION_LINE_DELIMITER, ";");
			Map<Object,Object> options2 = new HashMap<>();
			options.put(XtextResource.OPTION_ENCODING, UTF_8);
			options2.put(XtextResource.OPTION_SAVE_ONLY_IF_CHANGED, Boolean.TRUE);
			resource2.save(options2);
			saveDedalAdlFile(dedalDiagram, resource, options, options2);
			
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * @param dedalDiagram
	 * @param resource
	 * @param options
	 * @param options2
	 * @throws IOException
	 */
	private static void saveDedalAdlFile(DedalDiagram dedalDiagram, Resource resource, Map<Object, Object> options,
			Map<Object, Object> options2) throws IOException {
		try {
			resource.save(options);
		} catch (Exception e) {
			resource.delete(options2);
			logger.error("could not generate " + dedalDiagram.getName() +".dedaladl");
		}
	}
}