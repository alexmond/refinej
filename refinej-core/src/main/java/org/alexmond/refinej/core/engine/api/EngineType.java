package org.alexmond.refinej.core.engine.api;

public enum EngineType {

	SPOON, OPENREWRITE, JAVAPARSER;

	/**
	 * Case-insensitive lookup. Accepts "spoon", "rewrite", "openrewrite", "javaparser".
	 * @throws IllegalArgumentException for unknown values
	 */
	public static EngineType fromString(String value) {
		return switch (value.toLowerCase().trim()) {
			case "spoon" -> SPOON;
			case "rewrite", "openrewrite" -> OPENREWRITE;
			case "javaparser" -> JAVAPARSER;
			default -> throw new IllegalArgumentException(
					"Unknown engine '" + value + "'. Valid values: spoon, rewrite, javaparser");
		};
	}

}
