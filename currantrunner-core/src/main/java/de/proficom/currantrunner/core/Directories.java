package de.proficom.currantrunner.core;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

public class Directories {
	/**
	 * Environment parameter to adjust the sub directory for DB
	 */
	private final static String PARAM_SUBDIR_CURRANRUNNER = "currantRunner.dataDirectory";
	private static String subDirCurrantRunner = "CurrantRunner";

	/**
	 * Get the project's directory from TestNG context Iterate the path to get for
	 * '/target' path element which parent is used as base directory
	 * 
	 * @return Project's directory
	 */
	private static Path GetProjectDirectory() {
		// Get project directory (including /target/test-classes)
		Path basePath;
		try {
			basePath = Paths.get(Directories.class.getResource("/").toURI()).getParent();
		} catch (URISyntaxException e) {
			basePath = Paths.get("").toAbsolutePath();
 		}

		// Iterate all path elements and try to find 'target' component
		Iterator<Path> itBaseDirectories = basePath.iterator();
		boolean hasTargetPath = false;
		int idxTargetPath = -1;
		while (itBaseDirectories.hasNext()) {
			idxTargetPath++;
			String curPathElement = itBaseDirectories.next().toString();
			if (curPathElement.equals("target")) {
				hasTargetPath = true;
				break;
			}
		}

		// If found: Return this part of directory
		if (hasTargetPath) {
			return Paths.get(basePath.getRoot().toString(), basePath.subpath(0, idxTargetPath).toString());
		}
		return basePath;
	}

	/**
	 * Get the data base directory from test context
	 * 
	 * @param testNgContext Context object of TestNG
	 * @return Project's database directory
	 */
	public static String GetDatabaseDirectory() {
		// Read subdirectory from environment settings
		if (System.getProperty(PARAM_SUBDIR_CURRANRUNNER) != null) {
			subDirCurrantRunner = System.getProperty(PARAM_SUBDIR_CURRANRUNNER);
		}
		
		// Get the path to database
		Path projectDir = GetProjectDirectory();
		Path dbDirectory = projectDir.resolve(subDirCurrantRunner);
		dbDirectory = dbDirectory.resolve("database-files");
		return getFile(dbDirectory.toString()).getAbsolutePath();
	}

	/**
	 * Creates a File object for the given path. If directories at the path are
	 * missing, they are created.
	 * 
	 * @param path directory String
	 * @return created File object
	 */
	private static File getFile(String path) {
		File file = new File(path);
		if (file.exists()) {
			return file;
		}
		file.mkdirs();
		return file;
	}
}
