package org.alexmond.refinej.engine.rewrite;

import lombok.extern.slf4j.Slf4j;
import org.alexmond.refinej.core.domain.ChangeSet;
import org.alexmond.refinej.core.domain.Reference;
import org.alexmond.refinej.core.domain.Symbol;
import org.alexmond.refinej.core.engine.api.BuildType;
import org.alexmond.refinej.core.engine.api.EngineType;
import org.alexmond.refinej.core.engine.api.RefactoringEngine;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * OpenRewrite-based implementation of {@link RefactoringEngine}.
 *
 * <p>Phase 1: stub — safe no-ops for lifecycle methods, throws for compute/apply.
 * Full implementation: Phase 5 (RFJ-050–051).
 */
@Slf4j
@Component
public class OpenRewriteEngine implements RefactoringEngine {

    private List<Symbol> indexedSymbols = List.of();
    private List<Reference> indexedReferences = List.of();

    @Override
    public EngineType getType() {
        return EngineType.OPENREWRITE;
    }

    @Override
    public void indexProject(Path projectRoot, BuildType buildType) {
        log.info("[OpenRewriteEngine] indexProject called — not yet implemented (RFJ-050)");
        // TODO RFJ-050: parse with JavaParser.fromJavaVersion() and build LST
    }

    @Override
    public Optional<Symbol> findSymbol(String qualifiedName) {
        return indexedSymbols.stream()
                .filter(s -> s.qualifiedName().equals(qualifiedName))
                .findFirst();
    }

    @Override
    public List<Reference> findReferences(Symbol symbol) {
        return indexedReferences.stream()
                .filter(r -> r.symbol().qualifiedName().equals(symbol.qualifiedName()))
                .toList();
    }

    @Override
    public List<Reference> findReferencesInFile(Path filePath) {
        return indexedReferences.stream()
                .filter(r -> filePath.toString().equals(r.filePath()))
                .toList();
    }

    @Override
    public ChangeSet computeRename(Symbol oldSymbol, String newQualifiedName) {
        throw new UnsupportedOperationException("computeRename not yet implemented — see RFJ-050");
    }

    @Override
    public ChangeSet computeMove(Symbol symbol, String newPackageName) {
        throw new UnsupportedOperationException("computeMove not yet implemented — see RFJ-050");
    }

    @Override
    public void apply(ChangeSet changeSet, boolean dryRun) {
        throw new UnsupportedOperationException("apply not yet implemented — see RFJ-050");
    }

    @Override
    public void clearIndex() {
        indexedSymbols = List.of();
        indexedReferences = List.of();
        log.info("[OpenRewriteEngine] Index cleared");
    }

    @Override
    public List<Symbol> getAllSymbols() {
        return indexedSymbols;
    }

    @Override
    public List<Reference> getAllReferences() {
        return indexedReferences;
    }
}
