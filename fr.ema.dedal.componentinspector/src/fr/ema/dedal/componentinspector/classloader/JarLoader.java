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
	private static final String APACHE = "org.apache";
	private static final String ECLIPSE_JDT = "org.eclipse.jdt";

	/**
	 * Parameterized constructor
	 * @param urls
	 */
	public JarLoader(URL[] urls) {
		super(urls);
	}

	public Map<URI, List<Class<?>>> loadClasses() {
		Map<URI, List<Class<?>>> jarMapping = new HashMap<>();
		URL[] urls = this.getURLs();
		for (URL url : urls) {			
			List<Class<?>> listClasses = new ArrayList<>();

//			if(URI.create(url.getFile()).getPath().endsWith(".jar"))
//			{
				traverseJar(jarMapping, url, listClasses);
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
	 */
	private void traverseJar(Map<URI, List<Class<?>>> jarMapping, URL url, List<Class<?>> listClasses) {
		JarEntry entry = null;
		try (InputStream in = new FileInputStream(url.getFile());
				JarInputStream jar = new JarInputStream(in);){
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
						!(
								name.startsWith(SPRING_FRAMEWORK) 
								|| name.startsWith(AOP_ALLIANCE) 
								|| name.startsWith(APACHE)
								|| name.startsWith(ECLIPSE_JDT)
								) && 
						name.endsWith(JAVA_CLASS_SUFFIX))
				{
					listClasses.add(this.loadClass(name.substring(0, name.length()-JAVA_CLASS_SUFFIX.length())));
					jarMapping.put(url.toURI(), listClasses);
				}
				entry = jar.getNextJarEntry();
			}
		} catch (IOException | ClassNotFoundException | URISyntaxException e1) {
			logger.error(e1.getMessage(), e1);
		}
	}	
}
