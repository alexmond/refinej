package org.alexmond.refinej.engine.rewrite;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.alexmond.refinej.core.domain.Reference;
import org.alexmond.refinej.core.domain.Symbol;
import org.alexmond.refinej.core.domain.UsageKind;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;

/**
 * OpenRewrite visitor that extracts {@link Reference} instances from a
 * {@link J.CompilationUnit}.
 *
 * <p>
 * Handles imports, new-instance calls, method calls, extends/implements, annotations,
 * type references, and field accesses. Deduplicates by (filePath, line, column, symbol).
 */
class RewriteReferenceExtractor extends JavaIsoVisitor<ExecutionContext> {

	private final Map<String, Symbol> symbolIndex;

	private final AtomicLong idSequence;

	private final List<Reference> references = new ArrayList<>();

	private final Set<String> seen = new HashSet<>();

	private String currentFilePath;

	private String[] currentLines;

	RewriteReferenceExtractor(Map<String, Symbol> symbolIndex, AtomicLong idSequence) {
		this.symbolIndex = symbolIndex;
		this.idSequence = idSequence;
	}

	List<Reference> getReferences() {
		return List.copyOf(this.references);
	}

	void extractFrom(J.CompilationUnit cu, Path sourceRoot) {
		this.currentFilePath = sourceRoot.resolve(cu.getSourcePath()).toAbsolutePath().normalize().toString();
		this.currentLines = cu.printAll().split("\n", -1);
		this.visit(cu, new InMemoryExecutionContext());
	}

	@Override
	public J.Import visitImport(J.Import _import, ExecutionContext ctx) {
		if (_import.getTypeName() != null) {
			String fqn = _import.getTypeName();
			Symbol sym = this.symbolIndex.get(fqn);
			if (sym != null) {
				int line = findLine("import " + fqn, this.currentLines);
				addRef(sym, line, 0, UsageKind.IMPORT);
			}
		}
		return super.visitImport(_import, ctx);
	}

	@Override
	public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
		// Check extends
		if (classDecl.getExtends() != null) {
			addTypeTreeRef(classDecl.getExtends(), UsageKind.EXTENDS);
		}
		// Check implements
		if (classDecl.getImplements() != null) {
			for (TypeTree iface : classDecl.getImplements()) {
				addTypeTreeRef(iface, UsageKind.IMPLEMENTS);
			}
		}
		return super.visitClassDeclaration(classDecl, ctx);
	}

	@Override
	public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
		if (newClass.getClazz() != null) {
			addTypeTreeRef(newClass.getClazz(), UsageKind.NEW_INSTANCE);
		}
		return super.visitNewClass(newClass, ctx);
	}

	@Override
	public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
		JavaType.Method methodType = method.getMethodType();
		if (methodType != null && methodType.getDeclaringType() != null) {
			String fqnPrefix = methodType.getDeclaringType().getFullyQualifiedName() + "#" + method.getSimpleName()
					+ "(";
			this.symbolIndex.entrySet()
				.stream()
				.filter((e) -> e.getKey().startsWith(fqnPrefix))
				.findFirst()
				.ifPresent((e) -> {
					int line = findLineForMethodCall(method.getSimpleName(), this.currentLines);
					addRef(e.getValue(), line, 0, UsageKind.METHOD_CALL);
				});
		}
		return super.visitMethodInvocation(method, ctx);
	}

	@Override
	public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
		JavaType.Variable varType = fieldAccess.getName().getFieldType();
		if (varType != null && varType.getOwner() instanceof JavaType.FullyQualified owner) {
			String fqn = owner.getFullyQualifiedName() + "#" + fieldAccess.getSimpleName();
			Symbol sym = this.symbolIndex.get(fqn);
			if (sym != null) {
				int line = findLine(fieldAccess.getSimpleName(), this.currentLines);
				addRef(sym, line, 0, UsageKind.FIELD_ACCESS);
			}
		}
		return super.visitFieldAccess(fieldAccess, ctx);
	}

	@Override
	public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
		JavaType type = identifier.getType();
		if (type instanceof JavaType.FullyQualified fq) {
			String fqn = fq.getFullyQualifiedName();
			Symbol sym = this.symbolIndex.get(fqn);
			if (sym != null) {
				int line = findLine(identifier.getSimpleName(), this.currentLines);
				addRef(sym, line, 0, UsageKind.TYPE_REFERENCE);
			}
		}
		return super.visitIdentifier(identifier, ctx);
	}

	@Override
	public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
		if (annotation.getAnnotationType() != null) {
			JavaType type = annotation.getAnnotationType().getType();
			if (type instanceof JavaType.FullyQualified fq) {
				String fqn = fq.getFullyQualifiedName();
				Symbol sym = this.symbolIndex.get(fqn);
				if (sym != null) {
					String simpleName = fqn.contains(".") ? fqn.substring(fqn.lastIndexOf('.') + 1) : fqn;
					int line = findLine("@" + simpleName, this.currentLines);
					addRef(sym, line, 0, UsageKind.ANNOTATION);
				}
			}
		}
		return super.visitAnnotation(annotation, ctx);
	}

	private void addTypeTreeRef(TypeTree typeTree, UsageKind kind) {
		JavaType type = typeTree.getType();
		if (type instanceof JavaType.FullyQualified fq) {
			String fqn = fq.getFullyQualifiedName();
			Symbol sym = this.symbolIndex.get(fqn);
			if (sym != null) {
				String searchName = fqn.contains(".") ? fqn.substring(fqn.lastIndexOf('.') + 1) : fqn;
				int line = findLine(searchName, this.currentLines);
				addRef(sym, line, 0, kind);
			}
		}
	}

	private void addRef(Symbol sym, int line, int col, UsageKind kind) {
		if (line < 1) {
			return;
		}
		String dedupeKey = sym.qualifiedName() + "|" + this.currentFilePath + "|" + line + "|" + col + "|" + kind;
		if (this.seen.add(dedupeKey)) {
			this.references
				.add(new Reference(this.idSequence.getAndIncrement(), sym, this.currentFilePath, line, col, kind));
		}
	}

	private static int findLine(String pattern, String[] lines) {
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].contains(pattern)) {
				return i + 1; // 1-based
			}
		}
		return 0;
	}

	private static int findLineForMethodCall(String methodName, String[] lines) {
		String dotPattern = "." + methodName + "(";
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].contains(dotPattern)) {
				return i + 1;
			}
		}
		return findLine(methodName + "(", lines);
	}

}
