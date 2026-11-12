package dev.polyglass.api;

/**
 * Thrown when a backend cannot initialise or a device operation fails.
 * Unchecked on purpose: the renderer has no meaningful recovery for a GPU
 * failure other than falling back to another backend or hard-failing.
 */
public class BackendException extends RuntimeException {

    public BackendException(String message) {
        super(message);
    }

    public BackendException(String message, Throwable cause) {
        super(message, cause);
    }
}
