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
 * {@code refinej index} — scan a Java project and build an in-memory symbol index.
 */
@Slf4j
@Component
@Command(
        name = "index",
        description = "Index a Java project to build the symbol index.",
        mixinStandardHelpOptions = true
)
public class IndexCommand implements Callable<Integer> {

    @Autowired
    private EngineResolver engineResolver;

    private final ObjectMapper objectMapper =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Option(names = "--path", defaultValue = ".", description = "Path to the project root (default: current directory).")
    private String path;

    @Option(names = "--engine", description = "Engine override: spoon | rewrite | javaparser.")
    private String engine;

    @Option(names = "--json", description = "Output as JSON.")
    private boolean json;

    @Override
    public Integer call() {
        Path projectRoot = Path.of(path).toAbsolutePath().normalize();
        BuildType buildType = BuildType.detect(projectRoot);
        RefactoringEngine eng = engineResolver.resolve(engine);

        log.info("Indexing {} with engine {} (build: {})", projectRoot, eng.getType(), buildType);

        long start = System.currentTimeMillis();
        eng.indexProject(projectRoot, buildType);
        long duration = System.currentTimeMillis() - start;

        long symbols = eng.getAllSymbols().size();
        long refs    = eng.getAllReferences().size();

        if (json) {
            print(new JsonDto.IndexResponse(
                    "ok", eng.getType().name().toLowerCase(), symbols, refs, duration));
        } else {
            System.out.printf("Indexed %s in %dms using %s — %d symbols, %d references%n",
                    projectRoot, duration, eng.getType().name().toLowerCase(), symbols, refs);
        }
        return 0;
    }

    private void print(Object obj) {
        try {
            System.out.println(objectMapper.writeValueAsString(obj));
        } catch (Exception e) {
            System.err.println("{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
        }
    }
}
