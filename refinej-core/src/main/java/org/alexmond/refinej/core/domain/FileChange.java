package org.alexmond.refinej.core.domain;

import java.nio.file.Path;

/**
 * Describes the transformation of a single file as part of a {@link ChangeSet}.
 * {@code newFilePath} differs from {@code filePath} only when a file is moved.
 */
public record FileChange(Path filePath, Path newFilePath, // same as filePath unless the
															// file is being moved/renamed
		String originalContent, String newContent, String unifiedDiff) {

	/** Convenience constructor for in-place edits (file is not moved). */
	public FileChange(Path filePath, String originalContent, String newContent, String unifiedDiff) {
		this(filePath, filePath, originalContent, newContent, unifiedDiff);
	}

	public boolean isMove() {
		return !this.filePath.equals(this.newFilePath);
	}

}
