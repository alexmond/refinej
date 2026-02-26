package org.alexmond.refinej.core.engine.api;

import java.nio.file.Path;
import java.util.List;

/**
 * Resolves the compile classpath for a project so that engines can perform accurate
 * cross-dependency type resolution.
 */
public interface ClasspathResolver {

	/**
	 * Returns the classpath entries (JARs, class directories) for the given project.
	 * Returns an empty list on failure — engines must handle the no-classpath case
	 * gracefully (reduced semantic accuracy, no crash).
	 */
	List<Path> resolve(Path projectRoot, BuildType buildType);

}
