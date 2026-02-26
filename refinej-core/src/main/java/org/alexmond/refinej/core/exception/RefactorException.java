package org.alexmond.refinej.core.exception;

/**
 * Base exception for all RefineJ refactoring errors.
 * Subclass for specific failure modes so the CLI can present tailored messages.
 */
public class RefactorException extends RuntimeException {

    public RefactorException(String message) {
        super(message);
    }

    public RefactorException(String message, Throwable cause) {
        super(message, cause);
    }

    // ---------------------------------------------------------------------------
    // Subclasses
    // ---------------------------------------------------------------------------

    /** The requested symbol could not be found in the index. */
    public static class SymbolNotFoundException extends RefactorException {
        public SymbolNotFoundException(String qualifiedName) {
            super("Symbol not found in index: " + qualifiedName
                  + ". Run 'refinej index' first.");
        }
    }

    /** The project has not been indexed yet. */
    public static class NotIndexedException extends RefactorException {
        public NotIndexedException() {
            super("Project is not indexed. Run 'refinej index <path>' first.");
        }
    }

    /** The refactoring would produce a name clash or other unresolvable conflict. */
    public static class ConflictException extends RefactorException {
        public ConflictException(String message) {
            super(message);
        }
    }

    /** A file could not be read or written during apply. */
    public static class FileOperationException extends RefactorException {
        public FileOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
