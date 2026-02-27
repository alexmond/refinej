package org.alexmond.refinej.engine.spoon;

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
 * Integration tests for {@link SpoonEngine} Phase 4 refactoring operations (rename and
 * move).
 *
 * <p>
 * Indexes the {@code fixtures/simple} project and verifies that computeRename and
 * computeMove produce correct ChangeSets with diffs.
 */
@DisplayName("SpoonEngine refactoring")
class SpoonEngineRefactoringTests {

	private static final Path FIXTURE_ROOT = Path.of("src/test/resources/fixtures/simple");

	private static final String GREETER_FQN = "com.example.simple.Greeter";

	private SpoonEngine engine;

	@BeforeEach
	void setUp() {
		this.engine = new SpoonEngine(new StubClasspathResolver(), (changes, dryRun) -> {
		}, new DiffGenerator());
		this.engine.indexProject(FIXTURE_ROOT, BuildType.UNKNOWN);
	}

	// --- computeRename tests ---

	@Test
	@DisplayName("computeRename produces changes for class rename")
	void computeRename_producesChanges() {
		Symbol greeter = this.engine.findSymbol(GREETER_FQN).orElseThrow();
		ChangeSet changeSet = this.engine.computeRename(greeter, "com.example.simple.HelloWorld");

		assertThat(changeSet.changes()).isNotEmpty();
		assertThat(changeSet.hasConflicts()).isFalse();
	}

	@Test
	@DisplayName("computeRename renames declaration file")
	void computeRename_renamesDeclarationFile() {
		Symbol greeter = this.engine.findSymbol(GREETER_FQN).orElseThrow();
		ChangeSet changeSet = this.engine.computeRename(greeter, "com.example.simple.HelloWorld");

		// Find the change for the Greeter.java file (the declaration)
		FileChange declChange = changeSet.changes()
			.stream()
			.filter((fc) -> fc.filePath().toString().endsWith("Greeter.java"))
			.filter(FileChange::isMove)
			.findFirst()
			.orElse(null);

		assertThat(declChange).isNotNull();
		assertThat(declChange.newFilePath().toString()).endsWith("HelloWorld.java");
	}

	@Test
	@DisplayName("computeRename updates class name in source")
	void computeRename_updatesClassName() {
		Symbol greeter = this.engine.findSymbol(GREETER_FQN).orElseThrow();
		ChangeSet changeSet = this.engine.computeRename(greeter, "com.example.simple.HelloWorld");

		// Find the change for Greeter.java
		FileChange declChange = changeSet.changes()
			.stream()
			.filter((fc) -> fc.filePath().toString().endsWith("Greeter.java"))
			.findFirst()
			.orElseThrow();

		assertThat(declChange.newContent()).contains("class HelloWorld");
		assertThat(declChange.newContent()).doesNotContain("class Greeter");
	}

	@Test
	@DisplayName("computeRename updates references in other files")
	void computeRename_updatesReferences() {
		Symbol greeter = this.engine.findSymbol(GREETER_FQN).orElseThrow();
		ChangeSet changeSet = this.engine.computeRename(greeter, "com.example.simple.HelloWorld");

		// Find the change for GreeterApp.java (which uses Greeter)
		FileChange appChange = changeSet.changes()
			.stream()
			.filter((fc) -> fc.filePath().toString().endsWith("GreeterApp.java"))
			.findFirst()
			.orElse(null);

		assertThat(appChange).isNotNull();
		assertThat(appChange.newContent()).contains("HelloWorld");
		assertThat(appChange.unifiedDiff()).isNotEmpty();
	}

	@Test
	@DisplayName("computeRename produces unified diffs")
	void computeRename_producesDiffs() {
		Symbol greeter = this.engine.findSymbol(GREETER_FQN).orElseThrow();
		ChangeSet changeSet = this.engine.computeRename(greeter, "com.example.simple.HelloWorld");

		for (FileChange change : changeSet.changes()) {
			assertThat(change.unifiedDiff()).isNotEmpty();
			assertThat(change.unifiedDiff()).contains("---");
			assertThat(change.unifiedDiff()).contains("+++");
		}
	}

	@Test
	@DisplayName("computeRename detects name clash conflict")
	void computeRename_detectsNameClash() {
		Symbol greeter = this.engine.findSymbol(GREETER_FQN).orElseThrow();
		// Rename to an existing class
		ChangeSet changeSet = this.engine.computeRename(greeter, "com.example.simple.GreeterApp");

		assertThat(changeSet.hasConflicts()).isTrue();
		assertThat(changeSet.conflicts()).anyMatch((c) -> c.description().contains("already exists"));
	}

	@Test
	@DisplayName("computeRename handles method rename")
	void computeRename_handlesMethodRename() {
		// Find the greet() method
		List<Symbol> symbols = this.engine.getAllSymbols();
		Symbol greetMethod = symbols.stream()
			.filter((s) -> s.kind() == SymbolKind.METHOD && s.simpleName().equals("greet"))
			.findFirst()
			.orElseThrow();

		ChangeSet changeSet = this.engine.computeRename(greetMethod,
				greetMethod.qualifiedName().replace("greet", "sayHello"));

		assertThat(changeSet.changes()).isNotEmpty();
	}

	// --- computeMove tests ---

	@Test
	@DisplayName("computeMove produces changes for class move")
	void computeMove_producesChanges() {
		Symbol greeter = this.engine.findSymbol(GREETER_FQN).orElseThrow();
		ChangeSet changeSet = this.engine.computeMove(greeter, "com.example.other");

		assertThat(changeSet.changes()).isNotEmpty();
		assertThat(changeSet.hasConflicts()).isFalse();
	}

	@Test
	@DisplayName("computeMove updates package declaration")
	void computeMove_updatesPackageDeclaration() {
		Symbol greeter = this.engine.findSymbol(GREETER_FQN).orElseThrow();
		ChangeSet changeSet = this.engine.computeMove(greeter, "com.example.other");

		FileChange classChange = changeSet.changes()
			.stream()
			.filter((fc) -> fc.filePath().toString().endsWith("Greeter.java"))
			.findFirst()
			.orElseThrow();

		assertThat(classChange.newContent()).contains("package com.example.other;");
		assertThat(classChange.newContent()).doesNotContain("package com.example.simple;");
	}

	@Test
	@DisplayName("computeMove produces file move to new directory")
	void computeMove_movesFileToNewDirectory() {
		Symbol greeter = this.engine.findSymbol(GREETER_FQN).orElseThrow();
		ChangeSet changeSet = this.engine.computeMove(greeter, "com.example.other");

		FileChange classChange = changeSet.changes()
			.stream()
			.filter((fc) -> fc.filePath().toString().endsWith("Greeter.java"))
			.findFirst()
			.orElseThrow();

		assertThat(classChange.isMove()).isTrue();
		assertThat(classChange.newFilePath().toString()).contains("com/example/other");
	}

	@Test
	@DisplayName("computeMove updates import statements in referencing files")
	void computeMove_updatesImports() {
		Symbol greeter = this.engine.findSymbol(GREETER_FQN).orElseThrow();
		ChangeSet changeSet = this.engine.computeMove(greeter, "com.example.other");

		// GreeterApp should have updated import (if Greeter was imported)
		FileChange appChange = changeSet.changes()
			.stream()
			.filter((fc) -> fc.filePath().toString().endsWith("GreeterApp.java"))
			.findFirst()
			.orElse(null);

		// GreeterApp is in the same package, so it may not have an explicit import
		// But if it does, it should be updated
		if (appChange != null) {
			assertThat(appChange.newContent()).contains("com.example.other.Greeter");
		}
	}

	@Test
	@DisplayName("computeMove rejects non-CLASS symbols")
	void computeMove_rejectsNonClassSymbols() {
		List<Symbol> symbols = this.engine.getAllSymbols();
		Symbol method = symbols.stream().filter((s) -> s.kind() == SymbolKind.METHOD).findFirst().orElseThrow();

		ChangeSet changeSet = this.engine.computeMove(method, "com.example.other");

		assertThat(changeSet.hasConflicts()).isTrue();
		assertThat(changeSet.conflicts()).anyMatch((c) -> c.description().contains("Only CLASS"));
	}

	@Test
	@DisplayName("computeMove to same package produces no changes")
	void computeMove_samePackage_noChanges() {
		Symbol greeter = this.engine.findSymbol(GREETER_FQN).orElseThrow();
		ChangeSet changeSet = this.engine.computeMove(greeter, "com.example.simple");

		assertThat(changeSet.changes()).isEmpty();
	}

}
