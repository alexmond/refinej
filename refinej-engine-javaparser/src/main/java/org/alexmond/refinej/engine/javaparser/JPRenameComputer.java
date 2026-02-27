package org.alexmond.refinej.engine.javaparser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
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
 * Computes all source changes needed for a symbol rename, producing a {@link ChangeSet}
 * without modifying any files. Uses the indexed reference positions to apply text-based
 * replacements.
 */
@Slf4j
class JPRenameComputer {

	private final Map<String, Symbol> symbolIndex;

	private final Map<Long, List<Reference>> referencesBySymbolId;

	private final DiffGenerator diffGenerator;

	JPRenameComputer(Map<String, Symbol> symbolIndex, Map<Long, List<Reference>> referencesBySymbolId,
			DiffGenerator diffGenerator) {
		this.symbolIndex = symbolIndex;
		this.referencesBySymbolId = referencesBySymbolId;
		this.diffGenerator = diffGenerator;
	}

	ChangeSet compute(Symbol oldSymbol, String newQualifiedName) {
		List<Conflict> conflicts = new ArrayList<>();
		List<FileChange> changes = new ArrayList<>();

		String oldSimpleName = oldSymbol.simpleName();
		String newSimpleName = extractSimpleName(newQualifiedName, oldSymbol.kind());

		if (this.symbolIndex.containsKey(newQualifiedName)) {
			conflicts.add(new Conflict("Symbol already exists: " + newQualifiedName, null, 0));
		}

		List<Reference> refs = this.referencesBySymbolId.getOrDefault(oldSymbol.id(), List.of());

		Map<String, List<Reference>> refsByFile = refs.stream()
			.filter((r) -> r.filePath() != null)
			.collect(Collectors.groupingBy(Reference::filePath));

		if (oldSymbol.filePath() != null && !refsByFile.containsKey(oldSymbol.filePath())) {
			refsByFile.put(oldSymbol.filePath(), List.of());
		}

		Pattern oldNamePattern = Pattern.compile("\\b" + Pattern.quote(oldSimpleName) + "\\b");

		for (Map.Entry<String, List<Reference>> entry : refsByFile.entrySet()) {
			String filePath = entry.getKey();
			List<Reference> fileRefs = entry.getValue();

			try {
				String originalContent = Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
				String[] lines = originalContent.split("\n", -1);

				java.util.Set<Integer> refLines = fileRefs.stream().map(Reference::line).collect(Collectors.toSet());

				boolean isDeclarationFile = filePath.equals(oldSymbol.filePath());
				if (isDeclarationFile) {
					refLines.add(oldSymbol.lineStart());
				}

				boolean modified = false;
				for (int lineIdx : refLines) {
					if (lineIdx < 1 || lineIdx > lines.length) {
						continue;
					}
					String line = lines[lineIdx - 1];

					List<Reference> lineRefs = fileRefs.stream().filter((r) -> r.line() == lineIdx).toList();

					String newLine;
					if (isImportLine(lineRefs)) {
						newLine = line.replace(oldSymbol.qualifiedName(), newQualifiedName);
					}
					else {
						newLine = oldNamePattern.matcher(line).replaceAll(newSimpleName);
					}

					if (!newLine.equals(line)) {
						lines[lineIdx - 1] = newLine;
						modified = true;
					}
				}

				if (modified) {
					String newContent = String.join("\n", lines);
					Path path = Path.of(filePath);

					Path newFilePath = path;
					if (oldSymbol.kind() == SymbolKind.CLASS && isDeclarationFile
							&& path.getFileName().toString().equals(oldSimpleName + ".java")) {
						newFilePath = path.resolveSibling(newSimpleName + ".java");
					}

					String diff = this.diffGenerator.generateUnifiedDiff(path.getFileName().toString(), originalContent,
							newContent);
					changes.add(new FileChange(path, newFilePath, originalContent, newContent, diff));
				}
			}
			catch (IOException ex) {
				log.warn("Could not read file {}: {}", filePath, ex.getMessage());
				conflicts.add(new Conflict("Cannot read file: " + filePath, Path.of(filePath), 0));
			}
		}

		return new ChangeSet(changes, conflicts, true);
	}

	private static String extractSimpleName(String qualifiedName, SymbolKind kind) {
		if (kind == SymbolKind.METHOD || kind == SymbolKind.FIELD) {
			int hashIdx = qualifiedName.lastIndexOf('#');
			if (hashIdx >= 0) {
				String after = qualifiedName.substring(hashIdx + 1);
				int parenIdx = after.indexOf('(');
				return (parenIdx >= 0) ? after.substring(0, parenIdx) : after;
			}
		}
		int dotIdx = qualifiedName.lastIndexOf('.');
		return (dotIdx >= 0) ? qualifiedName.substring(dotIdx + 1) : qualifiedName;
	}

	private static boolean isImportLine(List<Reference> lineRefs) {
		return lineRefs.stream().anyMatch((r) -> r.usageKind() == UsageKind.IMPORT);
	}

}
