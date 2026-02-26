# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

No system `mvn` — use the project wrapper at `scripts/mvnw.sh`:

```bash
# Build all modules (skip tests)
./scripts/mvnw.sh clean install -DskipTests

# Build + run all tests
./scripts/mvnw.sh verify

# Run a single test class
./scripts/mvnw.sh test -pl refinej-cli -Dtest=EngineContractTest

# Run tests for one module
./scripts/mvnw.sh test -pl refinej-cli

# Build executable JAR
./scripts/mvnw.sh package -pl refinej-cli -DskipTests
java -jar refinej-cli/target/refinej-cli-0.1.0-SNAPSHOT.jar
```

The active engine is set via `refinej-cli/src/main/resources/application.yml`
(`refinej.default-engine: spoon`) or overridden at runtime with `--engine <spoon|rewrite|javaparser>`.

## Available Slash Commands (Skills)

| Command | Description |
|---------|-------------|
| `/build` | Build all modules (skip tests) |
| `/test` | Run full test suite |
| `/test-module <module>` | Run tests for a specific module |
| `/test-contract` | Run EngineContractTest (3 engines × 8 assertions = 24 tests) |
| `/implement <feature>` | Guided implementation with module-placement rules |
| `/add-engine-op <sig>` | Add a new operation to the engine interface + all 3 stubs |
| `/add-test <what>` | Add a JUnit 5 test following project conventions |
| `/review` | Review current diff for dependency violations, style, coverage |
| `/roadmap` | Show current phase status and next recommended tasks |
| `/engine-status` | Table of implemented vs. stubbed methods per engine |
| `/new-ticket <desc>` | Create a GitHub issue with proper labels/milestone |
| `/close-ticket <number>` | Close an issue with status-done label |
| `/explain <topic>` | Explain a class, concept, or design decision |
| `/architecture` | Full architectural overview |
| `/learn-spoon` | How to use Spoon 10.x for SpoonEngine |
| `/learn-openrewrite` | How to use OpenRewrite 8.x for OpenRewriteEngine |
| `/learn-javaparser` | How to use JavaParser 3.x for JavaParserEngine |

## Module Structure & Dependency Rules

```
refinej-parent (root pom, no code)
├── refinej-core          ← shared library: domain records, RefactoringEngine interface,
│                            services, utils — NO engine-specific code
├── refinej-cli           ← Spring Boot + Picocli entry point; owns application.yml,
│                            Picocli commands; depends on core + ALL 3 engine modules
├── refinej-engine-spoon      ← implements RefactoringEngine; depends on core only
├── refinej-engine-rewrite    ← implements RefactoringEngine; depends on core only
└── refinej-engine-javaparser ← implements RefactoringEngine; depends on core only
```

**Critical rule**: Engine modules must depend only on `refinej-core`. Never add
engine-specific imports to `refinej-core` or `refinej-cli`.

## Key Versions (from parent pom.xml)

- Spring Boot: 4.0.2
- Java: 21
- Picocli: 4.7.6 (CLI framework — not Spring Shell)
- Lombok: managed by Spring Boot BOM (`@Slf4j`, `@RequiredArgsConstructor`)
- Spoon: 10.4.0 (`fr.inria.gforge.spoon:spoon-core`)
- OpenRewrite: 8.73.2 (`org.openrewrite:rewrite-java`)
- JavaParser: 3.25.8 (`com.github.javaparser:javaparser-symbol-solver-core`)

## Architecture: What Goes Where

### `refinej-core`
- `domain/` — `Symbol`, `Reference`, `ChangeSet`, `FileChange`, `Conflict` (Java records), `SymbolKind`, `UsageKind` enums
- `engine/api/` — `RefactoringEngine` interface, `EngineType`, `BuildType`, `ClasspathResolver`, `ChangeApplier`
- `service/` — `IndexingService`, `QueryService`, `RefactoringService` (Phase 3+)
- `config/` — `EngineConfig` (factory that selects the active `RefactoringEngine` bean)
- `model/` — JSON DTOs for `--json` output (`JsonDto`)
- `util/` — `ClasspathResolverImpl`, `DiffGenerator`, `FileBackupManager`
- `exception/` — `RefactorException` and subclasses

### `refinej-cli`
- `RefineJApplication` — `@SpringBootApplication` entry point; starts Spring, then Picocli
- `commands/` — Picocli `@Command @Component` classes (each implements `Callable<Integer>`)
- `commands/SpringPicocliFactory` — bridges Spring DI into Picocli command instantiation
- `application.yml` — engine selection, logging, indexing config

### Engine modules
Each contains a single class implementing `RefactoringEngine`. The implementation
uses the engine library's AST/model directly (Spoon `CtModel`, OpenRewrite `LST`,
JavaParser `CompilationUnit`). JPA persistence is deferred to Phase 3.

## CLI Command Hierarchy

```
refinej --help | --version
refinej index    [--path] [--engine] [--json]
refinej reindex  [--path] [--changed-only] [--engine] [--json]
refinej query
  symbol         --name <fqn> [--engine] [--json]
  refs           --name <fqn> [--kind] [--engine] [--json]
refinej refactor
  rename         --old <fqn> --new <fqn> [--preview] [--yes] [--engine] [--json]
  move           --class <fqn> --to <pkg> [--preview] [--yes] [--engine] [--json]
refinej status   [--json]
```

## Code Style

- Use `@Slf4j` (Lombok) for loggers — no manual `LoggerFactory.getLogger`
- Use `@RequiredArgsConstructor` for Spring beans; use `@Autowired` fields in Picocli commands
- Domain model: Java records (immutable, no setters)
- Tests: JUnit 5 + AssertJ; avoid Mockito unless strictly necessary

## RefactoringEngine Contract

All three engines must implement the same interface. New operations are added to
the interface first (`refinej-core`), then stubbed in all three engines, then a
contract test assertion is added to `EngineContractTest`. Use `/add-engine-op` to
do this consistently.

`ChangeSet` is always computed first (pure, no file writes). `apply()` writes files
only when `dryRun=false`. All apply operations backup files first via `FileBackupManager`.

## Current Status (Phase 1 complete)

Phase 1 is done:
- All domain records, enums, engine API interfaces implemented in `refinej-core`
- All 3 engine stubs implemented with proper `@Slf4j` logging
- `EngineConfig` factory, `EngineResolver`, `SpringPicocliFactory` all wired
- `EngineContractTest`: 24 tests pass (3 engines × 8 assertions)
- CLI commands: Picocli-based, all compile and start correctly

Next: Phase 2 — SpoonEngine real implementation (RFJ-020 through RFJ-025).
Use `/roadmap` to see open issues, `/learn-spoon` to understand the library.

See `docs/ROADMAP.adoc` for the full phased plan.
See `docs/DETAILED-DESIGN.adoc` for complete interface signatures and package tree.
