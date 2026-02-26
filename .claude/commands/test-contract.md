---
description: Run the RefactoringEngine contract test suite (EngineContractTest) that validates all three engine implementations against the same set of assertions. Run this after changing any engine stub or the RefactoringEngine interface.
---
Run the engine contract tests:

```bash
cd /Users/alex.mondshain/IdeaProjects/refinej && ./scripts/mvnw.sh test -pl refinej-cli -Dtest=EngineContractTest
```

The contract test runs the same assertions against SpoonEngine, OpenRewriteEngine, and JavaParserEngine.
Currently there should be **24 passing tests** (3 engines × 8 assertions).

Report:
- Total tests run / passed / failed
- Which engine failed (if any) and what assertion
- Whether the contract count is consistent with the interface (one test per public method)
