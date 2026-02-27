package org.alexmond.refinej.engine.rewrite;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.alexmond.refinej.core.domain.Reference;
import org.alexmond.refinej.core.domain.Symbol;
import org.alexmond.refinej.core.domain.SymbolKind;
import org.alexmond.refinej.core.domain.UsageKind;
import org.alexmond.refinej.core.engine.api.BuildType;
import org.alexmond.refinej.core.util.DiffGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OpenRewriteEngine} indexing.
 *
 * <p>
 * Indexes the {@code fixtures/simple} project (Greeter + GreeterApp) and verifies that
 * symbols and references are extracted correctly.
 */
@DisplayName("OpenRewriteEngine indexing")
class OpenRewriteEngineIndexingTests {

	private static final Path FIXTURE_ROOT = Path.of("src/test/resources/fixtures/simple");

	private static final String GREETER_FQN = "com.example.simple.Greeter";

	private OpenRewriteEngine engine;

	@BeforeEach
	void setUp() {
		this.engine = new OpenRewriteEngine(new StubClasspathResolver(), (changes, dryRun) -> {
		}, new DiffGenerator());
		this.engine.indexProject(FIXTURE_ROOT, BuildType.UNKNOWN);
	}

	@Test
	@DisplayName("findSymbol returns Greeter class")
	void findSymbol_returnsGreeterClass() {
		Optional<Symbol> symbol = this.engine.findSymbol(GREETER_FQN);
		assertThat(symbol).isPresent();
		assertThat(symbol.get().kind()).isEqualTo(SymbolKind.CLASS);
		assertThat(symbol.get().simpleName()).isEqualTo("Greeter");
	}

	@Test
	@DisplayName("getAllSymbols includes Greeter and GreeterApp")
	void getAllSymbols_includesFixtureClasses() {
		List<Symbol> symbols = this.engine.getAllSymbols();
		assertThat(symbols).isNotEmpty();
		List<String> names = symbols.stream().map(Symbol::qualifiedName).toList();
		assertThat(names).contains(GREETER_FQN, "com.example.simple.GreeterApp");
	}

	@Test
	@DisplayName("getAllSymbols includes Greeter#greet() method")
	void getAllSymbols_includesGreeterMethod() {
		List<Symbol> symbols = this.engine.getAllSymbols();
		assertThat(symbols).anyMatch((s) -> s.qualifiedName().startsWith(GREETER_FQN + "#greet"));
	}

	@Test
	@DisplayName("getAllSymbols includes Greeter#name field")
	void getAllSymbols_includesGreeterField() {
		List<Symbol> symbols = this.engine.getAllSymbols();
		assertThat(symbols).anyMatch((s) -> s.qualifiedName().equals(GREETER_FQN + "#name"));
	}

	@Test
	@DisplayName("findReferences returns usages of Greeter from GreeterApp")
	void findReferences_returnsUsagesFromGreeterApp() {
		Symbol greeter = this.engine.findSymbol(GREETER_FQN).orElseThrow();
		List<Reference> refs = this.engine.findReferences(greeter);
		assertThat(refs).isNotEmpty();
	}

	@Test
	@DisplayName("findReferences includes NEW_INSTANCE usage")
	void findReferences_includesNewInstance() {
		Symbol greeter = this.engine.findSymbol(GREETER_FQN).orElseThrow();
		List<Reference> refs = this.engine.findReferences(greeter);
		assertThat(refs).anyMatch((r) -> r.usageKind() == UsageKind.NEW_INSTANCE);
	}

	@Test
	@DisplayName("clearIndex resets all state")
	void clearIndex_resetsState() {
		this.engine.clearIndex();
		assertThat(this.engine.getAllSymbols()).isEmpty();
		assertThat(this.engine.getAllReferences()).isEmpty();
		assertThat(this.engine.findSymbol(GREETER_FQN)).isEmpty();
	}

	@Test
	@DisplayName("findReferencesInFile returns refs in GreeterApp file")
	void findReferencesInFile_returnsRefsInGreeterApp() {
		Path greeterApp = FIXTURE_ROOT.resolve("src/main/java/com/example/simple/GreeterApp.java").toAbsolutePath();
		List<Reference> refs = this.engine.findReferencesInFile(greeterApp);
		assertThat(refs).isNotEmpty();
	}

}
