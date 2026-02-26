---
description: Give a full architectural overview of the RefineJ project — modules, dependency rules, key interfaces, data flow, and how the three refactoring engines relate to each other.
---
Give a complete architectural overview of the RefineJ project.

Read the following files first:
- `CLAUDE.md` (build commands, module rules)
- `docs/ARCHITECTURE.adoc` (system architecture)
- `docs/DETAILED-DESIGN.adoc` (interfaces, sequences, package tree)

Then explain:

## 1. Module structure
- What each Maven module is responsible for
- The strict dependency rule (engines → core only; cli → core + all engines)
- Why this structure was chosen

## 2. Core abstractions
- `RefactoringEngine` interface — all methods and their contracts
- `Symbol` / `Reference` / `ChangeSet` domain records
- `EngineConfig` factory — how the active engine is selected at startup

## 3. CLI layer
- Picocli command hierarchy (`refinej <subcommand>`)
- `SpringPicocliFactory` — how Spring beans get injected into Picocli commands
- `EngineResolver` — how `--engine` flag overrides the default

## 4. Three-engine design
- Why there are three separate engines (Spoon, OpenRewrite, JavaParser)
- What each engine excels at
- How they are kept in sync (contract test, interface-first rule)

## 5. Data flow
- From `refinej index` → engine stub → in-memory lists
- From `refinej refactor rename` → `computeRename` → `ChangeSet` → `apply` (Phase 4+)

## 6. Current status
Which parts are fully implemented vs. stubs, referencing the ROADMAP phases.
