package dev.polyglass.backend.metal;

import dev.polyglass.Probe;
import dev.polyglass.api.GraphicsApi;
import dev.polyglass.backend.AbstractBackend;

/**
 * macOS backend.
 *
 * <p>Bindings: no LWJGL Metal bindings exist. Two viable routes:
 * <ol>
 *   <li>an Objective-C/C shim ({@code polyglass_metal}) driven over FFM — fastest,
 *       most control, but a second pipeline compiler (GLSL → SPIR-V → MSL);</li>
 *   <li>MoltenVK and reuse the Vulkan backend, accepting the translation layer.</li>
 * </ol>
 * The shim route is the default plan; MoltenVK stays as a debugging comparison.
 * MoltenVK is cheaper than it sounds: LWJGL's Vulkan module already ships it, and
 * it compiles SPIR-V to MSL for you, so the Vulkan backend can be reused on macOS
 * without writing a Metal pipeline compiler.
 */
public final class MetalBackend extends AbstractBackend {

    public static final String NATIVE_LIBRARY = "polyglass_metal";

    public MetalBackend() {
        super("metal", GraphicsApi.METAL, 5);
    }

    @Override
    public boolean isAvailable() {
        return Probe.isMacOs() && Probe.hasNativeLibrary(NATIVE_LIBRARY);
    }

    @Override
    public String unavailableReason() {
        if (!Probe.isMacOs()) {
            return "Metal is macOS-only";
        }
        return "native shim '" + NATIVE_LIBRARY + "' is not loadable";
    }
}
