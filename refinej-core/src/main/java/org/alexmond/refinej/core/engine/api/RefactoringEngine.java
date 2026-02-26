package org.alexmond.refinej.core.engine.api;

import org.alexmond.refinej.core.domain.ChangeSet;
import org.alexmond.refinej.core.domain.Reference;
import org.alexmond.refinej.core.domain.Symbol;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Central abstraction for all three refactoring engines (Spoon, OpenRewrite, JavaParser).
 *
 * <p>Contract:
 * <ul>
 *   <li>{@link #indexProject} must be called before any query or compute method.</li>
 *   <li>{@link #computeRename} and {@link #computeMove} are pure — they produce a
 *       {@link ChangeSet} but never write to disk.</li>
 *   <li>{@link #apply} is the only method that modifies the filesystem, and only
 *       when {@code dryRun} is {@code false}.</li>
 * </ul>
 */
public interface RefactoringEngine {

    /** Identifies which engine implementation this is. */
    EngineType getType();

    /**
     * Parse the project rooted at {@code projectRoot} and build the internal semantic
     * model. Must be called before any query or refactoring operation.
     */
    void indexProject(Path projectRoot, BuildType buildType);

    /** Look up a symbol by its fully-qualified name. Returns empty if not indexed. */
    Optional<Symbol> findSymbol(String qualifiedName);

    /** Return all known usages of the given symbol. */
    List<Reference> findReferences(Symbol symbol);

    /** Return all references found in a specific file. */
    List<Reference> findReferencesInFile(Path filePath);

    /**
     * Compute all source changes needed to rename {@code oldSymbol} to
     * {@code newQualifiedName}. Does NOT write any files.
     */
    ChangeSet computeRename(Symbol oldSymbol, String newQualifiedName);

    /**
     * Compute all source changes needed to move {@code symbol} to
     * {@code newPackageName}. Does NOT write any files.
     */
    ChangeSet computeMove(Symbol symbol, String newPackageName);

    /**
     * Write the changes in {@code changeSet} to disk. Backs up all affected files
     * first. A no-op when {@code dryRun} is {@code true}.
     */
    void apply(ChangeSet changeSet, boolean dryRun);

    /** Discard all indexed state so the next {@link #indexProject} call starts fresh. */
    void clearIndex();

    /** Return all symbols extracted during the last {@link #indexProject} call. */
    List<Symbol> getAllSymbols();

    /** Return all references extracted during the last {@link #indexProject} call. */
    List<Reference> getAllReferences();
}
