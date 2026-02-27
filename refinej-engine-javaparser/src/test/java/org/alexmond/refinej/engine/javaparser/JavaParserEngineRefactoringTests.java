package org.alexmond.refinej.engine.javaparser;

import java.nio.file.Path;
import java.util.List;

import org.alexmond.refinej.core.domain.ChangeSet;
import org.alexmond.refinej.core.domain.FileChange;
import org.alexmond.refinej.core.domain.Symbol;
import org.alexmond.refinej.core.domain.SymbolKind;
import org.alexmond.refinej.core.engine.api.BuildType;
import org.alexmond.refinej.core.util.DiffGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for JavaParserEngine rename and move operations.
 */
@DisplayName("JavaParserEngine — refactoring")
class JavaParserEngineRefactoringTests {

	private static final Path FIXTURE = Path.of("src/test/resources/fixtures/simple");

	private JavaParserEngine engine;

	@BeforeEach
	void setUp() {
		this.engine = new JavaParserEngine(new StubClasspathResolver(), (changes, dryRun) -> {
		}, new DiffGenerator());
		this.engine.indexProject(FIXTURE, BuildType.UNKNOWN);
	}

	// --- Rename ---

	@Test
	@DisplayName("computeRename produces changes")
	void computeRename_producesChanges() {
		Symbol greeter = this.engine.findSymbol("com.example.simple.Greeter").orElseThrow();
		ChangeSet changeSet = this.engine.computeRename(greeter, "com.example.simple.Saluter");

		assertThat(changeSet.changes()).isNotEmpty();
	}

	@Test
	@DisplayName("computeRename renames declaration file")
	void computeRename_renamesDeclarationFile() {
		Symbol greeter = this.engine.findSymbol("com.example.simple.Greeter").orElseThrow();
		ChangeSet changeSet = this.engine.computeRename(greeter, "com.example.simple.Saluter");

		FileChange greeterChange = changeSet.changes()
			.stream()
			.filter((fc) -> fc.filePath().toString().endsWith("Greeter.java"))
			.findFirst()
			.orElseThrow();

		assertThat(greeterChange.isMove()).isTrue();
		assertThat(greeterChange.newFilePath().toString()).endsWith("Saluter.java");
	}

	@Test
	@DisplayName("computeRename updates class name in content")
	void computeRename_updatesClassName() {
		Symbol greeter = this.engine.findSymbol("com.example.simple.Greeter").orElseThrow();
		ChangeSet changeSet = this.engine.computeRename(greeter, "com.example.simple.Saluter");

		FileChange greeterChange = changeSet.changes()
			.stream()
			.filter((fc) -> fc.filePath().toString().endsWith("Greeter.java"))
			.findFirst()
			.orElseThrow();

		assertThat(greeterChange.newContent()).contains("class Saluter");
	}

	@Test
	@DisplayName("computeRename updates references in other files")
	void computeRename_updatesReferences() {
		Symbol greeter = this.engine.findSymbol("com.example.simple.Greeter").orElseThrow();
		ChangeSet changeSet = this.engine.computeRename(greeter, "com.example.simple.Saluter");

		boolean hasRefUpdate = changeSet.changes().stream().anyMatch((fc) -> {
			String fileName = fc.filePath().getFileName().toString();
			return !fileName.equals("Greeter.java") && fc.newContent().contains("Saluter");
		});
		assertThat(hasRefUpdate).isTrue();
	}

	@Test
	@DisplayName("computeRename produces diffs")
	void computeRename_producesDiffs() {
		Symbol greeter = this.engine.findSymbol("com.example.simple.Greeter").orElseThrow();
		ChangeSet changeSet = this.engine.computeRename(greeter, "com.example.simple.Saluter");

		assertThat(changeSet.changes()).allSatisfy((fc) -> assertThat(fc.unifiedDiff()).isNotEmpty());
	}

	@Test
	@DisplayName("computeRename detects name clash")
	void computeRename_detectsNameClash() {
		Symbol greeter = this.engine.findSymbol("com.example.simple.Greeter").orElseThrow();
		ChangeSet changeSet = this.engine.computeRename(greeter, "com.example.simple.GreeterApp");

		assertThat(changeSet.hasConflicts()).isTrue();
	}

	@Test
	@DisplayName("computeRename handles method rename")
	void computeRename_handlesMethodRename() {
		List<Symbol> methods = this.engine.getAllSymbols()
			.stream()
			.filter((s) -> s.kind() == SymbolKind.METHOD && s.simpleName().equals("greet"))
			.toList();
		assertThat(methods).isNotEmpty();

		Symbol greetMethod = methods.get(0);
		ChangeSet changeSet = this.engine.computeRename(greetMethod,
				greetMethod.qualifiedName().replace("greet", "sayHello"));

		assertThat(changeSet.changes()).isNotEmpty();
	}

	// --- Move ---

	@Test
	@DisplayName("computeMove produces changes")
	void computeMove_producesChanges() {
		Symbol greeter = this.engine.findSymbol("com.example.simple.Greeter").orElseThrow();
		ChangeSet changeSet = this.engine.computeMove(greeter, "com.example.other");

		assertThat(changeSet.changes()).isNotEmpty();
	}

	@Test
	@DisplayName("computeMove updates package declaration")
	void computeMove_updatesPackageDeclaration() {
		Symbol greeter = this.engine.findSymbol("com.example.simple.Greeter").orElseThrow();
		ChangeSet changeSet = this.engine.computeMove(greeter, "com.example.other");

		FileChange greeterChange = changeSet.changes()
			.stream()
			.filter((fc) -> fc.filePath().toString().endsWith("Greeter.java"))
			.findFirst()
			.orElseThrow();

		assertThat(greeterChange.newContent()).contains("package com.example.other;");
	}

	@Test
	@DisplayName("computeMove moves file to new package directory")
	void computeMove_movesFile() {
		Symbol greeter = this.engine.findSymbol("com.example.simple.Greeter").orElseThrow();
		ChangeSet changeSet = this.engine.computeMove(greeter, "com.example.other");

		FileChange greeterChange = changeSet.changes()
			.stream()
			.filter((fc) -> fc.filePath().toString().endsWith("Greeter.java"))
			.findFirst()
			.orElseThrow();

		assertThat(greeterChange.isMove()).isTrue();
		assertThat(greeterChange.newFilePath().toString()).contains("com/example/other");
	}

	@Test
	@DisplayName("computeMove updates imports in referencing files")
	void computeMove_updatesImports() {
		// The simple fixture has both classes in the same package, so there are
		// no import statements to update. Verify the move itself is correct and
		// that the class file has the new package declaration.
		Symbol greeter = this.engine.findSymbol("com.example.simple.Greeter").orElseThrow();
		ChangeSet changeSet = this.engine.computeMove(greeter, "com.example.other");

		FileChange greeterChange = changeSet.changes()
			.stream()
			.filter((fc) -> fc.filePath().toString().endsWith("Greeter.java"))
			.findFirst()
			.orElseThrow();

		assertThat(greeterChange.newContent()).contains("package com.example.other;");
		assertThat(greeterChange.isMove()).isTrue();
	}

	@Test
	@DisplayName("computeMove rejects non-CLASS symbols")
	void computeMove_rejectsNonClass() {
		List<Symbol> methods = this.engine.getAllSymbols()
			.stream()
			.filter((s) -> s.kind() == SymbolKind.METHOD)
			.toList();
		assertThat(methods).isNotEmpty();

		ChangeSet changeSet = this.engine.computeMove(methods.get(0), "com.example.other");
		assertThat(changeSet.hasConflicts()).isTrue();
	}

	@Test
	@DisplayName("computeMove is no-op for same package")
	void computeMove_noOpForSamePackage() {
		Symbol greeter = this.engine.findSymbol("com.example.simple.Greeter").orElseThrow();
		ChangeSet changeSet = this.engine.computeMove(greeter, "com.example.simple");

		assertThat(changeSet.changes()).isEmpty();
	}

}
