package org.alexmond.refinej.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alexmond.refinej.core.domain.ChangeSet;
import org.alexmond.refinej.core.domain.FileChange;
import org.alexmond.refinej.core.domain.Reference;
import org.alexmond.refinej.core.domain.Symbol;
import org.alexmond.refinej.core.domain.SymbolKind;
import org.alexmond.refinej.core.domain.UsageKind;
import org.alexmond.refinej.core.engine.api.BuildType;
import org.alexmond.refinej.core.engine.api.RefactoringEngine;
import org.alexmond.refinej.core.util.DiffGenerator;
import org.alexmond.refinej.engine.rewrite.OpenRewriteEngine;
import org.alexmond.refinej.engine.spoon.SpoonEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive integration tests that exercise the full RefineJ pipeline against the
 * complex multi-package fixture project. Tests indexing, querying, rename, and move
 * operations across both implemented engines.
 */
@DisplayName("Comprehensive integration")
class ComprehensiveIntegrationTests {

	private static final Path COMPLEX_FIXTURE = Path.of("src/test/resources/fixtures/complex");

	private static final String USER_FQN = "com.example.model.User";

	private static final String ORDER_FQN = "com.example.model.Order";

	private static final String USER_SERVICE_FQN = "com.example.service.UserService";

	private static final String ENTITY_FQN = "com.example.model.Entity";

	static Stream<Arguments> implementedEngines() {
		return Stream
			.of(Arguments.of("SpoonEngine", new SpoonEngine((root, buildType) -> List.of(), (changes, dryRun) -> {
			}, new DiffGenerator())), Arguments.of("OpenRewriteEngine",
					new OpenRewriteEngine((root, buildType) -> List.of(), (changes, dryRun) -> {
					}, new DiffGenerator())));
	}

	// =========================================================================
	// Indexing — multi-package project
	// =========================================================================

	@ParameterizedTest(name = "{0} — indexes multi-package project")
	@MethodSource("implementedEngines")
	void indexProject_indexesMultiPackageProject(String name, RefactoringEngine engine) {
		engine.indexProject(COMPLEX_FIXTURE, BuildType.UNKNOWN);

		List<Symbol> symbols = engine.getAllSymbols();
		assertThat(symbols).isNotEmpty();

		// Should find classes across all packages
		Set<String> classFqns = symbols.stream()
			.filter((s) -> s.kind() == SymbolKind.CLASS)
			.map(Symbol::qualifiedName)
			.collect(Collectors.toSet());

		assertThat(classFqns).contains(USER_FQN, ORDER_FQN, USER_SERVICE_FQN, ENTITY_FQN,
				"com.example.service.OrderService", "com.example.util.StringUtils", "com.example.App");
	}

	@ParameterizedTest(name = "{0} — extracts methods from complex classes")
	@MethodSource("implementedEngines")
	void indexProject_extractsMethods(String name, RefactoringEngine engine) {
		engine.indexProject(COMPLEX_FIXTURE, BuildType.UNKNOWN);

		List<Symbol> methods = engine.getAllSymbols().stream().filter((s) -> s.kind() == SymbolKind.METHOD).toList();
		List<String> methodNames = methods.stream().map(Symbol::simpleName).toList();

		assertThat(methodNames).contains("createUser", "findById", "createOrder", "getName", "getEmail");
	}

	@ParameterizedTest(name = "{0} — extracts fields")
	@MethodSource("implementedEngines")
	void indexProject_extractsFields(String name, RefactoringEngine engine) {
		engine.indexProject(COMPLEX_FIXTURE, BuildType.UNKNOWN);

		List<Symbol> fields = engine.getAllSymbols().stream().filter((s) -> s.kind() == SymbolKind.FIELD).toList();

		assertThat(fields).anyMatch((f) -> f.simpleName().equals("name") && f.qualifiedName().startsWith(USER_FQN));
		assertThat(fields).anyMatch((f) -> f.simpleName().equals("email") && f.qualifiedName().startsWith(USER_FQN));
	}

	@ParameterizedTest(name = "{0} — finds cross-package references")
	@MethodSource("implementedEngines")
	void indexProject_findsCrossPackageReferences(String name, RefactoringEngine engine) {
		engine.indexProject(COMPLEX_FIXTURE, BuildType.UNKNOWN);

		Symbol user = engine.findSymbol(USER_FQN).orElseThrow();
		List<Reference> refs = engine.findReferences(user);

		// User is referenced from OrderService, UserService, Order, and App
		assertThat(refs).isNotEmpty();
		Set<String> refFiles = refs.stream()
			.filter((r) -> r.filePath() != null)
			.map((r) -> Path.of(r.filePath()).getFileName().toString())
			.collect(Collectors.toSet());

		// Should be referenced from at least 2 different files
		assertThat(refFiles.size()).isGreaterThanOrEqualTo(2);
	}

	@ParameterizedTest(name = "{0} — finds EXTENDS references")
	@MethodSource("implementedEngines")
	void indexProject_findsExtendsReferences(String name, RefactoringEngine engine) {
		engine.indexProject(COMPLEX_FIXTURE, BuildType.UNKNOWN);

		Symbol entity = engine.findSymbol(ENTITY_FQN).orElseThrow();
		List<Reference> refs = engine.findReferences(entity);

		// User and Order both extend Entity
		assertThat(refs).anyMatch((r) -> r.usageKind() == UsageKind.EXTENDS);
	}

	// =========================================================================
	// Rename — multi-file impact
	// =========================================================================

	@ParameterizedTest(name = "{0} — rename User affects multiple files")
	@MethodSource("implementedEngines")
	void computeRename_userAffectsMultipleFiles(String name, RefactoringEngine engine) {
		engine.indexProject(COMPLEX_FIXTURE, BuildType.UNKNOWN);

		Symbol user = engine.findSymbol(USER_FQN).orElseThrow();
		ChangeSet changeSet = engine.computeRename(user, "com.example.model.Account");

		assertThat(changeSet.changes()).hasSizeGreaterThanOrEqualTo(2);
		assertThat(changeSet.hasConflicts()).isFalse();

		// User.java should be renamed to Account.java
		FileChange userChange = changeSet.changes()
			.stream()
			.filter((fc) -> fc.filePath().toString().endsWith("User.java"))
			.findFirst()
			.orElseThrow();
		assertThat(userChange.newContent()).contains("class Account");
		assertThat(userChange.isMove()).isTrue();
		assertThat(userChange.newFilePath().toString()).endsWith("Account.java");
	}

	@ParameterizedTest(name = "{0} — rename updates import statements")
	@MethodSource("implementedEngines")
	void computeRename_updatesImports(String name, RefactoringEngine engine) {
		engine.indexProject(COMPLEX_FIXTURE, BuildType.UNKNOWN);

		Symbol user = engine.findSymbol(USER_FQN).orElseThrow();
		ChangeSet changeSet = engine.computeRename(user, "com.example.model.Account");

		// Files that import User should have updated imports
		boolean hasImportUpdate = changeSet.changes().stream().anyMatch((fc) -> {
			String fileName = fc.filePath().getFileName().toString();
			return !fileName.equals("User.java") && fc.newContent().contains("Account");
		});
		assertThat(hasImportUpdate).isTrue();
	}

	@ParameterizedTest(name = "{0} — rename method updates call sites")
	@MethodSource("implementedEngines")
	void computeRename_methodUpdatesCallSites(String name, RefactoringEngine engine) {
		engine.indexProject(COMPLEX_FIXTURE, BuildType.UNKNOWN);

		// Find the getName() method on User
		List<Symbol> symbols = engine.getAllSymbols();
		Symbol getNameMethod = symbols.stream()
			.filter((s) -> s.kind() == SymbolKind.METHOD && s.simpleName().equals("getName")
					&& s.qualifiedName().startsWith(USER_FQN))
			.findFirst()
			.orElseThrow();

		ChangeSet changeSet = engine.computeRename(getNameMethod,
				getNameMethod.qualifiedName().replace("getName", "getFullName"));

		assertThat(changeSet.changes()).isNotEmpty();
	}

	// =========================================================================
	// Move — cross-package
	// =========================================================================

	@ParameterizedTest(name = "{0} — move User to different package")
	@MethodSource("implementedEngines")
	void computeMove_userToDifferentPackage(String name, RefactoringEngine engine) {
		engine.indexProject(COMPLEX_FIXTURE, BuildType.UNKNOWN);

		Symbol user = engine.findSymbol(USER_FQN).orElseThrow();
		ChangeSet changeSet = engine.computeMove(user, "com.example.domain");

		assertThat(changeSet.changes()).isNotEmpty();
		assertThat(changeSet.hasConflicts()).isFalse();

		// User.java should have updated package declaration
		FileChange userChange = changeSet.changes()
			.stream()
			.filter((fc) -> fc.filePath().toString().endsWith("User.java"))
			.findFirst()
			.orElseThrow();
		assertThat(userChange.newContent()).contains("package com.example.domain;");
		assertThat(userChange.isMove()).isTrue();
		assertThat(userChange.newFilePath().toString()).contains("com/example/domain");
	}

	// =========================================================================
	// Cross-engine equivalence — complex fixture
	// =========================================================================

	@Test
	@DisplayName("Spoon and OpenRewrite find same classes in complex project")
	void crossEngine_sameClassesInComplexProject() {
		SpoonEngine spoon = new SpoonEngine((root, buildType) -> List.of(), (changes, dryRun) -> {
		}, new DiffGenerator());
		OpenRewriteEngine rewrite = new OpenRewriteEngine((root, buildType) -> List.of(), (changes, dryRun) -> {
		}, new DiffGenerator());

		spoon.indexProject(COMPLEX_FIXTURE, BuildType.UNKNOWN);
		rewrite.indexProject(COMPLEX_FIXTURE, BuildType.UNKNOWN);

		Set<String> spoonClasses = spoon.getAllSymbols()
			.stream()
			.filter((s) -> s.kind() == SymbolKind.CLASS)
			.map(Symbol::qualifiedName)
			.collect(Collectors.toSet());

		Set<String> rewriteClasses = rewrite.getAllSymbols()
			.stream()
			.filter((s) -> s.kind() == SymbolKind.CLASS)
			.map(Symbol::qualifiedName)
			.collect(Collectors.toSet());

		assertThat(spoonClasses).isEqualTo(rewriteClasses);
	}

	@Test
	@DisplayName("Spoon and OpenRewrite produce equivalent rename diffs")
	void crossEngine_equivalentRenameDiffs() {
		SpoonEngine spoon = new SpoonEngine((root, buildType) -> List.of(), (changes, dryRun) -> {
		}, new DiffGenerator());
		OpenRewriteEngine rewrite = new OpenRewriteEngine((root, buildType) -> List.of(), (changes, dryRun) -> {
		}, new DiffGenerator());

		spoon.indexProject(COMPLEX_FIXTURE, BuildType.UNKNOWN);
		rewrite.indexProject(COMPLEX_FIXTURE, BuildType.UNKNOWN);

		Symbol spoonUser = spoon.findSymbol(USER_FQN).orElseThrow();
		Symbol rewriteUser = rewrite.findSymbol(USER_FQN).orElseThrow();

		ChangeSet spoonChanges = spoon.computeRename(spoonUser, "com.example.model.Account");
		ChangeSet rewriteChanges = rewrite.computeRename(rewriteUser, "com.example.model.Account");

		// Both engines should touch the same set of files
		Set<String> spoonFiles = spoonChanges.changes()
			.stream()
			.map((fc) -> fc.filePath().getFileName().toString())
			.collect(Collectors.toSet());
		Set<String> rewriteFiles = rewriteChanges.changes()
			.stream()
			.map((fc) -> fc.filePath().getFileName().toString())
			.collect(Collectors.toSet());

		assertThat(spoonFiles).isEqualTo(rewriteFiles);
	}

	// =========================================================================
	// Performance sanity checks
	// =========================================================================

	@ParameterizedTest(name = "{0} — indexes complex fixture under 5s")
	@MethodSource("implementedEngines")
	void indexProject_completesWithinTimeLimit(String name, RefactoringEngine engine) {
		long start = System.currentTimeMillis();
		engine.indexProject(COMPLEX_FIXTURE, BuildType.UNKNOWN);
		long elapsed = System.currentTimeMillis() - start;

		assertThat(elapsed).isLessThan(5000);
	}

	@ParameterizedTest(name = "{0} — rename completes under 2s")
	@MethodSource("implementedEngines")
	void computeRename_completesWithinTimeLimit(String name, RefactoringEngine engine) {
		engine.indexProject(COMPLEX_FIXTURE, BuildType.UNKNOWN);
		Symbol user = engine.findSymbol(USER_FQN).orElseThrow();

		long start = System.currentTimeMillis();
		engine.computeRename(user, "com.example.model.Account");
		long elapsed = System.currentTimeMillis() - start;

		assertThat(elapsed).isLessThan(2000);
	}

}
