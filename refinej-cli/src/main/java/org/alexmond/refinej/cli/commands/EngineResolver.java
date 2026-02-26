package org.alexmond.refinej.cli.commands;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.alexmond.refinej.core.engine.api.EngineType;
import org.alexmond.refinej.core.engine.api.RefactoringEngine;

import org.springframework.stereotype.Component;

/**
 * Resolves which {@link RefactoringEngine} to use for a given command invocation.
 *
 * <p>
 * When {@code --engine} is provided it overrides the configured default for that command
 * only. Otherwise the {@code @Primary} engine from {@code EngineConfig} is used.
 */
@Component
@RequiredArgsConstructor
public class EngineResolver {

	private final RefactoringEngine defaultEngine;

	private final List<RefactoringEngine> allEngines;

	/**
	 * Returns the engine to use for this invocation.
	 * @param engineOverride nullable/blank engine name from {@code --engine} flag
	 * @return the engine to use for this invocation
	 */
	public RefactoringEngine resolve(String engineOverride) {
		if (engineOverride == null || engineOverride.isBlank()) {
			return this.defaultEngine;
		}
		EngineType target = EngineType.fromString(engineOverride);
		return this.allEngines.stream()
			.filter((e) -> e.getType() == target)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException(
					"Unknown engine '" + engineOverride + "'. Valid values: spoon, rewrite, javaparser"));
	}

}
