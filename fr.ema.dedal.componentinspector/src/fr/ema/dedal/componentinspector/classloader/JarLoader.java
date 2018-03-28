/**
 * 
 */
package fr.ema.dedal.componentinspector.classloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
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
	private static final String SPRING_FRAMEWORK = "org.springframework";
	private static final String AOP_ALLIANCE = "org.aopalliance";
	private static final String JSON = "org.json";
	private static final String ECLIPSE_JDT = "org.eclipse.jdt";
	private static final String APACHE_COMMONS = "org.apache.commons.";
	private static final String APACHE_LOG4J = "org.apache.log4j.";
	private static final String ORG_OPENID4JAVA = "org.openid4java";

	private static final String JAVAX = "javax";

	/**
	 * Parameterized constructor
	 * @param urls
	 */
	public JarLoader(URL[] urls) {
		super(urls);
	}

	public Map<URI, List<Class<?>>> loadClasses(String name) {
		Map<URI, List<Class<?>>> jarMapping = new HashMap<>();
		URL[] urls = this.getURLs();
		for (URL url : urls) {			
			List<Class<?>> listClasses = new ArrayList<>();

				try {
					traverseJar(jarMapping, url, listClasses, name);
				} catch (URISyntaxException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
			
			
		}
		return jarMapping;
	}

	public Map<URI, List<Class<?>>> loadClasses() {
		Map<URI, List<Class<?>>> jarMapping = new HashMap<>();
		URL[] urls = this.getURLs();
		for (URL url : urls) {			
			List<Class<?>> listClasses = new ArrayList<>();

//			if(URI.create(url.getFile()).getPath().endsWith(".jar"))
//			{
				try {
					traverseJar(jarMapping, url, listClasses, "");
				} catch (URISyntaxException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
//			} else
//				try {
//					traverseFolder(jarMapping, url, listClasses);
//				} catch (URISyntaxException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
			
			
			
		}
		return jarMapping;
	}

//	private void traverseFolder(Map<URI, List<Class<?>>> jarMapping, URL url, List<Class<?>> listClasses) throws URISyntaxException {
//
//		File file = new File(url.toURI());
//		
//		if(file.isDirectory())
//		{
//			for(File f : file.listFiles())
//			{
//				try {
//					traverseFolder(jarMapping, new URL(f.getAbsolutePath()), listClasses);
//				} catch (MalformedURLException e) {
//					logger.error(e.getMessage(), e);
//				}
//			}
//		}
//		else if(file.getName().endsWith(".java")) {
//			System.out.println("coucou");
//		}
//		
//	}

	/**
	 * @param jarMapping
	 * @param url
	 * @param listClasses
	 * @param name2 
	 * @throws URISyntaxException 
	 * @throws IOException 
	 */
	private void traverseJar(Map<URI, List<Class<?>>> jarMapping, URL url, List<Class<?>> listClasses, String name2) throws URISyntaxException, IOException {
		JarEntry entry = null;
		InputStream in = new FileInputStream(url.getFile());
		JarInputStream jar = new JarInputStream(in);
		entry = jar.getNextJarEntry();
		while(entry != null)
		{	
			/*
			 * WE TRANSFORM THE PATH INTO THE FULLY QUALIFIED NAME OF THE CLASS
			 */
			String name = entry.getName().replaceAll("/", ".");
			/*
			 * WE NEED TO AVOID TO ANALYZE FRAMEWORK DEPENDENCIES
			 */
			if(
//					name.contains(name2)
					//						!name.startsWith(SPRING_FRAMEWORK) 
					//						&& !name.startsWith(AOP_ALLIANCE) 
					//						&& !name.startsWith(JSON)
					//						&& !name.startsWith(ECLIPSE_JDT)
					//						&& !name.startsWith(APACHE_COMMONS)
					//						&& !name.startsWith(APACHE_LOG4J)
					//						&& !name.startsWith(ORG_OPENID4JAVA)
//					&& !name.startsWith(JAVAX)
//					&& 
					name.endsWith(JAVA_CLASS_SUFFIX))
			{
				if(logger.isInfoEnabled())
					logger.info(name);
				load(jarMapping, url, listClasses, name);
			}
			entry = jar.getNextJarEntry();
		}
		jar.close();
	}

	/**
	 * @param jarMapping
	 * @param url
	 * @param listClasses
	 * @param name
	 * @throws ClassNotFoundException 
	 * @throws URISyntaxException 
	 */
	private void load(Map<URI, List<Class<?>>> jarMapping, URL url, List<Class<?>> listClasses, String name) throws URISyntaxException {
			Class<?> loadClass;
			try {
				loadClass = this.loadClass(name.substring(0, name.length()-JAVA_CLASS_SUFFIX.length()));
				listClasses.add(loadClass);
			} catch (ClassNotFoundException e) {
				logger.error(e.getMessage(), e);
			}
			jarMapping.put(url.toURI(), listClasses);
	}	
}
