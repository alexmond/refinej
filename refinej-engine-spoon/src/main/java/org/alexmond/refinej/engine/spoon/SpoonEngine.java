package org.alexmond.refinej.engine.spoon;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.alexmond.refinej.core.domain.ChangeSet;
import org.alexmond.refinej.core.domain.Reference;
import org.alexmond.refinej.core.domain.Symbol;
import org.alexmond.refinej.core.engine.api.BuildType;
import org.alexmond.refinej.core.engine.api.ChangeApplier;
import org.alexmond.refinej.core.engine.api.ClasspathResolver;
import org.alexmond.refinej.core.engine.api.EngineType;
import org.alexmond.refinej.core.engine.api.RefactoringEngine;
import org.alexmond.refinej.core.exception.RefactorException;
import org.alexmond.refinej.core.util.DiffGenerator;
import spoon.Launcher;

import org.springframework.stereotype.Component;

/**
 * Spoon-based implementation of {@link RefactoringEngine}.
 *
 * <p>
 * Phase 2: full indexing via Spoon {@link Launcher}. Symbols and references are held in
 * in-memory maps until Phase 3 wires in the JPA persistence layer.
 */
@Slf4j
@Component
public class SpoonEngine implements RefactoringEngine {

	private final ClasspathResolver classpathResolver;

	private final ChangeApplier changeApplier;

	private final DiffGenerator diffGenerator;

	/** In-memory symbol store (qualifiedName → Symbol). Replaced by JPA in Phase 3. */
	private Map<String, Symbol> symbolsByFqn = new HashMap<>();

	/** In-memory reference store (symbolId → References). Replaced by JPA in Phase 3. */
	private Map<Long, List<Reference>> referencesBySymbolId = new HashMap<>();

	/** References indexed by filePath for fast {@link #findReferencesInFile} queries. */
	private Map<String, List<Reference>> referencesByFile = new HashMap<>();

	private final AtomicLong idSequence = new AtomicLong(1);

	public SpoonEngine(ClasspathResolver classpathResolver, ChangeApplier changeApplier, DiffGenerator diffGenerator) {
		this.classpathResolver = classpathResolver;
		this.changeApplier = changeApplier;
		this.diffGenerator = diffGenerator;
	}

	@Override
	public EngineType getType() {
		return EngineType.SPOON;
	}

	@Override
	public void indexProject(Path projectRoot, BuildType buildType) {
		log.info("[SpoonEngine] Indexing {} (build: {})", projectRoot, buildType);

		List<Path> classpath = this.classpathResolver.resolve(projectRoot, buildType);
		boolean noClasspath = classpath.isEmpty();
		if (noClasspath) {
			log.warn("[SpoonEngine] No classpath resolved — symbol resolution will be partial");
		}

		Launcher launcher = new Launcher();
		Path srcMain = projectRoot.resolve("src/main/java");
		Path srcRoot = srcMain.toFile().isDirectory() ? srcMain : projectRoot;
		launcher.addInputResource(srcRoot.toString());
		launcher.getEnvironment().setSourceClasspath(classpath.stream().map(Path::toString).toArray(String[]::new));
		launcher.getEnvironment().setAutoImports(true);
		launcher.getEnvironment().setNoClasspath(noClasspath);
		launcher.getEnvironment().setComplianceLevel(17);
		launcher.getEnvironment().setCommentEnabled(false);

		var model = launcher.buildModel();

		SymbolExtractor symbolExtractor = new SymbolExtractor(this.idSequence);
		model.getRootPackage().accept(symbolExtractor);
		List<Symbol> extractedSymbols = symbolExtractor.getSymbols();

		Map<String, Symbol> newSymbolsByFqn = new HashMap<>();
		extractedSymbols.forEach((s) -> newSymbolsByFqn.put(s.qualifiedName(), s));

		ReferenceExtractor refExtractor = new ReferenceExtractor(newSymbolsByFqn, this.idSequence);
		model.getRootPackage().accept(refExtractor);
		List<Reference> extractedRefs = refExtractor.getReferences();

		Map<Long, List<Reference>> newRefsBySymbolId = new HashMap<>();
		Map<String, List<Reference>> newRefsByFile = new HashMap<>();
		for (Reference ref : extractedRefs) {
			newRefsBySymbolId.computeIfAbsent(ref.symbol().id(), (k) -> new ArrayList<>()).add(ref);
			if (ref.filePath() != null) {
				newRefsByFile.computeIfAbsent(ref.filePath(), (k) -> new ArrayList<>()).add(ref);
			}
		}

		this.symbolsByFqn = newSymbolsByFqn;
		this.referencesBySymbolId = newRefsBySymbolId;
		this.referencesByFile = newRefsByFile;

		log.info("[SpoonEngine] Indexed {} symbols, {} references", extractedSymbols.size(), extractedRefs.size());
	}

	@Override
	public Optional<Symbol> findSymbol(String qualifiedName) {
		return Optional.ofNullable(this.symbolsByFqn.get(qualifiedName));
	}

	@Override
	public List<Reference> findReferences(Symbol symbol) {
		return this.referencesBySymbolId.getOrDefault(symbol.id(), List.of());
	}

	@Override
	public List<Reference> findReferencesInFile(Path filePath) {
		return this.referencesByFile.getOrDefault(filePath.toAbsolutePath().toString(), List.of());
	}

	@Override
	public ChangeSet computeRename(Symbol oldSymbol, String newQualifiedName) {
		log.info("[SpoonEngine] Computing rename: {} → {}", oldSymbol.qualifiedName(), newQualifiedName);
		return new SpoonRenameComputer(this.symbolsByFqn, this.referencesBySymbolId, this.diffGenerator)
			.compute(oldSymbol, newQualifiedName);
	}

	@Override
	public ChangeSet computeMove(Symbol symbol, String newPackageName) {
		log.info("[SpoonEngine] Computing move: {} → {}", symbol.qualifiedName(), newPackageName);
		return new SpoonMoveComputer(this.symbolsByFqn, this.referencesBySymbolId, this.diffGenerator).compute(symbol,
				newPackageName);
	}

	@Override
	public void apply(ChangeSet changeSet, boolean dryRun) {
		try {
			this.changeApplier.backupAndApply(changeSet.changes(), dryRun);
		}
		catch (java.io.IOException ex) {
			throw new RefactorException.FileOperationException("Failed to apply changes", ex);
		}
	}

	@Override
	public void clearIndex() {
		this.symbolsByFqn = new HashMap<>();
		this.referencesBySymbolId = new HashMap<>();
		this.referencesByFile = new HashMap<>();
		this.idSequence.set(1);
		log.info("[SpoonEngine] Index cleared");
	}

	@Override
	public List<Symbol> getAllSymbols() {
		return List.copyOf(this.symbolsByFqn.values());
	}

	@Override
	public List<Reference> getAllReferences() {
		return this.referencesBySymbolId.values()
			.stream()
			.flatMap(List::stream)
			.collect(Collectors.toUnmodifiableList());
	}

}
