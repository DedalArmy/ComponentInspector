package fr.ema.dedal.componentinspector.explorer;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import org.apache.log4j.Logger;

import dedal.DedalDiagram;
import fr.ema.dedal.componentinspector.classloader.JarLoader;
import fr.ema.dedal.componentinspector.inspector.JarInspector;
import dedal.impl.DedalFactoryImpl;

public class Explorer {
	
	static final Logger logger = Logger.getLogger(Explorer.class);
	
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
			
			URL[] urlToLoad=new URL[]{Paths.get(singlePath).toUri().toURL()};
			JarLoader jarloader = new JarLoader(urlToLoad);
			
			/*
			 * let's extract some component classes
			 */
			JarInspector jarInspector = new JarInspector(jarloader);
			jarInspector.generate(result, sdslPath);
			return result;
		} 
		catch (IOException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}
	
}
