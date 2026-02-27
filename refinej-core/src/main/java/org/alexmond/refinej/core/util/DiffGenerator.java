package org.alexmond.refinej.core.util;

import java.util.Arrays;
import java.util.List;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;

import org.springframework.stereotype.Component;

/**
 * Generates unified diffs between original and modified file content using
 * java-diff-utils.
 */
@Component
public class DiffGenerator {

	/**
	 * Produce a unified diff string for the given file.
	 * @param fileName display name for the diff header (e.g. "src/Greeter.java")
	 * @param original the original file content
	 * @param modified the modified file content
	 * @return a unified diff string, or empty string if contents are identical
	 */
	public String generateUnifiedDiff(String fileName, String original, String modified) {
		List<String> originalLines = splitLines(original);
		List<String> modifiedLines = splitLines(modified);

		Patch<String> patch = DiffUtils.diff(originalLines, modifiedLines);
		if (patch.getDeltas().isEmpty()) {
			return "";
		}

		List<String> diffLines = UnifiedDiffUtils.generateUnifiedDiff("a/" + fileName, "b/" + fileName, originalLines,
				patch, 3);
		return String.join("\n", diffLines) + "\n";
	}

	private static List<String> splitLines(String content) {
		if (content == null || content.isEmpty()) {
			return List.of();
		}
		return Arrays.asList(content.split("\n", -1));
	}

}
