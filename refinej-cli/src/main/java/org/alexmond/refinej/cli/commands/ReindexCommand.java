package org.alexmond.refinej.cli.commands;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.alexmond.refinej.core.engine.api.BuildType;
import org.alexmond.refinej.core.engine.api.RefactoringEngine;
import org.alexmond.refinej.core.model.JsonDto;
import org.alexmond.refinej.core.service.IndexingService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * {@code refinej reindex} — clear the index and re-index from scratch (or incrementally).
 */
@Slf4j
@Component
@Command(name = "reindex", description = "Clear the index and re-index the project.", mixinStandardHelpOptions = true)
public class ReindexCommand implements Callable<Integer> {

	@Autowired
	private EngineResolver engineResolver;

	@Autowired
	private IndexingService indexingService;

	private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

	@Option(names = "--path", defaultValue = ".", description = "Path to the project root.")
	private String path;

	@Option(names = "--changed-only",
			description = "Only re-index files changed since last index (not yet implemented).")
	private boolean changedOnly;

	@Option(names = "--engine", description = "Engine override: spoon | rewrite | javaparser.")
	private String engine;

	@Option(names = "--json", description = "Output as JSON.")
	private boolean json;

	@Override
	public Integer call() {
		if (this.changedOnly) {
			// TODO RFJ-034: delegate to IndexingService.reindexChanged()
			System.out.println("Incremental reindex not yet implemented — see RFJ-034");
			return 0;
		}

		Path projectRoot = Path.of(this.path).toAbsolutePath().normalize();
		BuildType buildType = BuildType.detect(projectRoot);
		RefactoringEngine eng = this.engineResolver.resolve(this.engine);

		log.info("Re-indexing {} with engine {} (build: {})", projectRoot, eng.getType(), buildType);

		IndexingService.IndexingResult result = this.indexingService.index(eng, projectRoot, buildType);

		if (this.json) {
			print(new JsonDto.IndexResponse("ok", eng.getType().name().toLowerCase(), result.symbolCount(),
					result.referenceCount(), result.durationMs()));
		}
		else {
			System.out.printf("Re-indexed %s in %dms using %s — %d symbols, %d references%n", projectRoot,
					result.durationMs(), eng.getType().name().toLowerCase(), result.symbolCount(),
					result.referenceCount());
		}
		return 0;
	}

	private void print(Object obj) {
		try {
			System.out.println(this.objectMapper.writeValueAsString(obj));
		}
		catch (Exception ex) {
			System.err.println("{\"status\":\"error\",\"message\":\"" + ex.getMessage() + "\"}");
		}
	}

}
