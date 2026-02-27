package org.alexmond.refinej.core.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alexmond.refinej.core.domain.FileChange;
import org.alexmond.refinej.core.engine.api.ChangeApplier;

import org.springframework.stereotype.Component;

/**
 * Writes {@link FileChange}s to disk atomically, backing up originals first. This is the
 * only component that modifies source files.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChangeApplierImpl implements ChangeApplier {

	private final FileBackupManager backupManager;

	@Override
	public void backupAndApply(List<FileChange> changes, boolean dryRun) throws IOException {
		if (dryRun) {
			log.info("Dry run — skipping {} file write(s)", changes.size());
			return;
		}

		// Phase 1: backup all affected files
		for (FileChange change : changes) {
			this.backupManager.backup(change.filePath());
		}

		try {
			// Phase 2: write all changes
			for (FileChange change : changes) {
				writeChange(change);
			}
			this.backupManager.clear();
			log.info("Applied {} file change(s)", changes.size());
		}
		catch (IOException ex) {
			log.error("Apply failed — restoring backups", ex);
			this.backupManager.restoreAll();
			throw ex;
		}
	}

	private void writeChange(FileChange change) throws IOException {
		Path target = change.newFilePath();
		Files.createDirectories(target.getParent());

		// Write to a temp file first, then atomically move into place
		Path temp = target.resolveSibling(target.getFileName() + ".refinej-tmp");
		Files.writeString(temp, change.newContent(), StandardCharsets.UTF_8);
		Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

		// If this is a file move, delete the original
		if (change.isMove() && Files.exists(change.filePath())) {
			Files.delete(change.filePath());
			log.debug("Moved {} → {}", change.filePath(), target);
		}
		else {
			log.debug("Updated {}", target);
		}
	}

}
