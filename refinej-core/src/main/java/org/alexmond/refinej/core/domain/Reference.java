package org.alexmond.refinej.core.domain;

/**
 * Immutable representation of a single usage of a {@link Symbol} in source code.
 * This is the in-memory domain object; the JPA entity is {@code ReferenceEntity}.
 */
public record Reference(
        Long id,
        Symbol symbol,
        String filePath,
        int line,
        int column,
        UsageKind usageKind
) {}
