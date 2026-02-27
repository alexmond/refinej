package org.alexmond.refinej.engine.javaparser;

import java.nio.file.Path;
import java.util.List;

import org.alexmond.refinej.core.engine.api.BuildType;
import org.alexmond.refinej.core.engine.api.ClasspathResolver;

/**
 * Test-only classpath resolver that returns an empty classpath.
 */
class StubClasspathResolver implements ClasspathResolver {

	@Override
	public List<Path> resolve(Path projectRoot, BuildType buildType) {
		return List.of();
	}

}
