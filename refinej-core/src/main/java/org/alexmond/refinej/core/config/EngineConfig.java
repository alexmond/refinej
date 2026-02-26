package org.alexmond.refinej.core.config;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.alexmond.refinej.core.engine.api.EngineType;
import org.alexmond.refinej.core.engine.api.RefactoringEngine;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Selects the active {@link RefactoringEngine} bean at startup.
 *
 * <p>
 * All three engine implementations are always on the classpath and instantiated by
 * Spring. This factory picks the one matching {@code refinej.default-engine} (or the
 * per-command {@code --engine} flag via {@code EngineResolver} in refinej-cli).
 *
 * <p>
 * Using a factory over {@code @Profile} keeps all engines in the same JAR.
 */
@Slf4j
@Configuration
public class EngineConfig {

	@Value("${refinej.default-engine:spoon}")
	private String defaultEngine;

	/**
	 * Exposes the chosen engine as the {@code @Primary} {@link RefactoringEngine} bean.
	 * @param engines all {@link RefactoringEngine} implementations found by Spring
	 */
	@Bean
	@Primary
	public RefactoringEngine activeEngine(List<RefactoringEngine> engines) {
		EngineType target = EngineType.fromString(this.defaultEngine);

		RefactoringEngine selected = engines.stream()
			.filter((e) -> e.getType() == target)
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("No RefactoringEngine bean found for type: " + target
					+ ". Available: " + engines.stream().map((e) -> e.getType().name()).toList()));

		log.info("[RefineJ] Active engine: {}", selected.getType());
		return selected;
	}

}
