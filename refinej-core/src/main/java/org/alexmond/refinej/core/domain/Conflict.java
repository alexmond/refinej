package org.alexmond.refinej.core.domain;

import java.nio.file.Path;

/**
 * Describes a problem detected during {@code computeRename} / {@code computeMove} that
 * would make the refactoring unsafe. Conflicts are reported in the {@link ChangeSet} so
 * the caller can decide whether to abort or force-apply.
 */
public record Conflict(String description, Path filePath, int line) {
}
