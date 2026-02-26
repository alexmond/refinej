---
description: Build all RefineJ modules (skipping tests). Use when you need to compile the project or install artifacts for inter-module dependencies.
---
Build the project by running:

```bash
cd /Users/alex.mondshain/IdeaProjects/refinej && ./scripts/mvnw.sh clean install -DskipTests
```

Report the result. If the build fails, analyse the compiler errors and fix them before reporting back.
