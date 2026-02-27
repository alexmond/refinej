package org.alexmond.refinej.core.service;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alexmond.refinej.core.domain.Reference;
import org.alexmond.refinej.core.domain.ReferenceEntity;
import org.alexmond.refinej.core.domain.Symbol;
import org.alexmond.refinej.core.domain.SymbolEntity;
import org.alexmond.refinej.core.domain.UsageKind;
import org.alexmond.refinej.core.repository.ReferenceRepository;
import org.alexmond.refinej.core.repository.SymbolRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only query service that answers symbol and reference lookups from the persisted
 * database index.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueryService {

	private final SymbolRepository symbolRepository;

	private final ReferenceRepository referenceRepository;

	/** Look up a symbol by its fully-qualified name. Returns empty if not indexed. */
	public Optional<Symbol> findSymbol(String qualifiedName) {
		return this.symbolRepository.findByQualifiedName(qualifiedName).map(this::entityToSymbol);
	}

	/** Return all known usages of the symbol with the given fully-qualified name. */
	public List<Reference> findReferences(String qualifiedName) {
		return this.symbolRepository.findByQualifiedName(qualifiedName).map((symbolEntity) -> {
			Symbol symbol = entityToSymbol(symbolEntity);
			return this.referenceRepository.findBySymbol(symbolEntity)
				.stream()
				.map((refEntity) -> entityToReference(refEntity, symbol))
				.toList();
		}).orElse(List.of());
	}

	/**
	 * Return usages of the symbol with the given fully-qualified name, filtered by usage
	 * kind.
	 */
	public List<Reference> findReferences(String qualifiedName, UsageKind kind) {
		return this.symbolRepository.findByQualifiedName(qualifiedName).map((symbolEntity) -> {
			Symbol symbol = entityToSymbol(symbolEntity);
			return this.referenceRepository.findBySymbolAndUsageKind(symbolEntity, kind)
				.stream()
				.map((refEntity) -> entityToReference(refEntity, symbol))
				.toList();
		}).orElse(List.of());
	}

	private Symbol entityToSymbol(SymbolEntity entity) {
		return new Symbol(entity.getId(), entity.getKind(), entity.getSimpleName(), entity.getQualifiedName(),
				entity.getFilePath(), entity.getLineStart(), entity.getLineEnd(), entity.getTypeFqn());
	}

	private Reference entityToReference(ReferenceEntity entity, Symbol symbol) {
		return new Reference(entity.getId(), symbol, entity.getFilePath(), entity.getLine(), entity.getCol(),
				entity.getUsageKind());
	}

}
