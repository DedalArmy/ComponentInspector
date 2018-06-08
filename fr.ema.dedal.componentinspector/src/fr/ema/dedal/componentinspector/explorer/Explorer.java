package fr.ema.dedal.componentinspector.explorer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import org.apache.log4j.Logger;
import dedal.DedalDiagram;
import fr.ema.dedal.componentinspector.classloader.FolderLoader;
import fr.ema.dedal.componentinspector.classloader.JarLoader;
import fr.ema.dedal.componentinspector.inspector.JarInspector;
import dedal.impl.DedalFactoryImpl;

public class Explorer {

	static final Logger logger = Logger.getLogger(Explorer.class);
	static final String XML = ".xml";
	static final String JAR = ".jar";
	static final String WAR = ".war";
	static final String BEANS = "<beans";
	static final String BEAN = "<bean";

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

			inspectJarFromSinglePath(singlePath, sdslPath, result);
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
				if(new File(o).isDirectory())
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
				File parent = new File(f.getParent());
				if(f.isDirectory())
				{
					generateDedalDiagram(result, folder, f, parent);
				}
			}
			return result;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return Collections.emptyList();
	}

	/**
	 * @param result
	 * @param folder
	 * @param f
	 * @param parent
	 */
	private static void generateDedalDiagram(List<DedalDiagram> result, URI folder, File f, File parent) {
		JarLoader jarLoader = null;
		try {
			DedalDiagram dd = new DedalFactoryImpl().createDedalDiagram();
			dd.setName(parent.getName() + "." + f.getName());
			List<URI> xmlFiles = recursivelyGetFileURIs(folder, XML);
			List<URI> jarFiles = recursivelyGetFileURIs(folder, JAR, WAR);
			
			List<URI> subFolders = recursivelyGetFolders(folder);

			URL[] jarUrlsToLoad = new URL[jarFiles.size()];
			for(int i = 0; i<jarFiles.size(); i++)
			{
				jarUrlsToLoad[i]=jarFiles.get(i).toURL();
			}

//			URL[] urlsToLoad = {};
			URL[] urlsToLoad = new URL[subFolders.size()];
			for(int i = 0; i<subFolders.size(); i++)
			{
				urlsToLoad[i]=subFolders.get(i).toURL();
			}
			jarLoader = new JarLoader(urlsToLoad , jarUrlsToLoad);
//			List<URI> xmlFiles = jarLoader.getFilesFromJars(XML);
			List<URI> xmlSpringFiles = scanFiles(xmlFiles, BEANS, BEAN);
			JarInspector jarInspector = new JarInspector(jarLoader);
			jarInspector.generate(dd, xmlSpringFiles);
			result.add(dd);
		} catch(Exception | Error e) 
		{
			logger.error("the Dedal diagram could not be extracted... reconstruction ended with error " + e.getCause());
		}
		finally {
			try {
				jarLoader.close();
			} catch (IOException | NullPointerException e) {
				logger.error("Error while closing jarLoader", e);
			}
		}
	}

	private static List<URI> recursivelyGetFolders(URI folder) {
		List<URI> result = new ArrayList<>();
		File f1 = new File(folder);
		if(f1.isDirectory())
		{
			result.add(folder);
			List<URI> tempURIs;
			try {
				tempURIs = FolderLoader.loadFolder(Paths.get(folder));
				for(URI uri : tempURIs)
				{
					result.addAll(recursivelyGetFolders(uri));
				}
			} catch (IOException e) {
				logger.error("An error occured while getting the full list of subfolders");
			}
		}
		return result;
	}

	/**
	 * Loads and inspects the jar file.
	 * @param singlePath
	 * @param sdslPath
	 * @param result
	 * @throws MalformedURLException
	 */
	private static void inspectJarFromSinglePath(String singlePath, String sdslPath, DedalDiagram result)
			throws MalformedURLException {
		URL[] urlToLoad=new URL[]{Paths.get(singlePath).toUri().toURL()};
		JarLoader jarloader = new JarLoader(new URL[]{}, urlToLoad);

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
		if(f1.isDirectory())
		{
			List<URI> tempURIs = FolderLoader.loadFolder(Paths.get(folder));
			for(URI uri : tempURIs)
			{
				getFileURI(result, uri, fileExtensions);
			}
		}
		return result;
	}

	/**
	 * @param result
	 * @param uri
	 * @param fileExtensions
	 * @throws IOException
	 */
	private static void getFileURI(List<URI> result, URI uri, String... fileExtensions) throws IOException {
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
	
	/**
	 * 
	 * @param queries
	 * @param files
	 * @return
	 */
	private static List<URI> scanFiles(List<URI> files, String ... queries) {
		List<URI> result = new ArrayList<>();
		for(URI uri : files)
		{
			File f = new File(uri);
			if(existsInFile(f, queries))
				result.add(uri);
		}
		return result;
	}

	private static Boolean existsInFile(File f, String[] queries) {
			for(String query : queries)
			{
				if(!existsInFile(f, query))
					return Boolean.FALSE;
			}
		return Boolean.TRUE;
	}

	private static Boolean existsInFile(File f, String query) {
		try (Scanner input = new Scanner(f);){
			while(input.hasNextLine())
			{
				if(input.nextLine().contains(query))
					return Boolean.TRUE;
			}
        } catch (Exception e) {
            logger.error("A problem occured while scanning file " + f.getName() + ". Scanning ended up with " + e.getCause());
        }			
		return Boolean.FALSE;
	}

}
