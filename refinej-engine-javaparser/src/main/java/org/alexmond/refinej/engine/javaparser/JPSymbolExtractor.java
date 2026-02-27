package org.alexmond.refinej.engine.javaparser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.alexmond.refinej.core.domain.Symbol;
import org.alexmond.refinej.core.domain.SymbolKind;

/**
 * JavaParser visitor that extracts {@link Symbol} instances from a
 * {@link CompilationUnit}.
 */
class JPSymbolExtractor extends VoidVisitorAdapter<Void> {

	private final AtomicLong idSequence;

	private final List<Symbol> symbols = new ArrayList<>();

	private String currentFilePath;

	private String currentPackage = "";

	JPSymbolExtractor(AtomicLong idSequence) {
		this.idSequence = idSequence;
	}

	List<Symbol> getSymbols() {
		return List.copyOf(this.symbols);
	}

	void extractFrom(CompilationUnit cu, Path sourceFile) {
		this.currentFilePath = sourceFile.toAbsolutePath().toString();
		this.currentPackage = cu.getPackageDeclaration().map(PackageDeclaration::getNameAsString).orElse("");
		cu.accept(this, null);
	}

	@Override
	public void visit(PackageDeclaration pd, Void arg) {
		String name = pd.getNameAsString();
		int line = pd.getBegin().map((p) -> p.line).orElse(0);
		this.symbols.add(new Symbol(this.idSequence.getAndIncrement(), SymbolKind.PACKAGE,
				name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : name, name, null, line, line, null));
		super.visit(pd, arg);
	}

	@Override
	public void visit(ClassOrInterfaceDeclaration cid, Void arg) {
		String simpleName = cid.getNameAsString();
		String fqn = resolveFqn(cid);
		int lineStart = cid.getBegin().map((p) -> p.line).orElse(0);
		int lineEnd = cid.getEnd().map((p) -> p.line).orElse(0);
		this.symbols.add(new Symbol(this.idSequence.getAndIncrement(), SymbolKind.CLASS, simpleName, fqn,
				this.currentFilePath, lineStart, lineEnd, null));
		super.visit(cid, arg);
	}

	@Override
	public void visit(EnumDeclaration ed, Void arg) {
		String simpleName = ed.getNameAsString();
		String fqn = resolveFqn(ed);
		int lineStart = ed.getBegin().map((p) -> p.line).orElse(0);
		int lineEnd = ed.getEnd().map((p) -> p.line).orElse(0);
		this.symbols.add(new Symbol(this.idSequence.getAndIncrement(), SymbolKind.CLASS, simpleName, fqn,
				this.currentFilePath, lineStart, lineEnd, null));
		super.visit(ed, arg);
	}

	@Override
	public void visit(RecordDeclaration rd, Void arg) {
		String simpleName = rd.getNameAsString();
		String fqn = resolveFqn(rd);
		int lineStart = rd.getBegin().map((p) -> p.line).orElse(0);
		int lineEnd = rd.getEnd().map((p) -> p.line).orElse(0);
		this.symbols.add(new Symbol(this.idSequence.getAndIncrement(), SymbolKind.CLASS, simpleName, fqn,
				this.currentFilePath, lineStart, lineEnd, null));
		super.visit(rd, arg);
	}

	@Override
	public void visit(MethodDeclaration md, Void arg) {
		String simpleName = md.getNameAsString();
		String declaringType = findDeclaringType(md);
		String params = md.getParameters()
			.stream()
			.map((p) -> p.getType().asString())
			.reduce((a, b) -> a + "," + b)
			.orElse("");
		String fqn = declaringType + "#" + simpleName + "(" + params + ")";
		String returnType = md.getType().asString();
		int lineStart = md.getBegin().map((p) -> p.line).orElse(0);
		int lineEnd = md.getEnd().map((p) -> p.line).orElse(0);
		this.symbols.add(new Symbol(this.idSequence.getAndIncrement(), SymbolKind.METHOD, simpleName, fqn,
				this.currentFilePath, lineStart, lineEnd, returnType));
		super.visit(md, arg);
	}

	@Override
	public void visit(FieldDeclaration fd, Void arg) {
		String declaringType = findDeclaringType(fd);
		String fieldType = fd.getElementType().asString();
		int lineStart = fd.getBegin().map((p) -> p.line).orElse(0);
		int lineEnd = fd.getEnd().map((p) -> p.line).orElse(0);
		for (VariableDeclarator var : fd.getVariables()) {
			String simpleName = var.getNameAsString();
			String fqn = declaringType + "#" + simpleName;
			this.symbols.add(new Symbol(this.idSequence.getAndIncrement(), SymbolKind.FIELD, simpleName, fqn,
					this.currentFilePath, lineStart, lineEnd, fieldType));
		}
		// don't call super — we already processed the variable declarators
	}

	private String resolveFqn(com.github.javaparser.ast.body.TypeDeclaration<?> type) {
		// Check if this is a nested type
		if (type.getParentNode().isPresent()
				&& type.getParentNode().get() instanceof com.github.javaparser.ast.body.TypeDeclaration<?> parent) {
			return resolveFqn(parent) + "." + type.getNameAsString();
		}
		return this.currentPackage.isEmpty() ? type.getNameAsString()
				: this.currentPackage + "." + type.getNameAsString();
	}

	private String findDeclaringType(com.github.javaparser.ast.Node node) {
		return node.getParentNode()
			.filter((p) -> p instanceof com.github.javaparser.ast.body.TypeDeclaration<?>)
			.map((p) -> resolveFqn((com.github.javaparser.ast.body.TypeDeclaration<?>) p))
			.orElse(this.currentPackage);
	}

}
