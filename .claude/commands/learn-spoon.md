---
description: Learn how the Spoon AST library works in the context of RefineJ. Covers CtModel, CtType, CtMethod, the Launcher, and how to traverse/query/transform Java source code with Spoon. Essential reading before implementing SpoonEngine.
---
Teach me how Spoon works for implementing `SpoonEngine` in RefineJ.

First read:
- `refinej-engine-spoon/src/main/java/org/alexmond/refinej/engine/spoon/SpoonEngine.java` (current stub)
- `refinej-core/src/main/java/org/alexmond/refinej/core/engine/api/RefactoringEngine.java` (interface to implement)
- `refinej-core/src/main/java/org/alexmond/refinej/core/domain/Symbol.java` and `Reference.java` (output types)

Then explain Spoon (version 10.4.0, `fr.inria.gforge.spoon:spoon-core`):

## 1. Core concepts
- `Launcher` — entry point; how to configure input sources and classpath
- `CtModel` — the compiled model of the project
- `CtType`, `CtClass`, `CtInterface` — type representations
- `CtMethod`, `CtField`, `CtConstructor` — member representations
- `CtElement.getPosition()` — how to get file + line number (for `Symbol.filePath/lineStart/lineEnd`)

## 2. Building the index (for `indexProject`)
Show a minimal code sketch for:
- Creating a `Launcher` pointed at a source directory
- Building the model (`launcher.buildModel()`)
- Traversing all types to create `Symbol` records

## 3. Finding references (for `findReferences`)
- How to use `CtType.getReferences()` or `Query.getElements()` to find usages
- Converting a `CtReference` to a `Reference` record

## 4. Rename operation (for `computeRename`)
- How to rename a type/method/field and propagate all references
- The `RenameRefactoring` or manual reference replacement approach in Spoon 10.x

## 5. Key gotchas
- Spoon's model is mutable but the original source is not written until you call `prettyPrint` or a `ChangeCollector`
- How to produce a unified diff rather than writing to disk (for `FileChange.unifiedDiff`)
