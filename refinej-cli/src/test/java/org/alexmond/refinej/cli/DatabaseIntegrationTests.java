package org.alexmond.refinej.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.alexmond.refinej.core.domain.Reference;
import org.alexmond.refinej.core.domain.Symbol;
import org.alexmond.refinej.core.domain.SymbolKind;
import org.alexmond.refinej.core.domain.UsageKind;
import org.alexmond.refinej.core.engine.api.BuildType;
import org.alexmond.refinej.core.engine.api.RefactoringEngine;
import org.alexmond.refinej.core.repository.ReferenceRepository;
import org.alexmond.refinej.core.repository.SymbolRepository;
import org.alexmond.refinej.core.service.IndexingService;
import org.alexmond.refinej.core.service.QueryService;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack integration tests: index a fixture project with SpoonEngine, persist to H2
 * (with Flyway migrations), then query through {@link QueryService}.
 */
@SpringBootTest(classes = RefineJApplication.class)
@Transactional
class DatabaseIntegrationTests {

	private static final Path FIXTURE_ROOT = Path.of("src/test/resources/fixtures/simple");

	@Autowired
	private IndexingService indexingService;

	@Autowired
	private QueryService queryService;

	@Autowired
	private SymbolRepository symbolRepository;

	@Autowired
	private ReferenceRepository referenceRepository;

	@Autowired
	private List<RefactoringEngine> engines;

	private RefactoringEngine spoonEngine() {
		return this.engines.stream()
			.filter((e) -> e.getType().name().equalsIgnoreCase("SPOON"))
			.findFirst()
			.orElseThrow();
	}

	@Test
	void indexAndQuery_roundTrip() {
		IndexingService.IndexingResult result = this.indexingService.index(spoonEngine(), FIXTURE_ROOT,
				BuildType.UNKNOWN);

		assertThat(result.symbolCount()).isGreaterThan(0);
		assertThat(result.referenceCount()).isGreaterThan(0);
		assertThat(this.symbolRepository.count()).isEqualTo(result.symbolCount());
		assertThat(this.referenceRepository.count()).isEqualTo(result.referenceCount());
	}

	@Test
	void querySymbol_afterIndex_returnsCorrectSymbol() {
		this.indexingService.index(spoonEngine(), FIXTURE_ROOT, BuildType.UNKNOWN);

		Optional<Symbol> symbol = this.queryService.findSymbol("com.example.simple.Greeter");

		assertThat(symbol).isPresent();
		assertThat(symbol.get().kind()).isEqualTo(SymbolKind.CLASS);
		assertThat(symbol.get().simpleName()).isEqualTo("Greeter");
	}

	@Test
	void queryReferences_afterIndex_returnsUsages() {
		this.indexingService.index(spoonEngine(), FIXTURE_ROOT, BuildType.UNKNOWN);

		List<Reference> refs = this.queryService.findReferences("com.example.simple.Greeter");

		assertThat(refs).isNotEmpty();
	}

	@Test
	void queryReferences_withKindFilter_afterIndex() {
		this.indexingService.index(spoonEngine(), FIXTURE_ROOT, BuildType.UNKNOWN);

		List<Reference> allRefs = this.queryService.findReferences("com.example.simple.Greeter");
		List<Reference> newInstanceRefs = this.queryService.findReferences("com.example.simple.Greeter",
				UsageKind.NEW_INSTANCE);

		assertThat(newInstanceRefs).hasSizeLessThanOrEqualTo(allRefs.size());
		assertThat(newInstanceRefs).allMatch((r) -> r.usageKind() == UsageKind.NEW_INSTANCE);
	}

	@Test
	void reindex_clearsOldData_andPersistsNew() {
		this.indexingService.index(spoonEngine(), FIXTURE_ROOT, BuildType.UNKNOWN);
		long firstCount = this.symbolRepository.count();

		// Re-index — should produce same counts (same fixture)
		this.indexingService.index(spoonEngine(), FIXTURE_ROOT, BuildType.UNKNOWN);
		long secondCount = this.symbolRepository.count();

		assertThat(secondCount).isEqualTo(firstCount);
	}

	@Test
	void flywayMigration_validatesAgainstJpaEntities() {
		// If we reach here, the test context started successfully, which means:
		// 1. Flyway applied V1__initial_schema.sql
		// 2. Hibernate validated the schema against JPA entities (ddl-auto: validate)
		assertThat(this.symbolRepository.count()).isGreaterThanOrEqualTo(0);
		assertThat(this.referenceRepository.count()).isGreaterThanOrEqualTo(0);
	}

}
