package fr.ema.dedal.componentinspector.explorer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import dedal.DedalDiagram;
import fr.ema.dedal.componentinspector.classloader.FolderLoader;
import fr.ema.dedal.componentinspector.classloader.JarLoader;
import fr.ema.dedal.componentinspector.inspector.JarInspector;
import dedal.impl.DedalFactoryImpl;

public class Explorer {

	static final Logger logger = Logger.getLogger(Explorer.class);
	static final String xml = ".xml";
	static final String jar = ".jar";
	static final String war = ".war";

	private Explorer() {}

	/**
	 * 
	 * This method aims at generating a dedal diagram and more precisely a configuration of component classes from existing jar libraries
	 * 
	 * @param path : this is the path of the library to inspect
	 * @return 
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 */
	public static DedalDiagram generate(String singlePath, String sdslPath) {
		try 
		{
			/**
			 * we instanciate a new Dedal diagram that we will return as the result of this method
			 */
			DedalDiagram result = new DedalFactoryImpl().createDedalDiagram();
			result.setName("genDedalDiag");

			inspectJar(singlePath, sdslPath, result);
			return result;
		} 
		catch (IOException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	public static List<DedalDiagram> generateAll(String libPath)
	{
		List<DedalDiagram> result = new ArrayList<>();
		
		/**
		 * We know the library hierarchy tree
		 * 
		 * Library
		 * |
		 * |----Owners
		 * |	|
		 * |	|----Repositories
		 */
		try {
			List<URI> repos = new ArrayList<>();
			for(URI o : FolderLoader.loadFolder(Paths.get(libPath)))
			{
				repos.addAll(FolderLoader.loadFolder(Paths.get(o)));
			}
			if(logger.isInfoEnabled())
			{
				repos.forEach(r -> logger.info(r.toString()));
				logger.info("number of repositories that are going to be inspected : " + repos.size());
			}
			for(URI folder : repos)
			{
				File f = new File(folder);
				if(f.isDirectory())
				{
					List<URI> xmlFiles = recursivelyGetFileURIs(folder, xml);
					List<URI> jarFiles = recursivelyGetFileURIs(folder, jar, war);
					URL[] urlsToLoad = new URL[jarFiles.size()];
					for(int i = 0; i<jarFiles.size(); i++)
					{
						urlsToLoad[i]=jarFiles.get(i).toURL();
					}
					Map<URI, List<Class<?>>> classes;
					JarLoader jarloarder = new JarLoader(urlsToLoad);
					try
					{
						classes = jarloarder.loadClasses(f.getName());
					}catch (Exception e) {
						logger.error(e.getMessage(),e);
					}
					
					System.out.println("coucou");
				}
			}
			

			return result;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return Collections.emptyList();
	}

	/**
	 * Loads and inspects the jar file.
	 * @param singlePath
	 * @param sdslPath
	 * @param result
	 * @throws MalformedURLException
	 */
	private static void inspectJar(String singlePath, String sdslPath, DedalDiagram result)
			throws MalformedURLException {
		URL[] urlToLoad=new URL[]{Paths.get(singlePath).toUri().toURL()};
		JarLoader jarloader = new JarLoader(urlToLoad);

		/**
		 * Let's extract some component classes.
		 */
		JarInspector jarInspector = new JarInspector(jarloader);
		jarInspector.generate(result, sdslPath);
	}
	
	/**
	 * 
	 * @param folder
	 * @return
	 * @throws IOException 
	 */
	private static List<URI> recursivelyGetFileURIs(URI folder, String ... fileExtensions) throws IOException
	{
		List<URI> result = new ArrayList<>();
		File f1 = new File(folder);
		List<URI> tempURIs = new ArrayList<>();
		if(f1.isDirectory() && !f1.getName().contains("lib") && !f1.getName().contains("dependenc"))
		{
			tempURIs = FolderLoader.loadFolder(Paths.get(folder));
			for(URI uri : tempURIs)
			{
				File f = new File(uri);
				if(f.isFile())
					for(String ext : fileExtensions)
					{
						if(f.getName().endsWith(ext))
							result.add(uri);
					}
				else if(f.isDirectory())
				{
					result.addAll(recursivelyGetFileURIs(uri, fileExtensions));
				}
			}
		}
		return result;
	}

}
