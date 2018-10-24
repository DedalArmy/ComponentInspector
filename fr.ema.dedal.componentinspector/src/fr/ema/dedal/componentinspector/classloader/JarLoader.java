/**
 * 
 */
package fr.ema.dedal.componentinspector.classloader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;


import org.apache.log4j.Logger;


/**
 * @author Alexandre Le Borgne
 *
 */
public class JarLoader extends URLClassLoader {
	
	static final Logger logger = Logger.getLogger(JarLoader.class);

	static final String JAR = ".jar";
	static final String WAR = ".war";
	
	private URL[] jars;
	
	/**
	 * Parameterized constructor
	 * @param urls
	 */
	public JarLoader(URL[] urls, URL[] jarUrls) {
		super(urls);
		jars = jarUrls;
		this.initURLs();
	}
	
	private void initURLs()
	{
		for(URL url : jars)
		{
			this.addURL(url);
			JarEntry entry = null;
			try (InputStream in = new FileInputStream(url.getFile());
					JarInputStream jar = new JarInputStream(in);) {
				entry = jar.getNextJarEntry();
				while(entry != null)
				{	
					if (entry.isDirectory()) {
						String tempURL = url.toString() + "!/" + entry.getName();
						this.addURL(URI.create(tempURL).toURL());
					}
					entry = jar.getNextJarEntry();
				}
			} catch (IOException e) {
				logger.error("Error While initializing JarLoader", e);
			}
		}
	}

	public List<URI> getFilesFromJars(String fileExtension) {
		List<URI> result = new ArrayList<>();
		URL[] urls = this.getURLs();
		for (URL url : urls) {
			if(url.getPath().endsWith(JAR) || url.getPath().endsWith(WAR))
			{
				extractFiles(fileExtension, result, url);
			}
		}
		return result ;
	}

	/**
	 * @param fileExtension
	 * @param result
	 * @param url
	 */
	private void extractFiles(String fileExtension, List<URI> result, URL url) {
		JarEntry entry = null;
		try (InputStream in = new FileInputStream(url.getFile()); JarInputStream jar = new JarInputStream(in);)
		{
			entry = jar.getNextJarEntry();
			if(logger.isInfoEnabled())
				logger.info(url.toString());
			while(entry != null)
			{
				/*
				 * WE NEED TO AVOID TO ANALYZE FRAMEWORK DEPENDENCIES
				 */
				if(entry.getName().endsWith(fileExtension))
				{
					String tempURL = url.toString() + "!/" + entry.getName();
					result.add((URI.create(tempURL).toURL()).toURI());
				}
				entry = jar.getNextJarEntry();
			}
		} catch (IOException | URISyntaxException e) {
			logger.error("Error when getting files from jar", e);
		}
	}

}
