---
description: Learn how OpenRewrite works in the context of RefineJ. Covers LST, Recipes, visitors, and how to traverse/transform Java source code. Essential reading before implementing OpenRewriteEngine.
---
Teach me how OpenRewrite works for implementing `OpenRewriteEngine` in RefineJ.

First read:
- `refinej-engine-rewrite/src/main/java/org/alexmond/refinej/engine/rewrite/OpenRewriteEngine.java` (current stub)
- `refinej-core/src/main/java/org/alexmond/refinej/core/engine/api/RefactoringEngine.java` (interface to implement)
- `refinej-core/src/main/java/org/alexmond/refinej/core/domain/Symbol.java` and `Reference.java` (output types)

Then explain OpenRewrite (version 8.73.2, `org.openrewrite:rewrite-java`):

## 1. Core concepts
- `SourceFile` / `J.CompilationUnit` — the LST (Lossless Semantic Tree) for a Java file
- `Recipe` — a unit of transformation (implement `visitClassDeclaration`, etc.)
- `JavaParser` — how to parse source files into LSTs
- `ExecutionContext` — state passed through recipe execution
- `Cursor` — position in the LST tree during traversal

## 2. Building the index (for `indexProject`)
- How to parse a directory of Java files into a `List<SourceFile>`
- Traversing `J.ClassDeclaration`, `J.MethodDeclaration`, `J.VariableDeclarations` to create `Symbol` records
- Getting file path and line numbers from `J.ClassDeclaration.getPrefix().getWhitespace()` or `getCoordinates()`

## 3. Finding references (for `findReferences`)
- Using `JavaVisitor` to find all usages of a given FQN
- Converting LST nodes to `Reference` records

## 4. Rename operation (for `computeRename`)
- OpenRewrite's built-in `ChangeType`, `RenameVariable`, `ChangeMethodName` recipes
- How to collect diffs (the `Result` list from `Recipe.run()`) to produce `FileChange.unifiedDiff`

## 5. Key differences from Spoon
- OpenRewrite is diff-based and non-destructive; it produces a new LST and a diff
- Style-preserving: whitespace and comments are preserved in the LST
- How to use `Result.diff()` to get a unified diff string
