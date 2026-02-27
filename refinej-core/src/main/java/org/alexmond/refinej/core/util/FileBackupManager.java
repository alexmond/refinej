package org.alexmond.refinej.core.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

/**
 * Backs up source files before modification so they can be restored if an apply operation
 * fails partway through. Backups are stored under {@code .refinej/backups/<timestamp>/}.
 */
@Slf4j
@Component
public class FileBackupManager {

	private static final String BACKUP_DIR = ".refinej/backups";

	private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

	private final List<BackupEntry> entries = new ArrayList<>();

	private Path backupRoot;

	/**
	 * Back up a single file. The backup is stored under a timestamped directory.
	 * @param filePath the file to back up
	 * @throws IOException if the file cannot be copied
	 */
	public void backup(Path filePath) throws IOException {
		if (!Files.exists(filePath)) {
			return;
		}
		if (this.backupRoot == null) {
			this.backupRoot = filePath.getParent().resolve(BACKUP_DIR).resolve(LocalDateTime.now().format(TIMESTAMP));
		}
		Path relative = filePath.toAbsolutePath().normalize();
		Path backupPath = this.backupRoot.resolve(relative.getFileName());
		Files.createDirectories(backupPath.getParent());
		Files.copy(filePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
		this.entries.add(new BackupEntry(filePath, backupPath));
		log.debug("Backed up {} → {}", filePath, backupPath);
	}

	/**
	 * Restore all backed-up files to their original locations.
	 * @throws IOException if any file cannot be restored
	 */
	public void restoreAll() throws IOException {
		for (BackupEntry entry : this.entries) {
			Files.copy(entry.backupPath(), entry.originalPath(), StandardCopyOption.REPLACE_EXISTING);
			log.debug("Restored {} from {}", entry.originalPath(), entry.backupPath());
		}
		this.entries.clear();
	}

	/**
	 * Clear the backup list without restoring (call after a successful apply).
	 */
	public void clear() {
		this.entries.clear();
		this.backupRoot = null;
	}

	private record BackupEntry(Path originalPath, Path backupPath) {
	}

}
