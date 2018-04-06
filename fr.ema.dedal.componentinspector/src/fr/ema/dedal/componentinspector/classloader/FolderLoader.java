/**
 * 
 */
package fr.ema.dedal.componentinspector.classloader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

/**
 * @author Alexandre Le Borgne
 *
 */
public class FolderLoader {

	static final Logger logger = Logger.getLogger(FolderLoader.class);
	public static final String JAVA_CLASS_EXTENSION = ".class";
	public static final String JAR_FILE_EXTENSION = ".jar";	

	private FolderLoader() {}

	public static final List<URI> loadFolder(Path path) throws IOException {
		List<URI> uris = new ArrayList<>();
		Stream<Path> stream = Files.list(path);
		stream.forEach(p -> uris.add(p.toUri()));
		stream.close();
		return uris;
	}



}
