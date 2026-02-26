package org.alexmond.refinej.engine.spoon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.alexmond.refinej.core.domain.Reference;
import org.alexmond.refinej.core.domain.Symbol;
import org.alexmond.refinej.core.domain.UsageKind;
import spoon.reflect.code.CtAnnotationFieldAccess;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtImport;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;

/**
 * Spoon scanner that extracts {@link Reference} instances from a {@code CtModel}.
 *
 * <p>
 * Handles imports, new-instance calls, method calls, extends/implements, annotations,
 * type references, and field accesses. Deduplicates by (filePath, line, column, symbol).
 */
class ReferenceExtractor extends CtScanner {

	private final Map<String, Symbol> symbolIndex;

	private final AtomicLong idSequence;

	private final List<Reference> references = new ArrayList<>();

	private final Set<String> seen = new HashSet<>();

	ReferenceExtractor(Map<String, Symbol> symbolIndex, AtomicLong idSequence) {
		this.symbolIndex = symbolIndex;
		this.idSequence = idSequence;
	}

	List<Reference> getReferences() {
		return List.copyOf(this.references);
	}

	@Override
	public void visitCtImport(CtImport ctImport) {
		String ref = (ctImport.getReference() != null) ? ctImport.getReference().toString() : null;
		if (ref != null) {
			Symbol sym = this.symbolIndex.get(ref);
			if (sym != null) {
				addRef(sym, ctImport.getPosition(), UsageKind.IMPORT);
			}
		}
		super.visitCtImport(ctImport);
	}

	@Override
	public <T> void visitCtClass(CtClass<T> cls) {
		if (cls.getSuperclass() != null) {
			addTypeRef(cls.getSuperclass(), UsageKind.EXTENDS);
		}
		cls.getSuperInterfaces().forEach((i) -> addTypeRef(i, UsageKind.IMPLEMENTS));
		super.visitCtClass(cls);
	}

	@Override
	public <T> void visitCtInterface(CtInterface<T> iface) {
		iface.getSuperInterfaces().forEach((i) -> addTypeRef(i, UsageKind.EXTENDS));
		super.visitCtInterface(iface);
	}

	@Override
	public <A extends java.lang.annotation.Annotation> void visitCtAnnotation(CtAnnotation<A> annotation) {
		if (annotation.getAnnotationType() != null) {
			addTypeRef(annotation.getAnnotationType(), UsageKind.ANNOTATION);
		}
		super.visitCtAnnotation(annotation);
	}

	@Override
	public <T> void visitCtConstructorCall(CtConstructorCall<T> call) {
		if (call.getType() != null) {
			addTypeRef(call.getType(), UsageKind.NEW_INSTANCE);
		}
		super.visitCtConstructorCall(call);
	}

	@Override
	public <T> void visitCtInvocation(CtInvocation<T> invocation) {
		CtExecutableReference<?> exec = invocation.getExecutable();
		if (exec != null && exec.getDeclaringType() != null) {
			String fqn = exec.getDeclaringType().getQualifiedName() + "#" + exec.getSimpleName() + "(";
			// Match by prefix (ignoring parameter types for now)
			this.symbolIndex.entrySet()
				.stream()
				.filter((e) -> e.getKey().startsWith(fqn))
				.findFirst()
				.ifPresent((e) -> addRef(e.getValue(), invocation.getPosition(), UsageKind.METHOD_CALL));
		}
		super.visitCtInvocation(invocation);
	}

	@Override
	public <T> void visitCtFieldRead(CtFieldRead<T> fieldRead) {
		addFieldRef(fieldRead.getVariable(), fieldRead.getPosition(), UsageKind.FIELD_ACCESS);
		super.visitCtFieldRead(fieldRead);
	}

	@Override
	public <T> void visitCtFieldWrite(CtFieldWrite<T> fieldWrite) {
		addFieldRef(fieldWrite.getVariable(), fieldWrite.getPosition(), UsageKind.FIELD_ACCESS);
		super.visitCtFieldWrite(fieldWrite);
	}

	@Override
	public <T> void visitCtAnnotationFieldAccess(CtAnnotationFieldAccess<T> fieldAccess) {
		addFieldRef(fieldAccess.getVariable(), fieldAccess.getPosition(), UsageKind.FIELD_ACCESS);
		super.visitCtAnnotationFieldAccess(fieldAccess);
	}

	@Override
	public <T> void visitCtTypeReference(CtTypeReference<T> ref) {
		addTypeRef(ref, UsageKind.TYPE_REFERENCE);
		super.visitCtTypeReference(ref);
	}

	private void addTypeRef(CtTypeReference<?> ref, UsageKind kind) {
		if (ref == null) {
			return;
		}
		Symbol sym = this.symbolIndex.get(ref.getQualifiedName());
		if (sym != null) {
			addRef(sym, ref.getPosition(), kind);
		}
	}

	private void addFieldRef(CtFieldReference<?> ref, spoon.reflect.cu.SourcePosition pos, UsageKind kind) {
		if (ref == null || ref.getDeclaringType() == null) {
			return;
		}
		String fqn = ref.getDeclaringType().getQualifiedName() + "#" + ref.getSimpleName();
		Symbol sym = this.symbolIndex.get(fqn);
		if (sym != null) {
			addRef(sym, pos, kind);
		}
	}

	private void addRef(Symbol sym, spoon.reflect.cu.SourcePosition pos, UsageKind kind) {
		if (pos == null || !pos.isValidPosition()) {
			return;
		}
		String filePath = (pos.getFile() != null) ? pos.getFile().getAbsolutePath() : null;
		int line = pos.getLine();
		int col = pos.getColumn();
		String dedupeKey = sym.qualifiedName() + "|" + filePath + "|" + line + "|" + col + "|" + kind;
		if (this.seen.add(dedupeKey)) {
			this.references.add(new Reference(this.idSequence.getAndIncrement(), sym, filePath, line, col, kind));
		}
	}

}
