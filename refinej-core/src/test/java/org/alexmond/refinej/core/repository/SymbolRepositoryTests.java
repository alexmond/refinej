package org.alexmond.refinej.core.repository;

import java.util.List;
import java.util.Optional;

import org.alexmond.refinej.core.domain.SymbolEntity;
import org.alexmond.refinej.core.domain.SymbolKind;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link SymbolRepository} using an in-memory H2 database.
 */
@SpringBootTest
@Transactional
class SymbolRepositoryTests {

	@Autowired
	private SymbolRepository symbolRepository;

	@Test
	void save_andFindByQualifiedName() {
		this.symbolRepository.save(new SymbolEntity(null, SymbolKind.CLASS, "Greeter", "com.example.Greeter",
				"/src/Greeter.java", 1, 10, null));

		Optional<SymbolEntity> found = this.symbolRepository.findByQualifiedName("com.example.Greeter");

		assertThat(found).isPresent();
		assertThat(found.get().getSimpleName()).isEqualTo("Greeter");
		assertThat(found.get().getKind()).isEqualTo(SymbolKind.CLASS);
	}

	@Test
	void findByQualifiedName_returnsEmpty_whenNotFound() {
		assertThat(this.symbolRepository.findByQualifiedName("com.example.Unknown")).isEmpty();
	}

	@Test
	void findByKind_returnsMatchingSymbols() {
		this.symbolRepository.saveAll(List.of(
				new SymbolEntity(null, SymbolKind.CLASS, "Greeter", "com.example.Greeter", "/src/Greeter.java", 1, 10,
						null),
				new SymbolEntity(null, SymbolKind.METHOD, "greet", "com.example.Greeter.greet", "/src/Greeter.java", 3,
						5, "String"),
				new SymbolEntity(null, SymbolKind.CLASS, "App", "com.example.App", "/src/App.java", 1, 8, null)));

		List<SymbolEntity> classes = this.symbolRepository.findByKind(SymbolKind.CLASS);

		assertThat(classes).hasSize(2);
		assertThat(classes).extracting(SymbolEntity::getSimpleName).containsExactlyInAnyOrder("Greeter", "App");
	}

	@Test
	void findByFilePathStartingWith_returnsMatchingSymbols() {
		this.symbolRepository.saveAll(List.of(
				new SymbolEntity(null, SymbolKind.CLASS, "Greeter", "com.example.Greeter", "/src/main/Greeter.java", 1,
						10, null),
				new SymbolEntity(null, SymbolKind.CLASS, "App", "com.example.App", "/src/test/App.java", 1, 8, null)));

		List<SymbolEntity> mainSymbols = this.symbolRepository.findByFilePathStartingWith("/src/main/");

		assertThat(mainSymbols).hasSize(1);
		assertThat(mainSymbols.get(0).getSimpleName()).isEqualTo("Greeter");
	}

	@Test
	void uniqueConstraint_onQualifiedName() {
		this.symbolRepository.saveAndFlush(new SymbolEntity(null, SymbolKind.CLASS, "Greeter", "com.example.Greeter",
				"/src/Greeter.java", 1, 10, null));

		assertThatThrownBy(() -> this.symbolRepository.saveAndFlush(new SymbolEntity(null, SymbolKind.CLASS, "Greeter2",
				"com.example.Greeter", "/src/Greeter2.java", 1, 10, null)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void deleteByFilePath_removesMatchingEntities() {
		this.symbolRepository.saveAll(List.of(
				new SymbolEntity(null, SymbolKind.CLASS, "Greeter", "com.example.Greeter", "/src/Greeter.java", 1, 10,
						null),
				new SymbolEntity(null, SymbolKind.METHOD, "greet", "com.example.Greeter.greet", "/src/Greeter.java", 3,
						5, "String")));

		this.symbolRepository.deleteByFilePath("/src/Greeter.java");

		assertThat(this.symbolRepository.findAll()).isEmpty();
	}

}
