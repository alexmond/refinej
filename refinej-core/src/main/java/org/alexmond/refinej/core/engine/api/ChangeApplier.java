package org.alexmond.refinej.core.engine.api;

import org.alexmond.refinej.core.domain.FileChange;

import java.io.IOException;
import java.util.List;

/**
 * Writes a list of {@link FileChange}s to disk atomically, backing up originals first.
 * This is the only component in the system that modifies source files.
 */
public interface ChangeApplier {
    /**
     * Backup all affected files, then write new content using atomic file replacement.
     * A no-op when {@code dryRun} is true.
     *
     * @throws IOException if any file cannot be written
     */
    void backupAndApply(List<FileChange> changes, boolean dryRun) throws IOException;
}
