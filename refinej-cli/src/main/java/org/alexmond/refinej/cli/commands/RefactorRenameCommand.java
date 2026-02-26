package org.alexmond.refinej.cli.commands;

import java.util.List;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.alexmond.refinej.core.domain.ChangeSet;
import org.alexmond.refinej.core.domain.Symbol;
import org.alexmond.refinej.core.engine.api.RefactoringEngine;
import org.alexmond.refinej.core.exception.RefactorException;
import org.alexmond.refinej.core.model.JsonDto;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * {@code refinej refactor rename} — rename a symbol across the project.
 *
 * <p>
 * Phase 1: compute/apply throw {@link UnsupportedOperationException} until Phase 4
 * (RFJ-040).
 */
@Component
@Command(name = "rename", description = "Rename a symbol across the project.", mixinStandardHelpOptions = true)
public class RefactorRenameCommand implements Callable<Integer> {

	@Autowired
	private EngineResolver engineResolver;

	private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

	@Option(names = "--old", required = true, description = "Current fully-qualified name.")
	private String oldFqn;

	@Option(names = "--new", required = true, description = "New fully-qualified name.")
	private String newFqn;

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
		RefactoringEngine eng = this.engineResolver.resolve(this.engine);
		Symbol symbol = eng.findSymbol(this.oldFqn)
			.orElseThrow(() -> new RefactorException.SymbolNotFoundException(this.oldFqn));

		// TODO RFJ-040: throws UnsupportedOperationException until Phase 4
		ChangeSet changeSet = eng.computeRename(symbol, this.newFqn);
		printChangeSet(changeSet, "rename", this.oldFqn, this.newFqn);
		return changeSet.hasConflicts() ? 1 : 0;
	}

	private void printChangeSet(ChangeSet cs, String op, String from, String to) {
		if (this.json) {
			List<JsonDto.ConflictItem> conflicts = (cs.conflicts() != null) ? cs.conflicts()
				.stream()
				.map((c) -> new JsonDto.ConflictItem(c.description(),
						(c.filePath() != null) ? c.filePath().toString() : null, c.line()))
				.toList() : List.of();
			String diffText = cs.changes()
				.stream()
				.map((fc) -> (fc.unifiedDiff() != null) ? fc.unifiedDiff() : "")
				.reduce("", String::concat);
			try {
				System.out.println(this.objectMapper.writeValueAsString(new JsonDto.RefactorResponse(
						cs.hasConflicts() ? "conflicts" : "ok", op, from, to, cs.filesAffected(),
						new JsonDto.PreviewSummary(diffText, cs.filesAffected() + " file(s) changed"), conflicts)));
			}
			catch (Exception ex) {
				System.err.println("{\"status\":\"error\",\"message\":\"" + ex.getMessage() + "\"}");
			}
			return;
		}

		if (cs.hasConflicts()) {
			System.out.println("CONFLICTS detected — apply blocked:");
			cs.conflicts().forEach((c) -> System.out.println("  ✗ " + c.description()));
		}
		cs.changes()
			.stream()
			.filter((fc) -> fc.unifiedDiff() != null)
			.forEach((fc) -> System.out.println(fc.unifiedDiff()));
		System.out.printf("%d file(s) would change%n", cs.filesAffected());
	}

}
