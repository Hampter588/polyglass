package dev.polyglass.backend.d3d12;

import dev.polyglass.Probe;
import dev.polyglass.api.GraphicsApi;
import dev.polyglass.backend.AbstractBackend;

/**
 * Windows-only backend.
 *
 * <p>Bindings: there are no maintained LWJGL Direct3D 12 bindings, so this
 * backend talks to a small native shim ({@code polyglass_d3d12}) through the
 * Foreign Function & Memory API (final in Java 22, so no JNI glue needed).
 * See native/README.md for the C ABI contract.
 *
 * <p>Why bother when Vulkan works on Windows? Because on some drivers — and on
 * handhelds with vendor-specific compositors — D3D12 is the better-behaved
 * path, and it gives Windows users a fallback that does not depend on the
 * Vulkan loader.
 */
public final class D3D12Backend extends AbstractBackend {

    public static final String NATIVE_LIBRARY = "polyglass_d3d12";

    public D3D12Backend() {
        super("d3d12", GraphicsApi.D3D12, 20);
    }

    @Override
    public boolean isAvailable() {
        return Probe.isWindows() && Probe.hasNativeLibrary(NATIVE_LIBRARY);
    }

    @Override
    public String unavailableReason() {
        if (!Probe.isWindows()) {
            return "Direct3D 12 is Windows-only";
        }
        return "native shim '" + NATIVE_LIBRARY + "' is not loadable";
    }
}
