---
description: Show which RefactoringEngine methods are implemented vs. stubbed across all three engines (SpoonEngine, OpenRewriteEngine, JavaParserEngine). Useful before starting Phase 2+ implementation work.
---
Show the implementation status of all three RefactoringEngine implementations.

1. Read `refinej-core/src/main/java/org/alexmond/refinej/core/engine/api/RefactoringEngine.java` to get the full interface.
2. Read all three engine implementations:
   - `refinej-engine-spoon/src/main/java/org/alexmond/refinej/engine/spoon/SpoonEngine.java`
   - `refinej-engine-rewrite/src/main/java/org/alexmond/refinej/engine/rewrite/OpenRewriteEngine.java`
   - `refinej-engine-javaparser/src/main/java/org/alexmond/refinej/engine/javaparser/JavaParserEngine.java`

3. For each interface method, report its status in a table:

| Method | SpoonEngine | OpenRewriteEngine | JavaParserEngine |
|--------|-------------|-------------------|------------------|
| `indexProject` | stub / partial / done | ... | ... |
| `findSymbol` | ... | ... | ... |
| ... | ... | ... | ... |

Status legend:
- **stub** — throws `UnsupportedOperationException` or returns empty
- **partial** — some logic but incomplete
- **done** — fully implemented and tested

4. Summarise: which engine is furthest along, and what are the highest-priority gaps to fill next (cross-reference with open GitHub issues if helpful).
