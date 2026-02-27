package org.alexmond.refinej.engine.javaparser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import org.alexmond.refinej.core.domain.Reference;
import org.alexmond.refinej.core.domain.Symbol;
import org.alexmond.refinej.core.domain.UsageKind;

/**
 * JavaParser visitor that extracts {@link Reference} instances from a
 * {@link CompilationUnit}. Handles imports, extends/implements, new-instance,
 * method-call, field-access, annotations, and type references.
 */
class JPReferenceExtractor extends VoidVisitorAdapter<Void> {

	private final Map<String, Symbol> symbolIndex;

	private final AtomicLong idSequence;

	private final List<Reference> references = new ArrayList<>();

	private final Set<String> seen = new HashSet<>();

	private String currentFilePath;

	JPReferenceExtractor(Map<String, Symbol> symbolIndex, AtomicLong idSequence) {
		this.symbolIndex = symbolIndex;
		this.idSequence = idSequence;
	}

	List<Reference> getReferences() {
		return List.copyOf(this.references);
	}

	void extractFrom(CompilationUnit cu, Path sourceFile) {
		this.currentFilePath = sourceFile.toAbsolutePath().toString();
		cu.accept(this, null);
	}

	@Override
	public void visit(ImportDeclaration id, Void arg) {
		String importedName = id.getNameAsString();
		Symbol sym = this.symbolIndex.get(importedName);
		if (sym != null) {
			int line = id.getBegin().map((p) -> p.line).orElse(0);
			int col = id.getBegin().map((p) -> p.column).orElse(0);
			addRef(sym, line, col, UsageKind.IMPORT);
		}
		super.visit(id, arg);
	}

	@Override
	public void visit(ClassOrInterfaceDeclaration cid, Void arg) {
		// Handle extends
		for (ClassOrInterfaceType extendedType : cid.getExtendedTypes()) {
			resolveTypeRef(extendedType, UsageKind.EXTENDS);
		}
		// Handle implements
		for (ClassOrInterfaceType implementedType : cid.getImplementedTypes()) {
			resolveTypeRef(implementedType, UsageKind.IMPLEMENTS);
		}
		super.visit(cid, arg);
	}

	@Override
	public void visit(ObjectCreationExpr oce, Void arg) {
		resolveTypeRef(oce.getType(), UsageKind.NEW_INSTANCE);
		super.visit(oce, arg);
	}

	@Override
	public void visit(MethodCallExpr mce, Void arg) {
		try {
			ResolvedMethodDeclaration resolved = mce.resolve();
			String declaringType = resolved.declaringType().getQualifiedName();
			String methodName = resolved.getName();
			String prefix = declaringType + "#" + methodName + "(";
			this.symbolIndex.entrySet()
				.stream()
				.filter((e) -> e.getKey().startsWith(prefix))
				.findFirst()
				.ifPresent((e) -> {
					int line = mce.getBegin().map((p) -> p.line).orElse(0);
					int col = mce.getBegin().map((p) -> p.column).orElse(0);
					addRef(e.getValue(), line, col, UsageKind.METHOD_CALL);
				});
		}
		catch (Exception ex) {
			// Symbol resolution failed — skip
		}
		super.visit(mce, arg);
	}

	@Override
	public void visit(FieldAccessExpr fae, Void arg) {
		try {
			ResolvedFieldDeclaration resolved = fae.resolve().asField();
			String fqn = resolved.declaringType().getQualifiedName() + "#" + resolved.getName();
			Symbol sym = this.symbolIndex.get(fqn);
			if (sym != null) {
				int line = fae.getBegin().map((p) -> p.line).orElse(0);
				int col = fae.getBegin().map((p) -> p.column).orElse(0);
				addRef(sym, line, col, UsageKind.FIELD_ACCESS);
			}
		}
		catch (Exception ex) {
			// Symbol resolution failed — skip
		}
		super.visit(fae, arg);
	}

	@Override
	public void visit(NameExpr ne, Void arg) {
		// NameExpr can be a field access (e.g. "name" referring to this.name)
		try {
			ResolvedType resolved = ne.calculateResolvedType();
			// If it resolves to a type reference, track it
			if (resolved.isReferenceType()) {
				String fqn = resolved.asReferenceType().getQualifiedName();
				Symbol sym = this.symbolIndex.get(fqn);
				if (sym != null) {
					int line = ne.getBegin().map((p) -> p.line).orElse(0);
					int col = ne.getBegin().map((p) -> p.column).orElse(0);
					addRef(sym, line, col, UsageKind.TYPE_REFERENCE);
				}
			}
		}
		catch (Exception ex) {
			// Resolution failed — skip
		}
		super.visit(ne, arg);
	}

	@Override
	public void visit(com.github.javaparser.ast.expr.MarkerAnnotationExpr ae, Void arg) {
		resolveAnnotation(ae);
		super.visit(ae, arg);
	}

	@Override
	public void visit(com.github.javaparser.ast.expr.SingleMemberAnnotationExpr ae, Void arg) {
		resolveAnnotation(ae);
		super.visit(ae, arg);
	}

	@Override
	public void visit(com.github.javaparser.ast.expr.NormalAnnotationExpr ae, Void arg) {
		resolveAnnotation(ae);
		super.visit(ae, arg);
	}

	private void resolveAnnotation(com.github.javaparser.ast.expr.AnnotationExpr ae) {
		String name = ae.getNameAsString();
		for (Map.Entry<String, Symbol> entry : this.symbolIndex.entrySet()) {
			if (entry.getKey().endsWith("." + name) || entry.getKey().equals(name)) {
				int line = ae.getBegin().map((p) -> p.line).orElse(0);
				int col = ae.getBegin().map((p) -> p.column).orElse(0);
				addRef(entry.getValue(), line, col, UsageKind.ANNOTATION);
				break;
			}
		}
	}

	@Override
	public void visit(ClassOrInterfaceType type, Void arg) {
		// General type reference (parameters, return types, variable declarations)
		resolveTypeRef(type, UsageKind.TYPE_REFERENCE);
		super.visit(type, arg);
	}

	private void resolveTypeRef(ClassOrInterfaceType type, UsageKind kind) {
		try {
			ResolvedReferenceTypeDeclaration resolved = type.resolve().asReferenceType().getTypeDeclaration().get();
			String fqn = resolved.getQualifiedName();
			Symbol sym = this.symbolIndex.get(fqn);
			if (sym != null) {
				int line = type.getBegin().map((p) -> p.line).orElse(0);
				int col = type.getBegin().map((p) -> p.column).orElse(0);
				addRef(sym, line, col, kind);
			}
		}
		catch (Exception ex) {
			// Symbol resolution failed — try simple name matching as fallback
			String name = type.getNameAsString();
			for (Map.Entry<String, Symbol> entry : this.symbolIndex.entrySet()) {
				if (entry.getKey().endsWith("." + name)) {
					int line = type.getBegin().map((p) -> p.line).orElse(0);
					int col = type.getBegin().map((p) -> p.column).orElse(0);
					addRef(entry.getValue(), line, col, kind);
					break;
				}
			}
		}
	}

	private void addRef(Symbol sym, int line, int col, UsageKind kind) {
		if (line == 0) {
			return;
		}
		String dedupeKey = sym.qualifiedName() + "|" + this.currentFilePath + "|" + line + "|" + col + "|" + kind;
		if (this.seen.add(dedupeKey)) {
			this.references
				.add(new Reference(this.idSequence.getAndIncrement(), sym, this.currentFilePath, line, col, kind));
		}
	}

}
