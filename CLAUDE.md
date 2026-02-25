# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

No Maven wrapper is present yet — use system `mvn`:

```bash
# Build all modules
mvn clean install -DskipTests

# Build + run tests
mvn clean verify

# Run a single test class
mvn test -pl refinej-core -Dtest=SymbolRepositoryTest

# Run the CLI (interactive Spring Shell)
mvn spring-boot:run -pl refinej-cli

# Build executable JAR
mvn package -pl refinej-cli -DskipTests
java -jar refinej-cli/target/refinej-cli-0.1.0-SNAPSHOT.jar
```

The active engine is set via `refinej-cli/src/main/resources/application.yml` (`refinej.default-engine: spoon`) or overridden at runtime with `--engine <spoon|rewrite|javaparser>`.

## Module Structure & Dependency Rules

```
refinej-parent (root pom, no code)
├── refinej-core          ← shared library: domain records, RefactoringEngine interface,
│                            services, JPA repositories, utils — NO engine-specific code
├── refinej-cli           ← Spring Boot entry point; owns application.yml, H2/JPA/Flyway config,
│                            Spring Shell commands; depends on core + ALL 3 engine modules
├── refinej-engine-spoon      ← implements RefactoringEngine; depends on core only
├── refinej-engine-rewrite    ← implements RefactoringEngine; depends on core only
└── refinej-engine-javaparser ← implements RefactoringEngine; depends on core only
```

**Critical rule**: Engine modules must depend only on `refinej-core`. Never add engine-specific imports to `refinej-core` or `refinej-cli`.

## Key Versions (from parent pom.xml)

- Spring Boot: 4.0.2
- Java: 21
- Spoon: 10.4.0 (`fr.inria.gforge.spoon:spoon-core`)
- OpenRewrite: 8.73.2 (`org.openrewrite:rewrite-java`)
- JavaParser: 3.25.8 (`com.github.javaparser:javaparser-symbol-solver-core`)

## Architecture: What Goes Where

### `refinej-core`
- `domain/` — `Symbol`, `Reference`, `ChangeSet`, `FileChange`, `Conflict` (Java records), `SymbolKind`, `UsageKind` enums
- `engine/api/` — `RefactoringEngine` interface, `EngineType`, `BuildType`, `ClasspathResolver`, `ChangeApplier`
- `repository/` — `SymbolRepository`, `ReferenceRepository` (Spring Data JPA), `SymbolEntity`, `ReferenceEntity`
- `service/` — `IndexingService`, `QueryService`, `RefactoringService`
- `config/` — `EngineConfig` (factory that selects the active `RefactoringEngine` bean)
- `model/` — JSON DTOs for `--json` output
- `util/` — `ClasspathResolverImpl`, `DiffGenerator`, `FileBackupManager`
- `exception/` — `RefactorException` and subclasses

### `refinej-cli`
- `RefineJApplication` — `@SpringBootApplication` entry point
- `commands/` — Spring Shell `@ShellComponent` classes: `IndexCommands`, `QueryCommands`, `RefactorCommands`
- `application.yml` — all runtime configuration; H2 file URL, engine selection, backup settings

### Engine modules
Each contains a single class implementing `RefactoringEngine`. The implementation uses the engine library's AST/model directly (Spoon `CtModel`, OpenRewrite `LST`, JavaParser `CompilationUnit`).

## RefactoringEngine Contract

All three engines must implement the same interface. New operations are added to the interface first, then implemented in all three engines. The `EngineConfig` factory in `refinej-core` selects the bean at startup based on `refinej.default-engine`.

`ChangeSet` is always computed first (pure, no file writes). `apply()` writes files only when `dryRun=false`. All apply operations backup files first via `FileBackupManager`.

## H2 Database

The index database is stored at `.refinej/index` (H2 file mode) in the directory where `refinej` is invoked. Schema is managed by Flyway migrations in `refinej-cli/src/main/resources/db/migration/`. The JPA entities (`SymbolEntity`, `ReferenceEntity`) live in `refinej-core` but the datasource is configured from `refinej-cli`.

## Current Status

The skeleton is in place — all Java files are stubs with `// TODO` comments. Active work starts with:
1. `RefactoringEngine` interface (`refinej-core/engine/api/`)
2. Domain records (`refinej-core/domain/`)
3. `EngineConfig` factory (`refinej-core/config/`)
4. `SpoonEngine` implementation (`refinej-engine-spoon/`)

See `docs/ROADMAP.adoc` for the full phased plan. See `docs/DETAILED-DESIGN.adoc` for complete interface signatures, sequence diagrams, and the full package tree.
