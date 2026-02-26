---
description: Close a RefineJ GitHub issue as done, applying the status-done label and writing a completion comment. Pass the issue number as the argument.
argument-hint: <issue-number>
---
Close GitHub issue #$ARGUMENTS as done.

1. View the issue to confirm it is complete:
```bash
gh issue view $ARGUMENTS --repo alexmond/refinej
```

2. Add the `status-done` label:
```bash
gh issue edit $ARGUMENTS --repo alexmond/refinej --add-label "status-done"
```

3. Close with a completion comment summarising what was implemented:
```bash
gh issue close $ARGUMENTS --repo alexmond/refinej --comment "Implemented in <commit/PR>. <one-line summary of what was done>."
```

Report the issue title and the comment that was posted.
