# RefineJ

**Safe, engine-agnostic Java refactoring from the command line.**

RefineJ indexes your Java codebase, computes rename and move refactorings with full reference tracking, and applies changes atomically with backup. It supports multiple analysis engines (Spoon, OpenRewrite) and produces structured JSON output for AI-agent integration.

## Quick Start

```bash
# Clone and build
git clone https://github.com/alexmond/refinej.git
cd refinej
./scripts/mvnw.sh package -pl refinej-cli -DskipTests

# Index a project
java -jar refinej-cli/target/refinej-cli-0.1.0-SNAPSHOT.jar index --path /path/to/your/project

# Preview a rename
java -jar refinej-cli/target/refinej-cli-0.1.0-SNAPSHOT.jar refactor rename \
  --old com.example.Foo --new com.example.Bar --preview

# Apply the rename
java -jar refinej-cli/target/refinej-cli-0.1.0-SNAPSHOT.jar refactor rename \
  --old com.example.Foo --new com.example.Bar --yes
```

## Command Reference

| Command | Description |
|---------|-------------|
| `refinej index --path <dir>` | Index a Java project (symbols + references) |
| `refinej reindex --path <dir>` | Re-index (full rebuild) |
| `refinej query symbol --name <fqn>` | Look up a symbol by fully-qualified name |
| `refinej query refs --name <fqn>` | Find all references to a symbol |
| `refinej refactor rename --old <fqn> --new <fqn>` | Rename a class, method, or field |
| `refinej refactor move --class <fqn> --to <pkg>` | Move a class to a different package |
| `refinej status` | Show engine and index statistics |

### Global Options

| Option | Description |
|--------|-------------|
| `--engine <spoon\|rewrite>` | Override the analysis engine (default: `spoon`) |
| `--json` | Output as structured JSON |
| `--preview` | Compute changes without applying (dry run) |
| `--yes` | Apply changes without confirmation |

## Engines

RefineJ supports multiple Java analysis engines behind a unified interface:

| Engine | Library | Status | Best For |
|--------|---------|--------|----------|
| **Spoon** | [Spoon 10.4](https://spoon.gforge.inria.fr/) | Full | Interactive refactoring, precise position tracking |
| **OpenRewrite** | [OpenRewrite 8.73](https://docs.openrewrite.org/) | Full | Batch migrations, recipe-based transforms |
| **JavaParser** | [JavaParser 3.25](https://javaparser.org/) | Stub | Lightweight analysis (planned) |

Switch engines per command with `--engine`:
```bash
refinej index --path ./myproject --engine rewrite
refinej refactor rename --old com.Foo --new com.Bar --engine rewrite --preview
```

Or set the default in `application.yml`:
```yaml
refinej:
  default-engine: spoon  # spoon | rewrite | javaparser
```

## AI-Agent Integration

RefineJ is designed for use by AI coding assistants (Claude Code, Cursor, Aider, etc.). Use `--json` output for structured responses:

```bash
# Step 1: Query references before suggesting a rename
refinej query refs --name com.example.Foo --json

# Step 2: Preview the change set
refinej refactor rename --old com.example.Foo --new com.example.Bar --preview --json

# Step 3: Apply if no conflicts
refinej refactor rename --old com.example.Foo --new com.example.Bar --yes --json
```

See [docs/AI-AGENT-GUIDE.md](docs/AI-AGENT-GUIDE.md) for the full integration guide with JSON schemas and error handling.

## Build from Source

**Requirements:** JDK 21+

```bash
# Build all modules (skip tests)
./scripts/mvnw.sh clean install -DskipTests

# Run the full test suite (111 tests)
./scripts/mvnw.sh verify

# Run tests for a specific module
./scripts/mvnw.sh test -pl refinej-engine-spoon

# Build the executable JAR
./scripts/mvnw.sh package -pl refinej-cli -DskipTests
java -jar refinej-cli/target/refinej-cli-0.1.0-SNAPSHOT.jar --help
```

### Project Structure

```
refinej-parent/
  refinej-core/              Shared domain, engine API, services, utilities
  refinej-cli/               Spring Boot + Picocli CLI entry point
  refinej-engine-spoon/      Spoon-based engine implementation
  refinej-engine-rewrite/    OpenRewrite-based engine implementation
  refinej-engine-javaparser/ JavaParser-based engine (stub)
```

Engine modules depend only on `refinej-core` — never on each other or on `refinej-cli`.

## How It Works

1. **Index**: Parse Java source files using the selected engine, extract symbols (classes, methods, fields) and references (imports, type usages, method calls), store in an embedded H2 database.

2. **Query**: Look up symbols by fully-qualified name, find all references with usage kind filtering.

3. **Refactor**: Compute a `ChangeSet` (file diffs) using indexed reference positions. Detect conflicts (name clashes, read-only files). Changes are computed purely — no files are modified until `apply()`.

4. **Apply**: Back up affected files to `.refinej/backups/`, then write changes atomically using temp-file + rename.

## License

See [LICENSE](LICENSE) for details.
