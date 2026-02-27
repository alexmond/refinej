package org.alexmond.refinej.core.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import org.alexmond.refinej.core.domain.ReferenceEntity;
import org.alexmond.refinej.core.domain.SymbolEntity;
import org.alexmond.refinej.core.domain.SymbolKind;
import org.alexmond.refinej.core.domain.UsageKind;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ReferenceRepository} using an in-memory H2 database.
 */
@SpringBootTest
@Transactional
class ReferenceRepositoryTests {

	@Autowired
	private SymbolRepository symbolRepository;

	@Autowired
	private ReferenceRepository referenceRepository;

	@Autowired
	private EntityManager em;

	@Test
	void findBySymbol_returnsMatchingReferences() {
		SymbolEntity symbol = this.symbolRepository.save(new SymbolEntity(null, SymbolKind.CLASS, "Greeter",
				"com.example.Greeter", "/src/Greeter.java", 1, 10, null));
		this.referenceRepository
			.save(new ReferenceEntity(null, symbol, "/src/App.java", 5, 10, UsageKind.NEW_INSTANCE));

		List<ReferenceEntity> refs = this.referenceRepository.findBySymbol(symbol);

		assertThat(refs).hasSize(1);
		assertThat(refs.get(0).getUsageKind()).isEqualTo(UsageKind.NEW_INSTANCE);
		assertThat(refs.get(0).getFilePath()).isEqualTo("/src/App.java");
	}

	@Test
	void deleteByFilePath_removesMatchingReferences() {
		SymbolEntity symbol = this.symbolRepository.save(new SymbolEntity(null, SymbolKind.CLASS, "Greeter",
				"com.example.Greeter", "/src/Greeter.java", 1, 10, null));
		this.referenceRepository
			.save(new ReferenceEntity(null, symbol, "/src/App.java", 5, 10, UsageKind.NEW_INSTANCE));
		this.referenceRepository.save(new ReferenceEntity(null, symbol, "/src/Other.java", 7, 3, UsageKind.IMPORT));

		this.referenceRepository.deleteByFilePath("/src/App.java");

		List<ReferenceEntity> remaining = this.referenceRepository.findAll();
		assertThat(remaining).hasSize(1);
		assertThat(remaining.get(0).getFilePath()).isEqualTo("/src/Other.java");
	}

	@Test
	void findBySymbolAndUsageKind_filtersCorrectly() {
		SymbolEntity symbol = this.symbolRepository.save(new SymbolEntity(null, SymbolKind.CLASS, "Greeter",
				"com.example.Greeter", "/src/Greeter.java", 1, 10, null));
		this.referenceRepository
			.save(new ReferenceEntity(null, symbol, "/src/App.java", 5, 10, UsageKind.NEW_INSTANCE));
		this.referenceRepository.save(new ReferenceEntity(null, symbol, "/src/Other.java", 3, 1, UsageKind.IMPORT));

		List<ReferenceEntity> importRefs = this.referenceRepository.findBySymbolAndUsageKind(symbol, UsageKind.IMPORT);

		assertThat(importRefs).hasSize(1);
		assertThat(importRefs.get(0).getFilePath()).isEqualTo("/src/Other.java");
	}

	@Test
	void cascadeDelete_removesReferences_whenSymbolDeleted() {
		SymbolEntity symbol = this.symbolRepository.save(new SymbolEntity(null, SymbolKind.CLASS, "Greeter",
				"com.example.Greeter", "/src/Greeter.java", 1, 10, null));
		this.referenceRepository
			.save(new ReferenceEntity(null, symbol, "/src/App.java", 5, 10, UsageKind.NEW_INSTANCE));

		this.symbolRepository.delete(symbol);
		this.symbolRepository.flush(); // flush DELETE to DB so ON DELETE CASCADE fires
		this.em.clear(); // evict stale session cache so findAll() reads from DB

		assertThat(this.referenceRepository.findAll()).isEmpty();
	}

}
