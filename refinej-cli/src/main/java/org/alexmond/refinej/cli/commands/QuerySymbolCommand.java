package org.alexmond.refinej.cli.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.alexmond.refinej.core.domain.Symbol;
import org.alexmond.refinej.core.engine.api.RefactoringEngine;
import org.alexmond.refinej.core.exception.RefactorException;
import org.alexmond.refinej.core.model.JsonDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * {@code refinej query symbol} — look up a symbol by its fully-qualified name.
 */
@Component
@Command(
        name = "symbol",
        description = "Look up a symbol by its fully-qualified name.",
        mixinStandardHelpOptions = true
)
public class QuerySymbolCommand implements Callable<Integer> {

    @Autowired
    private EngineResolver engineResolver;

    private final ObjectMapper objectMapper =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Option(names = "--name", required = true, description = "Fully-qualified name of the symbol.")
    private String name;

    @Option(names = "--engine", description = "Engine override: spoon | rewrite | javaparser.")
    private String engine;

    @Option(names = "--json", description = "Output as JSON.")
    private boolean json;

    @Override
    public Integer call() {
        RefactoringEngine eng = engineResolver.resolve(engine);
        Symbol symbol = eng.findSymbol(name)
                .orElseThrow(() -> new RefactorException.SymbolNotFoundException(name));

        if (json) {
            try {
                System.out.println(objectMapper.writeValueAsString(new JsonDto.SymbolResponse(
                        "ok", symbol.qualifiedName(), symbol.kind().name(),
                        symbol.filePath(), symbol.lineStart(), symbol.lineEnd())));
            } catch (Exception e) {
                System.err.println("{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
                return 1;
            }
        } else {
            System.out.printf("[%s] %s%n  file: %s:%d-%d%n  type: %s%n",
                    symbol.kind(), symbol.qualifiedName(),
                    symbol.filePath(), symbol.lineStart(), symbol.lineEnd(),
                    symbol.typeFqn() != null ? symbol.typeFqn() : "n/a");
        }
        return 0;
    }
}
