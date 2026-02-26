package org.alexmond.refinej.core.domain;

/**
 * Immutable representation of an indexed Java symbol (class, method, field, package).
 * This is the in-memory domain object; the JPA entity is {@code SymbolEntity}.
 */
public record Symbol(Long id, SymbolKind kind, String simpleName, String qualifiedName, String filePath, int lineStart,
		int lineEnd, String typeFqn // return type for methods, declared type for fields;
									// null for classes/packages
) {
}
