---
description: Create a new GitHub issue for the RefineJ project following the project's labeling conventions (priority, phase, area labels). Describe the ticket you want to create.
argument-hint: <ticket title and description>
---
Create a new GitHub issue for the RefineJ project: **$ARGUMENTS**

## Issue creation rules

### Required labels (pick one from each group)
| Group | Labels |
|-------|--------|
| Priority | `priority-P0` (critical) · `priority-P1` (high) · `priority-P2` (medium) · `priority-P3` (low) |
| Phase | `phase-1` through `phase-6` (which phase this belongs to) |
| Area | `area-core` · `area-cli` · `area-engine-spoon` · `area-engine-rewrite` · `area-engine-javaparser` · `area-test` · `area-docs` · `area-infra` |

### Milestone
Assign to the matching Phase milestone (e.g. `Phase 1` for `phase-1` issues).

### Issue body template
```markdown
## Goal
One sentence describing what this achieves.

## Design notes
- Key design decisions or constraints
- Reference to DETAILED-DESIGN.adoc section if applicable

## Implementation target
- File(s) to change
- Method(s) to add/modify

## Acceptance criteria
- [ ] Specific, testable criterion 1
- [ ] Specific, testable criterion 2

## Dependencies
Blocked by: #<issue-number> (if any)
```

### Ticket ID
Check existing issues to determine the next RFJ-NNN number:
```bash
gh issue list --repo alexmond/refinej --limit 100 --state all | sort -t- -k2 -n | tail -5
```

Create the issue with:
```bash
gh issue create --repo alexmond/refinej --title "RFJ-NNN: <title>" --body "..." --label "<labels>" --milestone "<Phase N>"
```
