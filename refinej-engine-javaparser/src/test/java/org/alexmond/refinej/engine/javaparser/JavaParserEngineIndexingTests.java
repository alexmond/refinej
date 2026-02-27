package org.alexmond.refinej.engine.javaparser;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
 * Integration tests for JavaParserEngine indexing against the simple fixture project.
 */
@DisplayName("JavaParserEngine — indexing")
class JavaParserEngineIndexingTests {

	private static final Path FIXTURE = Path.of("src/test/resources/fixtures/simple");

	private JavaParserEngine engine;

	@BeforeEach
	void setUp() {
		this.engine = new JavaParserEngine(new StubClasspathResolver(), (changes, dryRun) -> {
		}, new DiffGenerator());
		this.engine.indexProject(FIXTURE, BuildType.UNKNOWN);
	}

	@Test
	@DisplayName("findSymbol returns Greeter class")
	void findSymbol_returnsGreeterClass() {
		assertThat(this.engine.findSymbol("com.example.simple.Greeter")).isPresent()
			.hasValueSatisfying((s) -> assertThat(s.kind()).isEqualTo(SymbolKind.CLASS));
	}

	@Test
	@DisplayName("getAllSymbols includes both fixture classes")
	void getAllSymbols_includesBothClasses() {
		Set<String> classNames = this.engine.getAllSymbols()
			.stream()
			.filter((s) -> s.kind() == SymbolKind.CLASS)
			.map(Symbol::qualifiedName)
			.collect(Collectors.toSet());

		assertThat(classNames).contains("com.example.simple.Greeter", "com.example.simple.GreeterApp");
	}

	@Test
	@DisplayName("getAllSymbols includes methods")
	void getAllSymbols_includesMethods() {
		List<String> methodNames = this.engine.getAllSymbols()
			.stream()
			.filter((s) -> s.kind() == SymbolKind.METHOD)
			.map(Symbol::simpleName)
			.toList();

		assertThat(methodNames).contains("greet");
	}

	@Test
	@DisplayName("getAllSymbols includes fields")
	void getAllSymbols_includesFields() {
		List<Symbol> fields = this.engine.getAllSymbols().stream().filter((s) -> s.kind() == SymbolKind.FIELD).toList();

		assertThat(fields).isNotEmpty();
	}

	@Test
	@DisplayName("findReferences returns usages of Greeter")
	void findReferences_returnsGreeterUsages() {
		Symbol greeter = this.engine.findSymbol("com.example.simple.Greeter").orElseThrow();
		List<Reference> refs = this.engine.findReferences(greeter);

		assertThat(refs).isNotEmpty();
	}

	@Test
	@DisplayName("findReferences includes NEW_INSTANCE usage")
	void findReferences_includesNewInstance() {
		Symbol greeter = this.engine.findSymbol("com.example.simple.Greeter").orElseThrow();
		List<Reference> refs = this.engine.findReferences(greeter);

		assertThat(refs).anyMatch((r) -> r.usageKind() == UsageKind.NEW_INSTANCE);
	}

	@Test
	@DisplayName("clearIndex removes all symbols")
	void clearIndex_removesAll() {
		this.engine.clearIndex();

		assertThat(this.engine.getAllSymbols()).isEmpty();
		assertThat(this.engine.getAllReferences()).isEmpty();
	}

	@Test
	@DisplayName("findReferencesInFile returns refs for a specific file")
	void findReferencesInFile_returnsRefs() {
		Path appFile = FIXTURE.resolve("src/main/java/com/example/simple/GreeterApp.java").toAbsolutePath();
		List<Reference> refs = this.engine.findReferencesInFile(appFile);

		assertThat(refs).isNotEmpty();
	}

}
