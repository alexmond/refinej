---
description: Run tests for a specific RefineJ module. Pass the module artifact ID as the argument (e.g. /test-module refinej-cli).
argument-hint: <module-name> [test-class]
---
Run tests for module `$ARGUMENTS`:

```bash
cd /Users/alex.mondshain/IdeaProjects/refinej && ./scripts/mvnw.sh test -pl $ARGUMENTS
```

If a specific test class is provided as a second argument, add `-Dtest=<ClassName>`.

Valid modules: `refinej-core`, `refinej-cli`, `refinej-engine-spoon`, `refinej-engine-rewrite`, `refinej-engine-javaparser`.

Report the full test results. If tests fail, show the failure messages and stack traces.
