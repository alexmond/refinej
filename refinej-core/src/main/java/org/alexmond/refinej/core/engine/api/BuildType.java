package org.alexmond.refinej.core.engine.api;

import java.nio.file.Files;
import java.nio.file.Path;

public enum BuildType {

	MAVEN, GRADLE, UNKNOWN;

	/** Detect build type by inspecting files in the given project root. */
	public static BuildType detect(Path projectRoot) {
		if (Files.exists(projectRoot.resolve("pom.xml"))) {
			return MAVEN;
		}
		if (Files.exists(projectRoot.resolve("build.gradle"))
				|| Files.exists(projectRoot.resolve("build.gradle.kts"))) {
			return GRADLE;
		}
		return UNKNOWN;
	}

}
