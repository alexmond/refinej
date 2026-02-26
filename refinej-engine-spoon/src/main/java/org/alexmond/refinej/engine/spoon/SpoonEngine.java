package org.alexmond.refinej.engine.spoon;

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
 * Spoon-based implementation of {@link RefactoringEngine}.
 *
 * <p>
 * Phase 1: stub — safe no-ops for lifecycle methods, throws for compute/apply. Full
 * implementation: Phase 2 (RFJ-020–025) + Phase 4 (RFJ-040–041).
 */
@Slf4j
@Component
public class SpoonEngine implements RefactoringEngine {

	private List<Symbol> indexedSymbols = List.of();

	private List<Reference> indexedReferences = List.of();

	@Override
	public EngineType getType() {
		return EngineType.SPOON;
	}

	@Override
	public void indexProject(Path projectRoot, BuildType buildType) {
		log.info("[SpoonEngine] indexProject called — not yet implemented (RFJ-020)");
		// TODO RFJ-020: build Spoon CtModel and extract symbols + references
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
		throw new UnsupportedOperationException("computeRename not yet implemented — see RFJ-040");
	}

	@Override
	public ChangeSet computeMove(Symbol symbol, String newPackageName) {
		throw new UnsupportedOperationException("computeMove not yet implemented — see RFJ-041");
	}

	@Override
	public void apply(ChangeSet changeSet, boolean dryRun) {
		throw new UnsupportedOperationException("apply not yet implemented — see RFJ-044");
	}

	@Override
	public void clearIndex() {
		this.indexedSymbols = List.of();
		this.indexedReferences = List.of();
		log.info("[SpoonEngine] Index cleared");
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
