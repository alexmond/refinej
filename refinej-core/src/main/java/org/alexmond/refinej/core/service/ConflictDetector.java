package org.alexmond.refinej.core.service;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alexmond.refinej.core.domain.Conflict;
import org.alexmond.refinej.core.domain.FileChange;
import org.alexmond.refinej.core.repository.SymbolRepository;

import org.springframework.stereotype.Component;

/**
 * Detects conflicts that would make a refactoring operation unsafe. Conflicts are
 * informational — they are added to the
 * {@link org.alexmond.refinej.core.domain.ChangeSet} but never block computation. The
 * caller (CLI) decides whether to abort or force-apply.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConflictDetector {

	private final SymbolRepository symbolRepository;

	/**
	 * Run all conflict checks against the proposed changes.
	 * @param newQualifiedName the target qualified name for the rename/move
	 * @param changes the proposed file changes
	 * @return list of detected conflicts (may be empty)
	 */
	public List<Conflict> detect(String newQualifiedName, List<FileChange> changes) {
		List<Conflict> conflicts = new ArrayList<>();
		detectNameClash(newQualifiedName, conflicts);
		detectReadOnlyFiles(changes, conflicts);
		return conflicts;
	}

	/**
	 * Check if the target qualified name already exists in the symbol index.
	 */
	private void detectNameClash(String newQualifiedName, List<Conflict> conflicts) {
		this.symbolRepository.findByQualifiedName(newQualifiedName).ifPresent((existing) -> {
			String simpleName = extractSimpleName(newQualifiedName);
			String pkg = extractPackage(newQualifiedName);
			conflicts.add(new Conflict("'" + simpleName + "' already exists in " + pkg,
					(existing.getFilePath() != null) ? java.nio.file.Path.of(existing.getFilePath()) : null,
					existing.getLineStart()));
		});
	}

	/**
	 * Check if any affected file is not writable.
	 */
	private void detectReadOnlyFiles(List<FileChange> changes, List<Conflict> conflicts) {
		for (FileChange change : changes) {
			if (Files.exists(change.filePath()) && !Files.isWritable(change.filePath())) {
				conflicts.add(new Conflict(change.filePath() + " is not writable", change.filePath(), 0));
			}
		}
	}

	private static String extractSimpleName(String qualifiedName) {
		int hashIdx = qualifiedName.lastIndexOf('#');
		if (hashIdx >= 0) {
			String after = qualifiedName.substring(hashIdx + 1);
			int parenIdx = after.indexOf('(');
			return (parenIdx >= 0) ? after.substring(0, parenIdx) : after;
		}
		int dotIdx = qualifiedName.lastIndexOf('.');
		return (dotIdx >= 0) ? qualifiedName.substring(dotIdx + 1) : qualifiedName;
	}

	private static String extractPackage(String qualifiedName) {
		int hashIdx = qualifiedName.lastIndexOf('#');
		if (hashIdx >= 0) {
			return qualifiedName.substring(0, hashIdx);
		}
		int dotIdx = qualifiedName.lastIndexOf('.');
		return (dotIdx >= 0) ? qualifiedName.substring(0, dotIdx) : "";
	}

}
