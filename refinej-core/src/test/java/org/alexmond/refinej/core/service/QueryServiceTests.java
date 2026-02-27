package org.alexmond.refinej.core.service;

import java.util.List;
import java.util.Optional;

import org.alexmond.refinej.core.domain.Reference;
import org.alexmond.refinej.core.domain.ReferenceEntity;
import org.alexmond.refinej.core.domain.Symbol;
import org.alexmond.refinej.core.domain.SymbolEntity;
import org.alexmond.refinej.core.domain.SymbolKind;
import org.alexmond.refinej.core.domain.UsageKind;
import org.alexmond.refinej.core.repository.ReferenceRepository;
import org.alexmond.refinej.core.repository.SymbolRepository;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link QueryService} using an in-memory H2 database.
 */
@SpringBootTest
@Transactional
class QueryServiceTests {

	@Autowired
	private QueryService queryService;

	@Autowired
	private SymbolRepository symbolRepository;

	@Autowired
	private ReferenceRepository referenceRepository;

	@Test
	void findSymbol_returnsSymbol_whenExists() {
		this.symbolRepository.save(new SymbolEntity(null, SymbolKind.CLASS, "Greeter", "com.example.Greeter",
				"/src/Greeter.java", 1, 10, null));

		Optional<Symbol> result = this.queryService.findSymbol("com.example.Greeter");

		assertThat(result).isPresent();
		assertThat(result.get().qualifiedName()).isEqualTo("com.example.Greeter");
		assertThat(result.get().kind()).isEqualTo(SymbolKind.CLASS);
	}

	@Test
	void findSymbol_returnsEmpty_whenNotFound() {
		assertThat(this.queryService.findSymbol("com.example.Unknown")).isEmpty();
	}

	@Test
	void findReferences_returnsMappedReferences() {
		SymbolEntity symbol = this.symbolRepository.save(new SymbolEntity(null, SymbolKind.CLASS, "Greeter",
				"com.example.Greeter", "/src/Greeter.java", 1, 10, null));
		this.referenceRepository
			.save(new ReferenceEntity(null, symbol, "/src/App.java", 5, 10, UsageKind.NEW_INSTANCE));
		this.referenceRepository.save(new ReferenceEntity(null, symbol, "/src/Other.java", 3, 1, UsageKind.IMPORT));

		List<Reference> refs = this.queryService.findReferences("com.example.Greeter");

		assertThat(refs).hasSize(2);
		assertThat(refs).extracting(Reference::usageKind)
			.containsExactlyInAnyOrder(UsageKind.NEW_INSTANCE, UsageKind.IMPORT);
	}

	@Test
	void findReferences_returnsEmptyList_whenSymbolNotFound() {
		assertThat(this.queryService.findReferences("com.example.Unknown")).isEmpty();
	}

	@Test
	void findReferences_withKind_filtersAtDatabaseLevel() {
		SymbolEntity symbol = this.symbolRepository.save(new SymbolEntity(null, SymbolKind.CLASS, "Greeter",
				"com.example.Greeter", "/src/Greeter.java", 1, 10, null));
		this.referenceRepository
			.save(new ReferenceEntity(null, symbol, "/src/App.java", 5, 10, UsageKind.NEW_INSTANCE));
		this.referenceRepository.save(new ReferenceEntity(null, symbol, "/src/Other.java", 3, 1, UsageKind.IMPORT));

		List<Reference> refs = this.queryService.findReferences("com.example.Greeter", UsageKind.IMPORT);

		assertThat(refs).hasSize(1);
		assertThat(refs.get(0).usageKind()).isEqualTo(UsageKind.IMPORT);
		assertThat(refs.get(0).filePath()).isEqualTo("/src/Other.java");
	}

}
