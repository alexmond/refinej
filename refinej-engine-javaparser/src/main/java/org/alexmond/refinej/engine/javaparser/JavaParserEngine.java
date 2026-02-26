package org.alexmond.refinej.engine.javaparser;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.alexmond.refinej.core.domain.ChangeSet;
import org.alexmond.refinej.core.domain.Reference;
import org.alexmond.refinej.core.domain.Symbol;
import org.alexmond.refinej.core.engine.api.BuildType;
import org.alexmond.refinej.core.engine.api.EngineType;
import org.alexmond.refinej.core.engine.api.RefactoringEngine;

import org.springframework.stereotype.Component;

/**
 * JavaParser-based implementation of {@link RefactoringEngine}. Lightweight baseline —
 * smallest footprint, fastest startup.
 *
 * <p>
 * Phase 1: stub — safe no-ops for lifecycle methods, throws for compute/apply. Indexing
 * will use {@code CombinedTypeSolver} + {@code JavaSymbolSolver}.
 */
@Slf4j
@Component
public class JavaParserEngine implements RefactoringEngine {

	private List<Symbol> indexedSymbols = List.of();

	private List<Reference> indexedReferences = List.of();

	@Override
	public EngineType getType() {
		return EngineType.JAVAPARSER;
	}

	@Override
	public void indexProject(Path projectRoot, BuildType buildType) {
		log.info("[JavaParserEngine] indexProject called — not yet implemented");
		// TODO future: set up CombinedTypeSolver + JavaSymbolSolver, walk source tree
	}

	@Override
	public Optional<Symbol> findSymbol(String qualifiedName) {
		return this.indexedSymbols.stream().filter((s) -> s.qualifiedName().equals(qualifiedName)).findFirst();
	}

	@Override
	public List<Reference> findReferences(Symbol symbol) {
		return this.indexedReferences.stream()
			.filter((r) -> r.symbol().qualifiedName().equals(symbol.qualifiedName()))
			.toList();
	}

	@Override
	public List<Reference> findReferencesInFile(Path filePath) {
		return this.indexedReferences.stream().filter((r) -> filePath.toString().equals(r.filePath())).toList();
	}

	@Override
	public ChangeSet computeRename(Symbol oldSymbol, String newQualifiedName) {
		throw new UnsupportedOperationException("computeRename not yet implemented in JavaParserEngine");
	}

	@Override
	public ChangeSet computeMove(Symbol symbol, String newPackageName) {
		throw new UnsupportedOperationException("computeMove not yet implemented in JavaParserEngine");
	}

	@Override
	public void apply(ChangeSet changeSet, boolean dryRun) {
		throw new UnsupportedOperationException("apply not yet implemented in JavaParserEngine");
	}

	@Override
	public void clearIndex() {
		this.indexedSymbols = List.of();
		this.indexedReferences = List.of();
		log.info("[JavaParserEngine] Index cleared");
	}

	@Override
	public List<Symbol> getAllSymbols() {
		return this.indexedSymbols;
	}

	@Override
	public List<Reference> getAllReferences() {
		return this.indexedReferences;
	}

}
