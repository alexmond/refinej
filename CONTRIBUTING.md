# Contributing to RefineJ

Thank you for your interest in contributing to RefineJ!

## Getting Started

1. **Fork** the repository on GitHub
2. **Clone** your fork: `git clone https://github.com/<you>/refinej.git`
3. **Build** the project: `./scripts/mvnw.sh clean install -DskipTests`
4. **Run tests**: `./scripts/mvnw.sh verify`

## Development Workflow

1. Create a branch from `main`: `git checkout -b rfj-XXX/short-description`
2. Implement the change (see [docs/ROADMAP.adoc](docs/ROADMAP.adoc) for open issues)
3. Run `./scripts/mvnw.sh verify` — all tests must pass
4. Commit with a descriptive message referencing the ticket number
5. Push and open a Pull Request against `main`

## Commit Style

- Short imperative subject line (< 72 characters)
- Reference the ticket: `Add conflict detection for rename (RFJ-045)`
- Body explains **why**, not what (the diff shows what)

## Code Style

RefineJ uses [Spring Java Format](https://github.com/spring-io/spring-javaformat) and Checkstyle. Key rules:

- **Java 21** — use records, pattern matching, sealed classes where appropriate
- **Imports**: `java.*` then blank line, `*` (all other), then blank line, `org.springframework.*`
- **Naming**: `this.` prefix for instance fields, `(x) ->` for lambdas (parens required)
- **Braces**: always use braces for `if`/`else`/`for`/`while`
- **Test classes** must end with `Tests` (not `Test`)
- **Catch variables** must be named `ex` (not `e`)

Auto-format before committing:
```bash
./scripts/mvnw.sh spring-javaformat:apply
```

## Module Rules

```
refinej-core           <- shared library (NO engine-specific imports)
refinej-engine-spoon   <- depends only on refinej-core
refinej-engine-rewrite <- depends only on refinej-core
refinej-engine-javaparser <- depends only on refinej-core
refinej-cli            <- depends on core + all 3 engine modules
```

Engine modules must **never** depend on each other or on `refinej-cli`.

## Adding a New Engine Operation

Use the project convention to keep all three engines in sync:

1. Add the method to `RefactoringEngine` interface in `refinej-core`
2. Stub the method in all three engine implementations
3. Add a contract test assertion in `EngineContractTests`
4. Implement the method in each engine

## Running Tests

```bash
# Full suite (111+ tests)
./scripts/mvnw.sh verify

# Single module
./scripts/mvnw.sh test -pl refinej-engine-spoon

# Single test class
./scripts/mvnw.sh test -pl refinej-cli -Dtest=EngineContractTests
```

## Reporting Issues

Open an issue on [GitHub Issues](https://github.com/alexmond/refinej/issues) with:
- Steps to reproduce
- Expected vs actual behavior
- Java version and OS

## License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE).
