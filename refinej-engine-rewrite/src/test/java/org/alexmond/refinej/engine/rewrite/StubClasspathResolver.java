package org.alexmond.refinej.engine.rewrite;

import java.nio.file.Path;
import java.util.List;

import org.alexmond.refinej.core.engine.api.BuildType;
import org.alexmond.refinej.core.engine.api.ClasspathResolver;

/**
 * Test-only {@link ClasspathResolver} that returns an empty classpath.
 *
 * <p>
 * Forces the OpenRewrite parser into no-classpath mode, which is sufficient for the
 * simple fixture project that has no external dependencies.
 */
class StubClasspathResolver implements ClasspathResolver {

	@Override
	public List<Path> resolve(Path projectRoot, BuildType buildType) {
		return List.of();
	}

}
