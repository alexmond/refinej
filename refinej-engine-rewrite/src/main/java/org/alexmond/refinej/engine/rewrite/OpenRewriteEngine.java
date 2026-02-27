package org.alexmond.refinej.engine.rewrite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import org.springframework.stereotype.Component;

/**
 * OpenRewrite-based implementation of {@link RefactoringEngine}.
 *
 * <p>
 * Phase 5: full implementation using OpenRewrite's Java parser for indexing and
 * text-based rename/move computers for refactoring operations.
 */
@Slf4j
@Component
public class OpenRewriteEngine implements RefactoringEngine {

	private final ClasspathResolver classpathResolver;

	private final ChangeApplier changeApplier;

	private final DiffGenerator diffGenerator;

	/** In-memory symbol store (qualifiedName -> Symbol). */
	private Map<String, Symbol> symbolsByFqn = new HashMap<>();

	/** In-memory reference store (symbolId -> References). */
	private Map<Long, List<Reference>> referencesBySymbolId = new HashMap<>();

	/** References indexed by filePath for fast {@link #findReferencesInFile} queries. */
	private Map<String, List<Reference>> referencesByFile = new HashMap<>();

	private final AtomicLong idSequence = new AtomicLong(1);

	public OpenRewriteEngine(ClasspathResolver classpathResolver, ChangeApplier changeApplier,
			DiffGenerator diffGenerator) {
		this.classpathResolver = classpathResolver;
		this.changeApplier = changeApplier;
		this.diffGenerator = diffGenerator;
	}

	@Override
	public EngineType getType() {
		return EngineType.OPENREWRITE;
	}

	@Override
	public void indexProject(Path projectRoot, BuildType buildType) {
		log.info("[OpenRewriteEngine] Indexing {} (build: {})", projectRoot, buildType);

		List<Path> classpath = this.classpathResolver.resolve(projectRoot, buildType);
		if (classpath.isEmpty()) {
			log.warn("[OpenRewriteEngine] No classpath resolved — symbol resolution will be partial");
		}

		Path srcMain = projectRoot.resolve("src/main/java");
		Path srcRoot = srcMain.toFile().isDirectory() ? srcMain : projectRoot;

		List<Path> javaFiles = collectJavaFiles(srcRoot);
		if (javaFiles.isEmpty()) {
			log.warn("[OpenRewriteEngine] No Java files found under {}", srcRoot);
			return;
		}

		JavaParser parser = JavaParser.fromJavaVersion().classpath(classpath).build();

		List<J.CompilationUnit> compilationUnits = new ArrayList<>();
		parser
			.parse(javaFiles, projectRoot,
					new InMemoryExecutionContext((ex) -> log.warn("[OpenRewriteEngine] Parse error", ex)))
			.forEach((sf) -> {
				if (sf instanceof J.CompilationUnit cu) {
					compilationUnits.add(cu);
				}
			});

		log.info("[OpenRewriteEngine] Parsed {} compilation units", compilationUnits.size());

		// Extract symbols
		RewriteSymbolExtractor symbolExtractor = new RewriteSymbolExtractor(this.idSequence);
		for (J.CompilationUnit cu : compilationUnits) {
			symbolExtractor.extractFrom(cu, projectRoot);
		}
		List<Symbol> extractedSymbols = symbolExtractor.getSymbols();

		Map<String, Symbol> newSymbolsByFqn = new HashMap<>();
		extractedSymbols.forEach((s) -> newSymbolsByFqn.put(s.qualifiedName(), s));

		// Extract references
		RewriteReferenceExtractor refExtractor = new RewriteReferenceExtractor(newSymbolsByFqn, this.idSequence);
		for (J.CompilationUnit cu : compilationUnits) {
			refExtractor.extractFrom(cu, projectRoot);
		}
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

		log.info("[OpenRewriteEngine] Indexed {} symbols, {} references", extractedSymbols.size(),
				extractedRefs.size());
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
		log.info("[OpenRewriteEngine] Computing rename: {} -> {}", oldSymbol.qualifiedName(), newQualifiedName);
		return new RewriteRenameComputer(this.symbolsByFqn, this.referencesBySymbolId, this.diffGenerator)
			.compute(oldSymbol, newQualifiedName);
	}

	@Override
	public ChangeSet computeMove(Symbol symbol, String newPackageName) {
		log.info("[OpenRewriteEngine] Computing move: {} -> {}", symbol.qualifiedName(), newPackageName);
		return new RewriteMoveComputer(this.symbolsByFqn, this.referencesBySymbolId, this.diffGenerator).compute(symbol,
				newPackageName);
	}

	@Override
	public void apply(ChangeSet changeSet, boolean dryRun) {
		try {
			this.changeApplier.backupAndApply(changeSet.changes(), dryRun);
		}
		catch (IOException ex) {
			throw new RefactorException.FileOperationException("Failed to apply changes", ex);
		}
	}

	@Override
	public void clearIndex() {
		this.symbolsByFqn = new HashMap<>();
		this.referencesBySymbolId = new HashMap<>();
		this.referencesByFile = new HashMap<>();
		this.idSequence.set(1);
		log.info("[OpenRewriteEngine] Index cleared");
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

	private static List<Path> collectJavaFiles(Path root) {
		try (Stream<Path> walk = Files.walk(root)) {
			return walk.filter(Files::isRegularFile)
				.filter((p) -> p.toString().endsWith(".java"))
				.collect(Collectors.toList());
		}
		catch (IOException ex) {
			log.warn("[OpenRewriteEngine] Could not walk directory {}: {}", root, ex.getMessage());
			return List.of();
		}
	}

}
