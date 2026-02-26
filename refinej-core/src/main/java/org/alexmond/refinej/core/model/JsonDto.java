package org.alexmond.refinej.core.model;

import java.util.List;

/**
 * JSON-serialisable DTOs for {@code --json} output.
 * All types are records — immutable and directly serialisable by Jackson.
 */
public final class JsonDto {

    private JsonDto() {}

    // ---------------------------------------------------------------------------
    // Status / version
    // ---------------------------------------------------------------------------

    public record StatusResponse(
            String status,
            String engine,
            String indexPath,
            long symbolCount,
            long referenceCount,
            String version
    ) {}

    // ---------------------------------------------------------------------------
    // Indexing
    // ---------------------------------------------------------------------------

    public record IndexResponse(
            String status,
            String engine,
            long symbolCount,
            long referenceCount,
            long durationMs
    ) {}

    // ---------------------------------------------------------------------------
    // Query
    // ---------------------------------------------------------------------------

    public record SymbolResponse(
            String status,
            String qualifiedName,
            String kind,
            String filePath,
            int lineStart,
            int lineEnd
    ) {}

    public record RefsResponse(
            String status,
            String qualifiedName,
            long count,
            List<ReferenceItem> references
    ) {}

    public record ReferenceItem(
            String filePath,
            int line,
            int column,
            String usageKind
    ) {}

    // ---------------------------------------------------------------------------
    // Refactoring
    // ---------------------------------------------------------------------------

    public record RefactorResponse(
            String status,
            String operation,
            String oldQualifiedName,
            String newQualifiedName,
            int filesAffected,
            PreviewSummary preview,
            List<ConflictItem> conflicts
    ) {}

    public record PreviewSummary(
            String diff,
            String summary   // e.g. "3 files changed, 12 insertions(+), 4 deletions(-)"
    ) {}

    public record ConflictItem(
            String description,
            String filePath,
            int line
    ) {}

    // ---------------------------------------------------------------------------
    // Error
    // ---------------------------------------------------------------------------

    public record ErrorResponse(
            String status,
            String error,
            String message
    ) {
        public static ErrorResponse of(String message) {
            return new ErrorResponse("error", "RefactorException", message);
        }
    }
}
