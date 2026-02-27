package org.alexmond.refinej.core.service;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alexmond.refinej.core.domain.Reference;
import org.alexmond.refinej.core.domain.ReferenceEntity;
import org.alexmond.refinej.core.domain.Symbol;
import org.alexmond.refinej.core.domain.SymbolEntity;
import org.alexmond.refinej.core.engine.api.BuildType;
import org.alexmond.refinej.core.engine.api.RefactoringEngine;
import org.alexmond.refinej.core.repository.ReferenceRepository;
import org.alexmond.refinej.core.repository.SymbolRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Drives a full index cycle: runs the engine, then persists all discovered
 * {@link Symbol}s and {@link Reference}s to the database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class IndexingService {

	private final SymbolRepository symbolRepository;

	private final ReferenceRepository referenceRepository;

	/**
	 * Index the project at {@code projectRoot} using the given engine, then persist all
	 * symbols and references to the database (full re-index — previous data is cleared
	 * first).
	 */
	public IndexingResult index(RefactoringEngine engine, Path projectRoot, BuildType buildType) {
		long start = System.currentTimeMillis();
		engine.indexProject(projectRoot, buildType);

		this.referenceRepository.deleteAllInBatch();
		this.symbolRepository.deleteAllInBatch();

		List<SymbolEntity> saved = this.symbolRepository
			.saveAll(engine.getAllSymbols().stream().map(this::toSymbolEntity).toList());

		Map<String, SymbolEntity> byQualifiedName = saved.stream()
			.collect(Collectors.toMap(SymbolEntity::getQualifiedName, Function.identity()));

		List<ReferenceEntity> refEntities = engine.getAllReferences()
			.stream()
			.map((ref) -> toReferenceEntity(ref, byQualifiedName))
			.filter(Objects::nonNull)
			.toList();
		this.referenceRepository.saveAll(refEntities);

		long duration = System.currentTimeMillis() - start;
		log.info("Indexed {} — {} symbols, {} references in {}ms", projectRoot, saved.size(), refEntities.size(),
				duration);
		return new IndexingResult(saved.size(), refEntities.size(), duration);
	}

	private SymbolEntity toSymbolEntity(Symbol symbol) {
		return new SymbolEntity(null, symbol.kind(), symbol.simpleName(), symbol.qualifiedName(), symbol.filePath(),
				symbol.lineStart(), symbol.lineEnd(), symbol.typeFqn());
	}

	private ReferenceEntity toReferenceEntity(Reference ref, Map<String, SymbolEntity> symbolMap) {
		SymbolEntity symbolEntity = symbolMap.get(ref.symbol().qualifiedName());
		if (symbolEntity == null) {
			log.warn("Skipping reference — symbol not found in index: {}", ref.symbol().qualifiedName());
			return null;
		}
		return new ReferenceEntity(null, symbolEntity, ref.filePath(), ref.line(), ref.column(), ref.usageKind());
	}

	/**
	 * Summary of a completed indexing run.
	 */
	public record IndexingResult(long symbolCount, long referenceCount, long durationMs) {
	}

}
