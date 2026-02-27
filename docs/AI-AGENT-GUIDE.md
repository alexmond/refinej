# RefineJ AI-Agent Integration Guide

This guide explains how AI coding assistants (Claude Code, Cursor, Aider, GitHub Copilot Workspace) should use RefineJ for safe Java refactoring.

## Recommended Workflow

### Step 1: Index the Project

Before any refactoring, ensure the project is indexed:

```bash
refinej index --path /path/to/project --json
```

**Response:**
```json
{
  "status": "ok",
  "engine": "spoon",
  "symbolCount": 142,
  "referenceCount": 587,
  "durationMs": 1234
}
```

### Step 2: Query References

Before suggesting a rename, check how widely the symbol is used:

```bash
refinej query refs --name com.example.service.UserService --json
```

**Response:**
```json
{
  "status": "ok",
  "symbol": "com.example.service.UserService",
  "references": [
    {
      "filePath": "/src/main/java/com/example/controller/UserController.java",
      "line": 5,
      "column": 0,
      "usageKind": "IMPORT"
    },
    {
      "filePath": "/src/main/java/com/example/controller/UserController.java",
      "line": 12,
      "column": 0,
      "usageKind": "TYPE_REFERENCE"
    }
  ],
  "totalCount": 2
}
```

### Step 3: Preview the Refactoring

Compute the change set without applying:

```bash
refinej refactor rename \
  --old com.example.service.UserService \
  --new com.example.service.AccountService \
  --preview --json
```

**Response:**
```json
{
  "status": "ok",
  "changes": [
    {
      "filePath": "/src/main/java/com/example/service/UserService.java",
      "newFilePath": "/src/main/java/com/example/service/AccountService.java",
      "isMove": true,
      "unifiedDiff": "--- UserService.java\n+++ AccountService.java\n..."
    },
    {
      "filePath": "/src/main/java/com/example/controller/UserController.java",
      "isMove": false,
      "unifiedDiff": "--- UserController.java\n+++ UserController.java\n..."
    }
  ],
  "conflicts": [],
  "filesAffected": 2
}
```

### Step 4: Apply (Only If No Conflicts)

```bash
refinej refactor rename \
  --old com.example.service.UserService \
  --new com.example.service.AccountService \
  --yes --json
```

**Response:**
```json
{
  "status": "ok",
  "applied": true,
  "filesModified": 2,
  "backupDir": ".refinej/backups/20260227-143000"
}
```

## Move Operations

Moving a class to a different package:

```bash
# Preview
refinej refactor move \
  --class com.example.model.User \
  --to com.example.domain \
  --preview --json

# Apply
refinej refactor move \
  --class com.example.model.User \
  --to com.example.domain \
  --yes --json
```

## Conflict Handling

If conflicts are detected, the response includes them:

```json
{
  "status": "ok",
  "changes": [...],
  "conflicts": [
    {
      "description": "'AccountService' already exists in com.example.service",
      "filePath": "/src/main/java/com/example/service/AccountService.java",
      "line": 5
    }
  ],
  "filesAffected": 2
}
```

**Agent behavior when conflicts exist:**
1. Do NOT apply the refactoring
2. Report the conflicts to the user
3. Suggest alternatives (different name, resolve the conflict first)

## Usage Kinds

References are categorized by usage kind:

| Kind | Description | Example |
|------|-------------|---------|
| `IMPORT` | Import statement | `import com.example.Foo;` |
| `NEW_INSTANCE` | Constructor call | `new Foo()` |
| `METHOD_CALL` | Method invocation | `foo.bar()` |
| `EXTENDS` | Class inheritance | `class Bar extends Foo` |
| `IMPLEMENTS` | Interface implementation | `class Bar implements Foo` |
| `ANNOTATION` | Annotation usage | `@Foo` |
| `TYPE_REFERENCE` | Type usage in declarations | `Foo foo = ...` |
| `FIELD_ACCESS` | Field read/write | `foo.field` |

## Symbol Kinds

| Kind | Description |
|------|-------------|
| `CLASS` | Class, interface, enum, or record |
| `METHOD` | Method (FQN format: `pkg.Class#method(params)`) |
| `FIELD` | Field (FQN format: `pkg.Class#fieldName`) |
| `PACKAGE` | Package declaration |

## Error Handling

| Exit Code | Meaning | Agent Action |
|-----------|---------|--------------|
| 0 | Success | Proceed |
| 1 | Error (see stderr/JSON) | Report to user |

Common error scenarios:
- **Symbol not found**: Run `refinej index` first, or check the FQN spelling
- **Not indexed**: The project hasn't been indexed yet
- **Conflict**: Name clash or read-only file detected

## Engine Selection

Choose the engine based on your use case:

```bash
# Spoon (default) — best for interactive refactoring
refinej refactor rename --old Foo --new Bar --engine spoon --preview --json

# OpenRewrite — same functionality, different parser
refinej refactor rename --old Foo --new Bar --engine rewrite --preview --json
```

The database index is engine-agnostic. You can index with one engine and refactor with another.

## Best Practices for AI Agents

1. **Always preview first** — Use `--preview --json` before applying any refactoring
2. **Check for conflicts** — Never apply if `conflicts` array is non-empty
3. **Index before querying** — Ensure the project is indexed before running queries
4. **Use `--json`** — Always use JSON output for structured parsing
5. **Report backup location** — Tell the user where backups are stored after applying
6. **Prefer `--engine spoon`** — It has the most accurate position tracking
