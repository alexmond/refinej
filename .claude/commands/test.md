---
description: Run the full RefineJ test suite across all modules. Use after making changes to verify nothing is broken.
---
Run all tests:

```bash
cd /Users/alex.mondshain/IdeaProjects/refinej && ./mvnw verify
```

Report the test summary (tests run / failures / errors / skipped per module). If any tests fail, show the failure details and diagnose the root cause.
