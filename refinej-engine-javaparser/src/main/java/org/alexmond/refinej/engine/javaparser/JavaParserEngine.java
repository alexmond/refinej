package org.alexmond.refinej.engine.javaparser;

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

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
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

import org.springframework.stereotype.Component;

/**
 * JavaParser-based implementation of {@link RefactoringEngine}. Lightweight baseline —
 * smallest footprint, fastest startup.
 *
 * <p>
 * Uses {@link CombinedTypeSolver} with {@link JavaSymbolSolver} for accurate symbol
 * resolution. Parses sources via {@link StaticJavaParser} and extracts symbols and
 * references using visitor-based extractors.
 */
@Slf4j
@Component
public class JavaParserEngine implements RefactoringEngine {

	private final ClasspathResolver classpathResolver;

	private final ChangeApplier changeApplier;

	private final DiffGenerator diffGenerator;

	private Map<String, Symbol> symbolsByFqn = new HashMap<>();

	private Map<Long, List<Reference>> referencesBySymbolId = new HashMap<>();

	private Map<String, List<Reference>> referencesByFile = new HashMap<>();

	private final AtomicLong idSequence = new AtomicLong(1);

	public JavaParserEngine(ClasspathResolver classpathResolver, ChangeApplier changeApplier,
			DiffGenerator diffGenerator) {
		this.classpathResolver = classpathResolver;
		this.changeApplier = changeApplier;
		this.diffGenerator = diffGenerator;
	}

	@Override
	public EngineType getType() {
		return EngineType.JAVAPARSER;
	}

	@Override
	public void indexProject(Path projectRoot, BuildType buildType) {
		log.info("[JavaParserEngine] Indexing {} (build: {})", projectRoot, buildType);

		Path srcMain = projectRoot.resolve("src/main/java");
		Path srcRoot = srcMain.toFile().isDirectory() ? srcMain : projectRoot;

		// Set up type solver
		CombinedTypeSolver typeSolver = new CombinedTypeSolver();
		typeSolver.add(new ReflectionTypeSolver());
		typeSolver.add(new JavaParserTypeSolver(srcRoot));

		ParserConfiguration config = new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(typeSolver))
			.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
		StaticJavaParser.setConfiguration(config);

		// Collect all Java files
		List<Path> javaFiles;
		try (Stream<Path> walk = Files.walk(srcRoot)) {
			javaFiles = walk.filter((p) -> p.toString().endsWith(".java")).toList();
		}
		catch (IOException ex) {
			log.error("[JavaParserEngine] Failed to walk source tree: {}", ex.getMessage());
			return;
		}

		// Parse all files
		List<CompilationUnit> compilationUnits = new ArrayList<>();
		for (Path file : javaFiles) {
			try {
				CompilationUnit cu = StaticJavaParser.parse(file);
				cu.setStorage(file);
				compilationUnits.add(cu);
			}
			catch (IOException ex) {
				log.warn("[JavaParserEngine] Failed to parse {}: {}", file, ex.getMessage());
			}
		}

		// Extract symbols
		JPSymbolExtractor symbolExtractor = new JPSymbolExtractor(this.idSequence);
		for (CompilationUnit cu : compilationUnits) {
			Path sourceFile = cu.getStorage().map((s) -> s.getPath()).orElse(null);
			if (sourceFile != null) {
				symbolExtractor.extractFrom(cu, sourceFile);
			}
		}
		List<Symbol> extractedSymbols = symbolExtractor.getSymbols();

		Map<String, Symbol> newSymbolsByFqn = new HashMap<>();
		extractedSymbols.forEach((s) -> newSymbolsByFqn.put(s.qualifiedName(), s));

		// Extract references
		JPReferenceExtractor refExtractor = new JPReferenceExtractor(newSymbolsByFqn, this.idSequence);
		for (CompilationUnit cu : compilationUnits) {
			Path sourceFile = cu.getStorage().map((s) -> s.getPath()).orElse(null);
			if (sourceFile != null) {
				refExtractor.extractFrom(cu, sourceFile);
			}
		}
		List<Reference> extractedRefs = refExtractor.getReferences();

		// Build index maps
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

		log.info("[JavaParserEngine] Indexed {} symbols, {} references", extractedSymbols.size(), extractedRefs.size());
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
		log.info("[JavaParserEngine] Computing rename: {} → {}", oldSymbol.qualifiedName(), newQualifiedName);
		return new JPRenameComputer(this.symbolsByFqn, this.referencesBySymbolId, this.diffGenerator).compute(oldSymbol,
				newQualifiedName);
	}

	@Override
	public ChangeSet computeMove(Symbol symbol, String newPackageName) {
		log.info("[JavaParserEngine] Computing move: {} → {}", symbol.qualifiedName(), newPackageName);
		return new JPMoveComputer(this.symbolsByFqn, this.referencesBySymbolId, this.diffGenerator).compute(symbol,
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
		log.info("[JavaParserEngine] Index cleared");
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
