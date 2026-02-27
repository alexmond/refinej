package org.alexmond.refinej.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alexmond.refinej.core.domain.ChangeSet;
import org.alexmond.refinej.core.domain.Symbol;
import org.alexmond.refinej.core.domain.SymbolKind;
import org.alexmond.refinej.core.engine.api.BuildType;
import org.alexmond.refinej.core.engine.api.RefactoringEngine;
import org.alexmond.refinej.core.util.DiffGenerator;
import org.alexmond.refinej.engine.javaparser.JavaParserEngine;
import org.alexmond.refinej.engine.rewrite.OpenRewriteEngine;
import org.alexmond.refinej.engine.spoon.SpoonEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Contract test: the same assertions run against all three engine implementations.
 *
 * <p>
 * Phase 1 assertions cover the stub-phase behaviour. Phase 5 assertions verify that
 * implemented engines (Spoon, OpenRewrite) produce equivalent results for indexing and
 * refactoring operations.
 */
@DisplayName("RefactoringEngine contract")
class EngineContractTests {

	private static final Path FIXTURE_ROOT = Path.of("src/test/resources/fixtures/simple");

	private static final String GREETER_FQN = "com.example.simple.Greeter";

	static Stream<Arguments> allEngines() {
		return Stream
			.of(Arguments.of("SpoonEngine", new SpoonEngine((root, buildType) -> List.of(), (changes, dryRun) -> {
			}, new DiffGenerator())), Arguments.of("OpenRewriteEngine",
					new OpenRewriteEngine((root, buildType) -> List.of(), (changes, dryRun) -> {
					}, new DiffGenerator())), Arguments.of("JavaParserEngine",
							new JavaParserEngine((root, buildType) -> List.of(), (changes, dryRun) -> {
							}, new DiffGenerator())));
	}

	/** Engines with full indexing + refactoring support (all 3 engines). */
	static Stream<Arguments> implementedEngines() {
		return Stream
			.of(Arguments.of("SpoonEngine", new SpoonEngine((root, buildType) -> List.of(), (changes, dryRun) -> {
			}, new DiffGenerator())), Arguments.of("OpenRewriteEngine",
					new OpenRewriteEngine((root, buildType) -> List.of(), (changes, dryRun) -> {
					}, new DiffGenerator())), Arguments.of("JavaParserEngine",
							new JavaParserEngine((root, buildType) -> List.of(), (changes, dryRun) -> {
							}, new DiffGenerator())));
	}

	// =========================================================================
	// Phase 1 — stub-phase contract (all 3 engines)
	// =========================================================================

	@ParameterizedTest(name = "{0} — getType is non-null")
	@MethodSource("allEngines")
	void getType_isNonNull(String engineName, RefactoringEngine engine) {
		assertThat(engine.getType()).isNotNull();
	}

	@ParameterizedTest(name = "{0} — indexProject does not throw")
	@MethodSource("allEngines")
	void indexProject_doesNotThrow(String engineName, RefactoringEngine engine) {
		assertThatNoException().isThrownBy(() -> engine.indexProject(FIXTURE_ROOT, BuildType.UNKNOWN));
	}

	@ParameterizedTest(name = "{0} — findSymbol returns empty before index")
	@MethodSource("allEngines")
	void findSymbol_returnsEmpty_whenNotIndexed(String engineName, RefactoringEngine engine) {
		assertThat(engine.findSymbol(GREETER_FQN)).isEmpty();
	}

	@ParameterizedTest(name = "{0} — findReferences returns empty list before index")
	@MethodSource("allEngines")
	void findReferences_returnsEmptyList_whenNotIndexed(String engineName, RefactoringEngine engine) {
		var dummySymbol = new Symbol(1L, SymbolKind.CLASS, "Greeter", GREETER_FQN, null, 0, 0, null);
		assertThat(engine.findReferences(dummySymbol)).isEmpty();
	}

	@ParameterizedTest(name = "{0} — clearIndex does not throw")
	@MethodSource("allEngines")
	void clearIndex_doesNotThrow(String engineName, RefactoringEngine engine) {
		assertThatNoException().isThrownBy(engine::clearIndex);
	}

	@ParameterizedTest(name = "{0} — getAllSymbols returns empty before index")
	@MethodSource("allEngines")
	void getAllSymbols_returnsEmpty_whenNotIndexed(String engineName, RefactoringEngine engine) {
		assertThat(engine.getAllSymbols()).isEmpty();
	}

	@ParameterizedTest(name = "{0} — getAllReferences returns empty before index")
	@MethodSource("allEngines")
	void getAllReferences_returnsEmpty_whenNotIndexed(String engineName, RefactoringEngine engine) {
		assertThat(engine.getAllReferences()).isEmpty();
	}

	@ParameterizedTest(name = "{0} — clearIndex resets getAllSymbols")
	@MethodSource("allEngines")
	void clearIndex_resetsState(String engineName, RefactoringEngine engine) {
		engine.indexProject(FIXTURE_ROOT, BuildType.UNKNOWN);
		engine.clearIndex();
		assertThat(engine.getAllSymbols()).isEmpty();
		assertThat(engine.getAllReferences()).isEmpty();
	}

	// =========================================================================
	// Phase 5 — indexing contract (implemented engines only)
	// =========================================================================

	@ParameterizedTest(name = "{0} — indexProject populates symbols")
	@MethodSource("implementedEngines")
	void indexProject_populatesSymbols(String engineName, RefactoringEngine engine) {
		engine.indexProject(FIXTURE_ROOT, BuildType.UNKNOWN);
		assertThat(engine.getAllSymbols()).isNotEmpty();
	}

	@ParameterizedTest(name = "{0} — findSymbol returns Greeter after index")
	@MethodSource("implementedEngines")
	void findSymbol_returnsGreeter_afterIndex(String engineName, RefactoringEngine engine) {
		engine.indexProject(FIXTURE_ROOT, BuildType.UNKNOWN);
		assertThat(engine.findSymbol(GREETER_FQN)).isPresent();
		assertThat(engine.findSymbol(GREETER_FQN).get().kind()).isEqualTo(SymbolKind.CLASS);
	}

	@ParameterizedTest(name = "{0} — findReferences returns usages after index")
	@MethodSource("implementedEngines")
	void findReferences_returnsUsages_afterIndex(String engineName, RefactoringEngine engine) {
		engine.indexProject(FIXTURE_ROOT, BuildType.UNKNOWN);
		Symbol greeter = engine.findSymbol(GREETER_FQN).orElseThrow();
		assertThat(engine.findReferences(greeter)).isNotEmpty();
	}

	@ParameterizedTest(name = "{0} — getAllSymbols includes both fixture classes")
	@MethodSource("implementedEngines")
	void getAllSymbols_includesBothFixtureClasses(String engineName, RefactoringEngine engine) {
		engine.indexProject(FIXTURE_ROOT, BuildType.UNKNOWN);
		List<String> fqns = engine.getAllSymbols().stream().map(Symbol::qualifiedName).toList();
		assertThat(fqns).contains(GREETER_FQN, "com.example.simple.App");
	}

	// =========================================================================
	// Phase 5 — refactoring contract (implemented engines only)
	// =========================================================================

	@ParameterizedTest(name = "{0} — computeRename produces changes")
	@MethodSource("implementedEngines")
	void computeRename_producesChanges(String engineName, RefactoringEngine engine) {
		engine.indexProject(FIXTURE_ROOT, BuildType.UNKNOWN);
		Symbol greeter = engine.findSymbol(GREETER_FQN).orElseThrow();
		ChangeSet changeSet = engine.computeRename(greeter, "com.example.simple.HelloWorld");
		assertThat(changeSet.changes()).isNotEmpty();
		assertThat(changeSet.hasConflicts()).isFalse();
	}

	@ParameterizedTest(name = "{0} — computeMove produces changes")
	@MethodSource("implementedEngines")
	void computeMove_producesChanges(String engineName, RefactoringEngine engine) {
		engine.indexProject(FIXTURE_ROOT, BuildType.UNKNOWN);
		Symbol greeter = engine.findSymbol(GREETER_FQN).orElseThrow();
		ChangeSet changeSet = engine.computeMove(greeter, "com.example.other");
		assertThat(changeSet.changes()).isNotEmpty();
		assertThat(changeSet.hasConflicts()).isFalse();
	}

	@ParameterizedTest(name = "{0} — computeRename detects name clash")
	@MethodSource("implementedEngines")
	void computeRename_detectsNameClash(String engineName, RefactoringEngine engine) {
		engine.indexProject(FIXTURE_ROOT, BuildType.UNKNOWN);
		Symbol greeter = engine.findSymbol(GREETER_FQN).orElseThrow();
		ChangeSet changeSet = engine.computeRename(greeter, "com.example.simple.App");
		assertThat(changeSet.hasConflicts()).isTrue();
	}

	// =========================================================================
	// Phase 5 — cross-engine equivalence
	// =========================================================================

	@Test
	@DisplayName("Spoon and OpenRewrite produce equivalent symbol sets")
	void crossEngine_equivalentSymbols() {
		SpoonEngine spoon = new SpoonEngine((root, buildType) -> List.of(), (changes, dryRun) -> {
		}, new DiffGenerator());
		OpenRewriteEngine rewrite = new OpenRewriteEngine((root, buildType) -> List.of(), (changes, dryRun) -> {
		}, new DiffGenerator());

		spoon.indexProject(FIXTURE_ROOT, BuildType.UNKNOWN);
		rewrite.indexProject(FIXTURE_ROOT, BuildType.UNKNOWN);

		Set<String> spoonFqns = spoon.getAllSymbols()
			.stream()
			.filter((s) -> s.kind() == SymbolKind.CLASS)
			.map(Symbol::qualifiedName)
			.collect(Collectors.toSet());

		Set<String> rewriteFqns = rewrite.getAllSymbols()
			.stream()
			.filter((s) -> s.kind() == SymbolKind.CLASS)
			.map(Symbol::qualifiedName)
			.collect(Collectors.toSet());

		assertThat(spoonFqns).isEqualTo(rewriteFqns);
	}

	@Test
	@DisplayName("Spoon and OpenRewrite produce equivalent rename results")
	void crossEngine_equivalentRename() {
		SpoonEngine spoon = new SpoonEngine((root, buildType) -> List.of(), (changes, dryRun) -> {
		}, new DiffGenerator());
		OpenRewriteEngine rewrite = new OpenRewriteEngine((root, buildType) -> List.of(), (changes, dryRun) -> {
		}, new DiffGenerator());

		spoon.indexProject(FIXTURE_ROOT, BuildType.UNKNOWN);
		rewrite.indexProject(FIXTURE_ROOT, BuildType.UNKNOWN);

		Symbol spoonGreeter = spoon.findSymbol(GREETER_FQN).orElseThrow();
		Symbol rewriteGreeter = rewrite.findSymbol(GREETER_FQN).orElseThrow();

		ChangeSet spoonChanges = spoon.computeRename(spoonGreeter, "com.example.simple.HelloWorld");
		ChangeSet rewriteChanges = rewrite.computeRename(rewriteGreeter, "com.example.simple.HelloWorld");

		// Both should modify the same files
		Set<String> spoonFiles = spoonChanges.changes()
			.stream()
			.map((fc) -> fc.filePath().getFileName().toString())
			.collect(Collectors.toSet());
		Set<String> rewriteFiles = rewriteChanges.changes()
			.stream()
			.map((fc) -> fc.filePath().getFileName().toString())
			.collect(Collectors.toSet());

		assertThat(spoonFiles).isEqualTo(rewriteFiles);

		// Both should have "HelloWorld" in the renamed Greeter file's new content
		assertThat(spoonChanges.changes()).anyMatch((fc) -> fc.newContent().contains("class HelloWorld"));
		assertThat(rewriteChanges.changes()).anyMatch((fc) -> fc.newContent().contains("class HelloWorld"));
	}

}
