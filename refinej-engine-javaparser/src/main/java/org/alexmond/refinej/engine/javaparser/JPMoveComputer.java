package org.alexmond.refinej.engine.javaparser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.alexmond.refinej.core.domain.ChangeSet;
import org.alexmond.refinej.core.domain.Conflict;
import org.alexmond.refinej.core.domain.FileChange;
import org.alexmond.refinej.core.domain.Reference;
import org.alexmond.refinej.core.domain.Symbol;
import org.alexmond.refinej.core.domain.SymbolKind;
import org.alexmond.refinej.core.domain.UsageKind;
import org.alexmond.refinej.core.util.DiffGenerator;

/**
 * Computes all source changes needed to move a class to a new package, producing a
 * {@link ChangeSet} without modifying any files. Updates the package declaration in the
 * moved file and all import statements that reference it.
 */
@Slf4j
class JPMoveComputer {

	private final Map<String, Symbol> symbolIndex;

	private final Map<Long, List<Reference>> referencesBySymbolId;

	private final DiffGenerator diffGenerator;

	JPMoveComputer(Map<String, Symbol> symbolIndex, Map<Long, List<Reference>> referencesBySymbolId,
			DiffGenerator diffGenerator) {
		this.symbolIndex = symbolIndex;
		this.referencesBySymbolId = referencesBySymbolId;
		this.diffGenerator = diffGenerator;
	}

	ChangeSet compute(Symbol symbol, String newPackageName) {
		List<Conflict> conflicts = new ArrayList<>();
		List<FileChange> changes = new ArrayList<>();

		if (symbol.kind() != SymbolKind.CLASS) {
			conflicts.add(new Conflict("Only CLASS symbols can be moved; got " + symbol.kind(), null, 0));
			return new ChangeSet(changes, conflicts, true);
		}

		String oldQualifiedName = symbol.qualifiedName();
		String simpleName = symbol.simpleName();
		String newQualifiedName = newPackageName + "." + simpleName;

		if (this.symbolIndex.containsKey(newQualifiedName)) {
			conflicts.add(new Conflict("Symbol already exists at target: " + newQualifiedName, null, 0));
		}

		int lastDot = oldQualifiedName.lastIndexOf('.');
		String oldPackageName = (lastDot >= 0) ? oldQualifiedName.substring(0, lastDot) : "";

		if (oldPackageName.equals(newPackageName)) {
			return new ChangeSet(changes, conflicts, true);
		}

		if (symbol.filePath() != null) {
			try {
				Path sourcePath = Path.of(symbol.filePath());
				String originalContent = Files.readString(sourcePath, StandardCharsets.UTF_8);

				String newContent = originalContent.replaceFirst(
						"package\\s+" + escapeForRegex(oldPackageName) + "\\s*;", "package " + newPackageName + ";");

				Path newFilePath = computeNewFilePath(sourcePath, oldPackageName, newPackageName);

				String diff = this.diffGenerator.generateUnifiedDiff(sourcePath.getFileName().toString(),
						originalContent, newContent);
				changes.add(new FileChange(sourcePath, newFilePath, originalContent, newContent, diff));
			}
			catch (IOException ex) {
				log.warn("Could not read class file {}: {}", symbol.filePath(), ex.getMessage());
				conflicts.add(new Conflict("Cannot read file: " + symbol.filePath(), Path.of(symbol.filePath()), 0));
			}
		}

		List<Reference> refs = this.referencesBySymbolId.getOrDefault(symbol.id(), List.of());

		Map<String, List<Reference>> importRefsByFile = refs.stream()
			.filter((r) -> r.usageKind() == UsageKind.IMPORT && r.filePath() != null)
			.filter((r) -> !r.filePath().equals(symbol.filePath()))
			.collect(Collectors.groupingBy(Reference::filePath));

		for (Map.Entry<String, List<Reference>> entry : importRefsByFile.entrySet()) {
			String filePath = entry.getKey();
			try {
				Path path = Path.of(filePath);
				String originalContent = Files.readString(path, StandardCharsets.UTF_8);

				String newContent = originalContent.replace("import " + oldQualifiedName + ";",
						"import " + newQualifiedName + ";");

				if (!newContent.equals(originalContent)) {
					String diff = this.diffGenerator.generateUnifiedDiff(path.getFileName().toString(), originalContent,
							newContent);
					changes.add(new FileChange(path, originalContent, newContent, diff));
				}
			}
			catch (IOException ex) {
				log.warn("Could not read file {}: {}", filePath, ex.getMessage());
				conflicts.add(new Conflict("Cannot read file: " + filePath, Path.of(filePath), 0));
			}
		}

		return new ChangeSet(changes, conflicts, true);
	}

	private static Path computeNewFilePath(Path sourcePath, String oldPackage, String newPackage) {
		String oldPkgPath = oldPackage.replace('.', '/');
		String newPkgPath = newPackage.replace('.', '/');
		String absPath = sourcePath.toAbsolutePath().normalize().toString();

		int pkgIdx = absPath.lastIndexOf(oldPkgPath);
		if (pkgIdx >= 0) {
			String newAbsPath = absPath.substring(0, pkgIdx) + newPkgPath
					+ absPath.substring(pkgIdx + oldPkgPath.length());
			return Path.of(newAbsPath);
		}

		return sourcePath.getParent().resolve(newPkgPath.replace('/', '/')).resolve(sourcePath.getFileName());
	}

	private static String escapeForRegex(String text) {
		return text.replace(".", "\\.");
	}

}
