package org.alexmond.refinej.core.domain;

import java.util.List;

/**
 * The result of a {@code computeRename} or {@code computeMove} operation.
 * Always computed first (pure, no file writes). Pass to {@code apply()} to
 * write changes to disk.
 *
 * @param changes   per-file transformations; never null, may be empty
 * @param conflicts detected problems; non-empty means the operation is risky
 * @param dryRun    true when this set was produced for preview only
 */
public record ChangeSet(
        List<FileChange> changes,
        List<Conflict> conflicts,
        boolean dryRun
) {
    public boolean hasConflicts() {
        return conflicts != null && !conflicts.isEmpty();
    }

    public int filesAffected() {
        return changes == null ? 0 : changes.size();
    }
}
