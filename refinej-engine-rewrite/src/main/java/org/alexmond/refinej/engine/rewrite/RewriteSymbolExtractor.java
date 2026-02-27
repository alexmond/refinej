package org.alexmond.refinej.engine.rewrite;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.alexmond.refinej.core.domain.Symbol;
import org.alexmond.refinej.core.domain.SymbolKind;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

/**
 * OpenRewrite visitor that extracts {@link Symbol} instances from a
 * {@link J.CompilationUnit}.
 *
 * <p>
 * Handles classes, interfaces, enums, records, methods, and fields. Line numbers are
 * computed by searching the source text for declaration patterns.
 */
class RewriteSymbolExtractor extends JavaIsoVisitor<ExecutionContext> {

	private final AtomicLong idSequence;

	private final List<Symbol> symbols = new ArrayList<>();

	private String currentFilePath;

	private String[] currentLines;

	RewriteSymbolExtractor(AtomicLong idSequence) {
		this.idSequence = idSequence;
	}

	List<Symbol> getSymbols() {
		return List.copyOf(this.symbols);
	}

	void extractFrom(J.CompilationUnit cu, Path sourceRoot) {
		this.currentFilePath = sourceRoot.resolve(cu.getSourcePath()).toAbsolutePath().normalize().toString();
		this.currentLines = cu.printAll().split("\n", -1);
		this.visit(cu, new InMemoryExecutionContext());
	}

	@Override
	public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
		JavaType.FullyQualified type = classDecl.getType();
		if (type != null && !classDecl.getSimpleName().isEmpty()) {
			String fqn = type.getFullyQualifiedName();
			String simpleName = classDecl.getSimpleName();
			String kindKeyword = classDeclarationKeyword(classDecl);
			int lineStart = findLine(kindKeyword + " " + simpleName, this.currentLines);
			int lineEnd = lineStart; // approximate

			this.symbols.add(new Symbol(this.idSequence.getAndIncrement(), SymbolKind.CLASS, simpleName, fqn,
					this.currentFilePath, lineStart, lineEnd, null));

			// Extract fields from this class body
			extractFields(classDecl, type);
		}
		return super.visitClassDeclaration(classDecl, ctx);
	}

	@Override
	public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
		JavaType.Method methodType = method.getMethodType();
		if (methodType != null && methodType.getDeclaringType() != null) {
			String simpleName = method.getSimpleName();
			String declaringFqn = methodType.getDeclaringType().getFullyQualifiedName();
			String params = methodType.getParameterTypes()
				.stream()
				.map(RewriteSymbolExtractor::typeToFqn)
				.collect(Collectors.joining(","));
			String fqn = declaringFqn + "#" + simpleName + "(" + params + ")";
			String returnType = (methodType.getReturnType() != null) ? typeToFqn(methodType.getReturnType()) : null;
			int lineStart = findLine(simpleName + "(", this.currentLines);

			this.symbols.add(new Symbol(this.idSequence.getAndIncrement(), SymbolKind.METHOD, simpleName, fqn,
					this.currentFilePath, lineStart, lineStart, returnType));
		}
		return super.visitMethodDeclaration(method, ctx);
	}

	@Override
	public J.Package visitPackage(J.Package pkg, ExecutionContext ctx) {
		if (pkg.getExpression() != null) {
			String fqn = pkg.getExpression().printTrimmed(getCursor());
			String simpleName = fqn.contains(".") ? fqn.substring(fqn.lastIndexOf('.') + 1) : fqn;
			this.symbols.add(new Symbol(this.idSequence.getAndIncrement(), SymbolKind.PACKAGE, simpleName, fqn, null, 0,
					0, null));
		}
		return super.visitPackage(pkg, ctx);
	}

	private void extractFields(J.ClassDeclaration classDecl, JavaType.FullyQualified declaringType) {
		if (classDecl.getBody() == null) {
			return;
		}
		for (Statement stmt : classDecl.getBody().getStatements()) {
			if (stmt instanceof J.VariableDeclarations varDecls) {
				for (J.VariableDeclarations.NamedVariable var : varDecls.getVariables()) {
					String simpleName = var.getSimpleName();
					if (simpleName.isEmpty()) {
						continue;
					}
					String fqn = declaringType.getFullyQualifiedName() + "#" + simpleName;
					String fieldType = null;
					if (var.getVariableType() != null && var.getVariableType().getType() != null) {
						fieldType = typeToFqn(var.getVariableType().getType());
					}
					int lineStart = findLine(simpleName, this.currentLines);
					this.symbols.add(new Symbol(this.idSequence.getAndIncrement(), SymbolKind.FIELD, simpleName, fqn,
							this.currentFilePath, lineStart, lineStart, fieldType));
				}
			}
		}
	}

	private static String classDeclarationKeyword(J.ClassDeclaration classDecl) {
		return switch (classDecl.getKind()) {
			case Interface -> "interface";
			case Enum -> "enum";
			case Record -> "record";
			case Annotation -> "@interface";
			default -> "class";
		};
	}

	static String typeToFqn(JavaType type) {
		if (type instanceof JavaType.FullyQualified fq) {
			return fq.getFullyQualifiedName();
		}
		if (type instanceof JavaType.Primitive p) {
			return p.getKeyword();
		}
		if (type instanceof JavaType.Array arr) {
			return typeToFqn(arr.getElemType()) + "[]";
		}
		return type.toString();
	}

	private static int findLine(String pattern, String[] lines) {
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].contains(pattern)) {
				return i + 1; // 1-based
			}
		}
		return 0;
	}

}
