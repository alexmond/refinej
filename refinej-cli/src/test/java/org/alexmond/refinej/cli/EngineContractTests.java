package org.alexmond.refinej.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.alexmond.refinej.core.engine.api.BuildType;
import org.alexmond.refinej.core.engine.api.RefactoringEngine;
import org.alexmond.refinej.core.util.DiffGenerator;
import org.alexmond.refinej.engine.javaparser.JavaParserEngine;
import org.alexmond.refinej.engine.rewrite.OpenRewriteEngine;
import org.alexmond.refinej.engine.spoon.SpoonEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Contract test: the same assertions run against all three engine implementations.
 *
 * <p>
 * Phase 1 assertions cover the stub-phase behaviour. More assertions are added
 * incrementally as engines are implemented (Phase 2+).
 */
@DisplayName("RefactoringEngine contract")
class EngineContractTests {

	private static final Path FIXTURE_ROOT = Path.of("src/test/resources/fixtures/simple");

	static Stream<Arguments> allEngines() {
		return Stream
			.of(Arguments.of("SpoonEngine", new SpoonEngine((root, buildType) -> List.of(), (changes, dryRun) -> {
			}, new DiffGenerator())), Arguments.of("OpenRewriteEngine", new OpenRewriteEngine()),
					Arguments.of("JavaParserEngine", new JavaParserEngine()));
	}

	// -------------------------------------------------------------------------
	// 1. getType() returns a non-null EngineType
	// -------------------------------------------------------------------------

	@ParameterizedTest(name = "{0} — getType is non-null")
	@MethodSource("allEngines")
	void getType_isNonNull(String engineName, RefactoringEngine engine) {
		assertThat(engine.getType()).isNotNull();
	}

	// -------------------------------------------------------------------------
	// 2. indexProject does not throw (graceful no-op in stub phase)
	// -------------------------------------------------------------------------

	@ParameterizedTest(name = "{0} — indexProject does not throw")
	@MethodSource("allEngines")
	void indexProject_doesNotThrow(String engineName, RefactoringEngine engine) {
		assertThatNoException().isThrownBy(() -> engine.indexProject(FIXTURE_ROOT, BuildType.UNKNOWN));
	}

	// -------------------------------------------------------------------------
	// 3. findSymbol returns empty before indexing
	// -------------------------------------------------------------------------

	@ParameterizedTest(name = "{0} — findSymbol returns empty before index")
	@MethodSource("allEngines")
	void findSymbol_returnsEmpty_whenNotIndexed(String engineName, RefactoringEngine engine) {
		assertThat(engine.findSymbol("com.example.simple.Greeter")).isEmpty();
	}

	// -------------------------------------------------------------------------
	// 4. findReferences returns empty list before indexing
	// -------------------------------------------------------------------------

	@ParameterizedTest(name = "{0} — findReferences returns empty list before index")
	@MethodSource("allEngines")
	void findReferences_returnsEmptyList_whenNotIndexed(String engineName, RefactoringEngine engine) {
		// Create a dummy symbol — engines must return empty, not throw
		var dummySymbol = new org.alexmond.refinej.core.domain.Symbol(1L,
				org.alexmond.refinej.core.domain.SymbolKind.CLASS, "Greeter", "com.example.simple.Greeter", null, 0, 0,
				null);
		assertThat(engine.findReferences(dummySymbol)).isEmpty();
	}

	// -------------------------------------------------------------------------
	// 5. clearIndex does not throw
	// -------------------------------------------------------------------------

	@ParameterizedTest(name = "{0} — clearIndex does not throw")
	@MethodSource("allEngines")
	void clearIndex_doesNotThrow(String engineName, RefactoringEngine engine) {
		assertThatNoException().isThrownBy(engine::clearIndex);
	}

	// -------------------------------------------------------------------------
	// 6. getAllSymbols / getAllReferences return empty before indexing
	// -------------------------------------------------------------------------

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

	// -------------------------------------------------------------------------
	// 7. index then clear resets state
	// -------------------------------------------------------------------------

	@ParameterizedTest(name = "{0} — clearIndex resets getAllSymbols")
	@MethodSource("allEngines")
	void clearIndex_resetsState(String engineName, RefactoringEngine engine) {
		engine.indexProject(FIXTURE_ROOT, BuildType.UNKNOWN);
		engine.clearIndex();
		assertThat(engine.getAllSymbols()).isEmpty();
		assertThat(engine.getAllReferences()).isEmpty();
	}

}
