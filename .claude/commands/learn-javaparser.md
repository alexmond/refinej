---
description: Learn how JavaParser works in the context of RefineJ. Covers CompilationUnit, the symbol solver, and how to traverse/transform Java source code. Essential reading before implementing JavaParserEngine.
---
Teach me how JavaParser works for implementing `JavaParserEngine` in RefineJ.

First read:
- `refinej-engine-javaparser/src/main/java/org/alexmond/refinej/engine/javaparser/JavaParserEngine.java` (current stub)
- `refinej-core/src/main/java/org/alexmond/refinej/core/engine/api/RefactoringEngine.java` (interface to implement)
- `refinej-core/src/main/java/org/alexmond/refinej/core/domain/Symbol.java` and `Reference.java` (output types)

Then explain JavaParser (version 3.25.8, `com.github.javaparser:javaparser-symbol-solver-core`):

## 1. Core concepts
- `StaticJavaParser` / `JavaParser` — parsing source files into `CompilationUnit`
- `CompilationUnit` — the AST root for a single file
- `ClassOrInterfaceDeclaration`, `MethodDeclaration`, `FieldDeclaration` — node types
- `Position` / `Range` — how to get line/column information
- `SymbolSolver` — resolves type references to their declarations (needed for cross-file analysis)

## 2. Configuring the symbol solver (for cross-file analysis)
- `JavaSymbolSolver` with `CombinedTypeSolver`
- `JavaParserTypeSolver` (points at source directory)
- `ReflectionTypeSolver` (JDK types)
- How to attach the solver so `NameExpr.resolve()` works

## 3. Building the index (for `indexProject`)
- Parsing all `.java` files in a directory tree
- Using `VoidVisitorAdapter` to walk `ClassOrInterfaceDeclaration`, `MethodDeclaration`, `FieldDeclaration`
- Creating `Symbol` records from AST nodes

## 4. Finding references (for `findReferences`)
- Using the symbol solver to resolve `NameExpr`, `MethodCallExpr`, `ObjectCreationExpr`
- Checking if the resolved declaration matches the target symbol
- Creating `Reference` records with file path + line/column

## 5. Rename transformation (for `computeRename`)
- Walking the AST and renaming all matching `SimpleName` nodes
- Using `LexicalPreservingPrinter` to produce style-preserving output
- Generating a unified diff from original vs. transformed source
