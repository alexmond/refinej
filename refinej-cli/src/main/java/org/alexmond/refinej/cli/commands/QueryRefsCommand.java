package org.alexmond.refinej.cli.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.alexmond.refinej.core.domain.Reference;
import org.alexmond.refinej.core.domain.Symbol;
import org.alexmond.refinej.core.engine.api.RefactoringEngine;
import org.alexmond.refinej.core.exception.RefactorException;
import org.alexmond.refinej.core.model.JsonDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * {@code refinej query refs} — find all usages of a symbol.
 */
@Component
@Command(
        name = "refs",
        description = "Find all usages of a symbol.",
        mixinStandardHelpOptions = true
)
public class QueryRefsCommand implements Callable<Integer> {

    @Autowired
    private EngineResolver engineResolver;

    private final ObjectMapper objectMapper =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Option(names = "--name", required = true, description = "Fully-qualified name of the symbol.")
    private String name;

    @Option(names = "--kind", description = "Filter by usage kind (e.g. METHOD_CALL, IMPORT).")
    private String kind;

    @Option(names = "--engine", description = "Engine override: spoon | rewrite | javaparser.")
    private String engine;

    @Option(names = "--json", description = "Output as JSON.")
    private boolean json;

    @Override
    public Integer call() {
        RefactoringEngine eng = engineResolver.resolve(engine);
        Symbol symbol = eng.findSymbol(name)
                .orElseThrow(() -> new RefactorException.SymbolNotFoundException(name));

        List<Reference> refs = eng.findReferences(symbol);
        if (kind != null && !kind.isBlank()) {
            String upperKind = kind.toUpperCase();
            refs = refs.stream()
                    .filter(r -> r.usageKind().name().equals(upperKind))
                    .toList();
        }

        if (json) {
            List<JsonDto.ReferenceItem> items = refs.stream()
                    .map(r -> new JsonDto.ReferenceItem(
                            r.filePath(), r.line(), r.column(), r.usageKind().name()))
                    .toList();
            try {
                System.out.println(objectMapper.writeValueAsString(
                        new JsonDto.RefsResponse("ok", name, refs.size(), items)));
            } catch (Exception e) {
                System.err.println("{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
                return 1;
            }
        } else {
            if (refs.isEmpty()) {
                System.out.println("No references found for: " + name);
                return 0;
            }
            System.out.printf("%d reference(s) to %s:%n", refs.size(), name);
            refs.forEach(r -> System.out.printf("  %-20s %s:%d%n",
                    r.usageKind(), r.filePath(), r.line()));
        }
        return 0;
    }
}
