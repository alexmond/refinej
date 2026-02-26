package org.alexmond.refinej.cli.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.alexmond.refinej.core.engine.api.BuildType;
import org.alexmond.refinej.core.engine.api.RefactoringEngine;
import org.alexmond.refinej.core.model.JsonDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * {@code refinej reindex} — clear the index and re-index from scratch (or incrementally).
 */
@Slf4j
@Component
@Command(
        name = "reindex",
        description = "Clear the index and re-index the project.",
        mixinStandardHelpOptions = true
)
public class ReindexCommand implements Callable<Integer> {

    @Autowired
    private EngineResolver engineResolver;

    private final ObjectMapper objectMapper =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Option(names = "--path", defaultValue = ".", description = "Path to the project root.")
    private String path;

    @Option(names = "--changed-only", description = "Only re-index files changed since last index (not yet implemented).")
    private boolean changedOnly;

    @Option(names = "--engine", description = "Engine override: spoon | rewrite | javaparser.")
    private String engine;

    @Option(names = "--json", description = "Output as JSON.")
    private boolean json;

    @Override
    public Integer call() {
        if (changedOnly) {
            // TODO RFJ-034: delegate to IndexingService.reindexChanged()
            System.out.println("Incremental reindex not yet implemented — see RFJ-034");
            return 0;
        }

        Path projectRoot = Path.of(path).toAbsolutePath().normalize();
        BuildType buildType = BuildType.detect(projectRoot);
        RefactoringEngine eng = engineResolver.resolve(engine);

        eng.clearIndex();
        log.info("Re-indexing {} with engine {} (build: {})", projectRoot, eng.getType(), buildType);

        long start = System.currentTimeMillis();
        eng.indexProject(projectRoot, buildType);
        long duration = System.currentTimeMillis() - start;

        long symbols = eng.getAllSymbols().size();
        long refs    = eng.getAllReferences().size();

        if (json) {
            try {
                System.out.println(objectMapper.writeValueAsString(new JsonDto.IndexResponse(
                        "ok", eng.getType().name().toLowerCase(), symbols, refs, duration)));
            } catch (Exception e) {
                System.err.println("{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
            }
        } else {
            System.out.printf("Re-indexed %s in %dms using %s — %d symbols, %d references%n",
                    projectRoot, duration, eng.getType().name().toLowerCase(), symbols, refs);
        }
        return 0;
    }
}
