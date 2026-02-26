---
description: Show the current RefineJ roadmap status — which phase we are in, what is done, what is next, and what GitHub issues are open/closed. Use when planning the next piece of work or reviewing overall progress.
---
Show the current RefineJ development status.

1. Read `docs/ROADMAP.adoc` for the full phased plan.
2. Run the following to get open GitHub issues by phase label:

```bash
gh issue list --repo alexmond/refinej --label phase-1 --state open --limit 50
gh issue list --repo alexmond/refinej --label phase-2 --state open --limit 50
```

3. Run this to see recently closed issues:

```bash
gh issue list --repo alexmond/refinej --state closed --limit 20
```

Then summarise:
- **Current phase** (which phase is actively in progress)
- **Completed this phase** (closed issues)
- **Remaining this phase** (open issues with current phase label)
- **Next phase** (what comes after)
- **Recommended next task** (highest-priority open issue that is unblocked)
