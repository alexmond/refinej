package org.alexmond.refinej.cli.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.alexmond.refinej.core.domain.ChangeSet;
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
 * {@code refinej refactor move} — move a class to a new package.
 *
 * <p>Phase 1: compute/apply throw {@link UnsupportedOperationException} until Phase 4 (RFJ-041).
 */
@Component
@Command(
        name = "move",
        description = "Move a class to a new package.",
        mixinStandardHelpOptions = true
)
public class RefactorMoveCommand implements Callable<Integer> {

    @Autowired
    private EngineResolver engineResolver;

    private final ObjectMapper objectMapper =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Option(names = "--class", required = true, description = "Fully-qualified class name to move.")
    private String classFqn;

    @Option(names = "--to", required = true, description = "Target package name.")
    private String targetPackage;

    @Option(names = "--preview", defaultValue = "true", description = "Show diff without applying (default: true).")
    private boolean preview;

    @Option(names = "--yes", description = "Apply without confirmation prompt.")
    private boolean yes;

    @Option(names = "--engine", description = "Engine override: spoon | rewrite | javaparser.")
    private String engine;

    @Option(names = "--json", description = "Output as JSON.")
    private boolean json;

    @Override
    public Integer call() {
        RefactoringEngine eng = engineResolver.resolve(engine);
        Symbol symbol = eng.findSymbol(classFqn)
                .orElseThrow(() -> new RefactorException.SymbolNotFoundException(classFqn));

        // TODO RFJ-041: throws UnsupportedOperationException until Phase 4
        ChangeSet changeSet = eng.computeMove(symbol, targetPackage);
        String newFqn = targetPackage + "." + symbol.simpleName();
        printChangeSet(changeSet, "move", classFqn, newFqn);
        return changeSet.hasConflicts() ? 1 : 0;
    }

    private void printChangeSet(ChangeSet cs, String op, String from, String to) {
        if (json) {
            List<JsonDto.ConflictItem> conflicts = cs.conflicts() == null ? List.of() :
                    cs.conflicts().stream()
                            .map(c -> new JsonDto.ConflictItem(
                                    c.description(),
                                    c.filePath() != null ? c.filePath().toString() : null,
                                    c.line()))
                            .toList();
            String diffText = cs.changes().stream()
                    .map(fc -> fc.unifiedDiff() != null ? fc.unifiedDiff() : "")
                    .reduce("", String::concat);
            try {
                System.out.println(objectMapper.writeValueAsString(new JsonDto.RefactorResponse(
                        cs.hasConflicts() ? "conflicts" : "ok",
                        op, from, to, cs.filesAffected(),
                        new JsonDto.PreviewSummary(diffText, cs.filesAffected() + " file(s) changed"),
                        conflicts)));
            } catch (Exception e) {
                System.err.println("{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
            }
            return;
        }

        if (cs.hasConflicts()) {
            System.out.println("CONFLICTS detected — apply blocked:");
            cs.conflicts().forEach(c -> System.out.println("  ✗ " + c.description()));
        }
        cs.changes().forEach(fc -> {
            if (fc.unifiedDiff() != null) System.out.println(fc.unifiedDiff());
        });
        System.out.printf("%d file(s) would change%n", cs.filesAffected());
    }
}
