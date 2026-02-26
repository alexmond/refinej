package org.alexmond.refinej.engine.spoon;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.alexmond.refinej.core.domain.Symbol;
import org.alexmond.refinej.core.domain.SymbolKind;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.visitor.CtScanner;

/**
 * Spoon scanner that extracts {@link Symbol} instances from a {@code CtModel}.
 *
 * <p>
 * Handles classes, interfaces, enums, records, methods, fields, and packages. Synthetic,
 * anonymous, and unnamed elements are skipped.
 */
class SymbolExtractor extends CtScanner {

	private final AtomicLong idSequence;

	private final List<Symbol> symbols = new ArrayList<>();

	SymbolExtractor(AtomicLong idSequence) {
		this.idSequence = idSequence;
	}

	List<Symbol> getSymbols() {
		return List.copyOf(this.symbols);
	}

	@Override
	public <T> void visitCtClass(CtClass<T> cls) {
		if (!cls.isAnonymous() && !cls.getSimpleName().isEmpty()) {
			this.symbols.add(toSymbol(cls, SymbolKind.CLASS, null));
		}
		super.visitCtClass(cls);
	}

	@Override
	public <T> void visitCtInterface(CtInterface<T> iface) {
		if (!iface.getSimpleName().isEmpty()) {
			this.symbols.add(toSymbol(iface, SymbolKind.CLASS, null));
		}
		super.visitCtInterface(iface);
	}

	@Override
	public <T extends Enum<?>> void visitCtEnum(CtEnum<T> ctEnum) {
		if (!ctEnum.getSimpleName().isEmpty()) {
			this.symbols.add(toSymbol(ctEnum, SymbolKind.CLASS, null));
		}
		super.visitCtEnum(ctEnum);
	}

	@Override
	public void visitCtRecord(CtRecord record) {
		if (!record.getSimpleName().isEmpty()) {
			this.symbols.add(toSymbol(record, SymbolKind.CLASS, null));
		}
		super.visitCtRecord(record);
	}

	@Override
	public <T> void visitCtMethod(CtMethod<T> method) {
		if (!method.isImplicit()) {
			String returnType = (method.getType() != null) ? method.getType().getQualifiedName() : null;
			this.symbols.add(toSymbol(method, SymbolKind.METHOD, returnType));
		}
		super.visitCtMethod(method);
	}

	@Override
	public <T> void visitCtField(CtField<T> field) {
		if (!field.isImplicit() && !field.getSimpleName().isEmpty()) {
			String fieldType = (field.getType() != null) ? field.getType().getQualifiedName() : null;
			this.symbols.add(toSymbol(field, SymbolKind.FIELD, fieldType));
		}
		super.visitCtField(field);
	}

	@Override
	public void visitCtPackage(CtPackage pkg) {
		if (!pkg.isUnnamedPackage() && !pkg.getSimpleName().isEmpty()) {
			this.symbols.add(new Symbol(this.idSequence.getAndIncrement(), SymbolKind.PACKAGE, pkg.getSimpleName(),
					pkg.getQualifiedName(), null, 0, 0, null));
		}
		super.visitCtPackage(pkg);
	}

	@Override
	public <T> void visitCtLocalVariable(CtLocalVariable<T> localVariable) {
		// skip local variables — not indexed at this phase
	}

	private Symbol toSymbol(CtTypeMember member, SymbolKind kind, String typeFqn) {
		var pos = member.getPosition();
		String filePath = (pos != null && pos.isValidPosition() && pos.getFile() != null)
				? pos.getFile().getAbsolutePath() : null;
		int lineStart = (pos != null && pos.isValidPosition()) ? pos.getLine() : 0;
		int lineEnd = (pos != null && pos.isValidPosition()) ? pos.getEndLine() : 0;
		String fqn = qualifiedNameOf(member);
		return new Symbol(this.idSequence.getAndIncrement(), kind, member.getSimpleName(), fqn, filePath, lineStart,
				lineEnd, typeFqn);
	}

	private static String qualifiedNameOf(CtTypeMember member) {
		if (member instanceof CtType<?> type) {
			return type.getQualifiedName();
		}
		CtType<?> declaring = member.getDeclaringType();
		String prefix = (declaring != null) ? declaring.getQualifiedName() : "";
		if (member instanceof CtMethod<?> method) {
			StringBuilder sig = new StringBuilder(prefix).append("#").append(method.getSimpleName()).append("(");
			List<String> params = method.getParameters()
				.stream()
				.map((p) -> (p.getType() != null) ? p.getType().getQualifiedName() : "?")
				.toList();
			sig.append(String.join(",", params)).append(")");
			return sig.toString();
		}
		return prefix + "#" + member.getSimpleName();
	}

}
