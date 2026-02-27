package org.alexmond.refinej.core.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.alexmond.refinej.core.domain.Conflict;
import org.alexmond.refinej.core.domain.FileChange;
import org.alexmond.refinej.core.domain.SymbolEntity;
import org.alexmond.refinej.core.domain.SymbolKind;
import org.alexmond.refinej.core.repository.SymbolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ConflictDetector}.
 */
@SpringBootTest
@Transactional
class ConflictDetectorTests {

	@Autowired
	private ConflictDetector conflictDetector;

	@Autowired
	private SymbolRepository symbolRepository;

	@Test
	void detectNameClash_whenTargetExists() {
		this.symbolRepository
			.save(new SymbolEntity(null, SymbolKind.CLASS, "Bar", "com.example.Bar", "/src/Bar.java", 1, 10, null));

		List<Conflict> conflicts = this.conflictDetector.detect("com.example.Bar", List.of());

		assertThat(conflicts).hasSize(1);
		assertThat(conflicts.get(0).description()).contains("'Bar' already exists in com.example");
	}

	@Test
	void detectNameClash_whenTargetDoesNotExist() {
		List<Conflict> conflicts = this.conflictDetector.detect("com.example.NewName", List.of());

		assertThat(conflicts).isEmpty();
	}

	@Test
	void detectReadOnlyFile(@TempDir Path tempDir) throws IOException {
		// Create a read-only file
		Path readOnlyFile = tempDir.resolve("ReadOnly.java");
		Files.writeString(readOnlyFile, "public class ReadOnly {}");
		readOnlyFile.toFile().setReadOnly();

		try {
			FileChange change = new FileChange(readOnlyFile, "public class ReadOnly {}", "public class NewName {}",
					"diff");

			List<Conflict> conflicts = this.conflictDetector.detect("com.example.NewName", List.of(change));

			assertThat(conflicts).hasSize(1);
			assertThat(conflicts.get(0).description()).contains("is not writable");
		}
		finally {
			readOnlyFile.toFile().setWritable(true);
		}
	}

	@Test
	void detectNoConflicts_whenAllClear(@TempDir Path tempDir) throws IOException {
		Path writableFile = tempDir.resolve("Writable.java");
		Files.writeString(writableFile, "public class Writable {}");

		FileChange change = new FileChange(writableFile, "public class Writable {}", "public class NewName {}", "diff");

		List<Conflict> conflicts = this.conflictDetector.detect("com.example.NewName", List.of(change));

		assertThat(conflicts).isEmpty();
	}

}
