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
 * {@code refinej refactor move} — move a class to a new package.
 */
@Component
@Command(name = "move", description = "Move a class to a new package.", mixinStandardHelpOptions = true)
public class RefactorMoveCommand implements Callable<Integer> {

	@Autowired
	private EngineResolver engineResolver;

	private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

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
		RefactoringEngine eng = this.engineResolver.resolve(this.engine);
		Symbol symbol = eng.findSymbol(this.classFqn)
			.orElseThrow(() -> new RefactorException.SymbolNotFoundException(this.classFqn));

		ChangeSet changeSet = eng.computeMove(symbol, this.targetPackage);
		String newFqn = this.targetPackage + "." + symbol.simpleName();
		printChangeSet(changeSet, "move", this.classFqn, newFqn);

		// Apply changes if --yes is set and there are no conflicts
		if (this.yes && !changeSet.hasConflicts()) {
			eng.apply(changeSet, false);
			System.out.println("Applied move successfully.");
		}
		else if (!this.preview && !changeSet.hasConflicts()) {
			eng.apply(changeSet, false);
			System.out.println("Applied move successfully.");
		}

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
			cs.conflicts().forEach((c) -> System.out.println("  * " + c.description()));
		}
		cs.changes()
			.stream()
			.filter((fc) -> fc.unifiedDiff() != null)
			.forEach((fc) -> System.out.println(fc.unifiedDiff()));
		System.out.printf("%d file(s) would change%n", cs.filesAffected());
	}

}
