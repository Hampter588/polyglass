package dev.polyglass.api;

/**
 * A pluggable graphics implementation. One backend per API; the renderer above
 * it never sees API-specific types.
 *
 * <p>Implementations are registered in {@link Backends} and are expected to be
 * cheap to construct — probing (adapter enumeration, extension queries) should
 * happen in {@link #isAvailable()} or lazily in {@link #createDevice}.
 */
public interface GraphicsBackend {

    /** Stable config id: {@code opengl}, {@code vulkan}, {@code d3d12}, {@code metal}. */
    String id();

    GraphicsApi api();

    /** Lower number is tried first when the user asks for {@code auto}. */
    int priority();

    /** {@code true} if this backend can start on this machine right now. */
    boolean isAvailable();

    /** Human-readable reason {@link #isAvailable()} returned {@code false}; {@code null} otherwise. */
    default String unavailableReason() {
        return null;
    }

    /**
     * Creates the device. The caller owns the result and must close it.
     *
     * @throws BackendException if initialisation fails; the caller will fall back
     *                          to the next backend in the chain.
     */
    Device createDevice(DeviceConfig config) throws BackendException;

    /** Releases backend-global resources (loaded natives, cached adapters, ...). */
    default void close() {
    }
}
