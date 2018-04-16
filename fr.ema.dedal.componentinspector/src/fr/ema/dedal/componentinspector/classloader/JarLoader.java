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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;


import org.apache.log4j.Logger;


/**
 * @author Alexandre Le Borgne
 *
 */
public class JarLoader extends URLClassLoader {
	
	static final Logger logger = Logger.getLogger(JarLoader.class);

	private static final String JAVA_CLASS_SUFFIX = ".class";
//	private static final String SPRING_FRAMEWORK = "org.springframework";
//	private static final String AOP_ALLIANCE = "org.aopalliance";
//	private static final String JSON = "org.json";
//	private static final String ECLIPSE_JDT = "org.eclipse.jdt";
//	private static final String APACHE_COMMONS = "org.apache.commons.";
//	private static final String APACHE_LOG4J = "org.apache.log4j.";
//	private static final String ORG_OPENID4JAVA = "org.openid4java";
//	private static final String JAVAX = "javax";
//	private static final String ORG_XML = "org.xml.";
//	private static final String GROOVY_CLASS_SUFFIX = ".groovy";

	static final String JAR = ".jar";
	static final String WAR = ".war";
	
	private List<String> toReload = new ArrayList<>();
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

//	public Map<URI, List<Class<?>>> loadClasses() {
//		Map<URI, List<Class<?>>> jarMapping = new HashMap<>();
//		URL[] urls = this.getURLs();
//		for (URL url : urls) {		
//			List<Class<?>> listClasses = new ArrayList<>();
//			if(url.getPath().endsWith(JAR) || url.getPath().endsWith(WAR))
//			{
//				traverseJar(jarMapping, url, listClasses);
//			}
//		}
//		return jarMapping;
//	}

//	/**
//	 * @param jarMapping
//	 * @param url
//	 * @param listClasses
//	 * @param name2 
//	 * @throws URISyntaxException 
//	 * @throws IOException 
//	 */
//	private void traverseJar(Map<URI, List<Class<?>>> jarMapping, URL url, List<Class<?>> listClasses) {
//		JarEntry entry = null;
//		try (InputStream in = new FileInputStream(url.getFile()); JarInputStream jar = new JarInputStream(in);)
//		{
//			entry = jar.getNextJarEntry();
//			if(logger.isInfoEnabled())
//				logger.info(url.toString());
//			while(entry != null)
//			{	
//				/*
//				 * WE TRANSFORM THE PATH INTO THE FULLY QUALIFIED NAME OF THE CLASS
//				 */
//				String name = entry.getName().replaceAll("/", ".");
//				if(name.startsWith("WEB-INF.classes."))
//					name = name.replaceFirst("WEB-INF.classes.", "");
//				/*
//				 * WE NEED TO AVOID TO ANALYZE FRAMEWORK DEPENDENCIES
//				 */
//				if(
//						name.endsWith(JAVA_CLASS_SUFFIX)
////						&& name.contains(name2)
////						&& !name.startsWith(SPRING_FRAMEWORK) 
////						&& !name.startsWith(AOP_ALLIANCE) 
////						&& !name.startsWith(JSON)
////						&& !name.startsWith(ECLIPSE_JDT)
////						&& 
////						!name.startsWith(APACHE_COMMONS)
////						&& !name.startsWith(APACHE_LOG4J)
////						&& !name.startsWith(ORG_OPENID4JAVA)
////						&& !name.startsWith(JAVAX)
////						&& !name.startsWith(ORG_XML)
//						)
//				{
//					load(jarMapping, url, listClasses, name);
//				}
//				entry = jar.getNextJarEntry();
//			}
//		} catch (IOException e) {
//			logger.error("A problem occured while loading JAR", e);
//		}
//	}

//	/**
//	 * @param jarMapping
//	 * @param url
//	 * @param listClasses
//	 * @param name
//	 * @throws ClassNotFoundException 
//	 * @throws URISyntaxException 
//	 */
//	private void load(Map<URI, List<Class<?>>> jarMapping, URL url, List<Class<?>> listClasses, String name) {
//			Class<?> loadClass;
//			try {
//				loadClass = this.loadClass(name.substring(0, name.length()-JAVA_CLASS_SUFFIX.length()));
//				listClasses.add(loadClass);
//			} 
//			catch (NoClassDefFoundError | IllegalAccessError | VerifyError | ClassNotFoundException e) {
//				toReload.add(name.substring(0, name.length()-JAVA_CLASS_SUFFIX.length()));
//				logger.error("A problem occured while loading class " + name + ". Loading ended with " + e.getCause());
//			}
//			try {
//				jarMapping.put(url.toURI(), listClasses);
//			} 
//			catch (URISyntaxException e) {
//				logger.error(e.getMessage(), e);
//			}
//	}
}
