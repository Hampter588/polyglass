package dev.polyglass.backend.software;

import com.mojang.blaze3d.GLFWErrorCapture;
import com.mojang.blaze3d.shaders.GpuDebugOptions;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.BackendCreationException;
import com.mojang.blaze3d.systems.GpuBackend;
import com.mojang.blaze3d.systems.GpuDevice;

/**
 * CPU rasteriser backend. No GPU, no driver, no window API.
 *
 * <p>Status: the raster core ({@link SoftRaster}) is implemented and tested,
 * but {@link #createDevice} still refuses. That is deliberate — it makes the
 * entry safe to leave in the preference list, because Minecraft treats
 * {@code BackendCreationException} as "try the next candidate" and falls
 * through to Vulkan/OpenGL instead of booting into a half-bound device.</p>
 *
 * <p>Remove the throw only when the device can actually produce a frame.</p>
 */
public final class SoftBackend implements GpuBackend {

    public static final String NAME = "polyglass-software";

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * No window hints: the software backend does not ask GLFW for a context.
     * The window is still created by the game for input and is simply never
     * presented to.
     */
    @Override
    public void setWindowHints() {
    }

    @Override
    public void handleWindowCreationErrors(GLFWErrorCapture.Error error) throws BackendCreationException {
        throw new BackendCreationException(
                "polyglass: software backend does not create a window context",
                BackendCreationException.Reason.GLFW_ERROR);
    }

    @Override
    public GpuDevice createDevice(long window, ShaderSource defaultShaderSource,
                                  GpuDebugOptions debugOptions, Runnable criticalShaderLoader)
            throws BackendCreationException {
        throw new BackendCreationException(
                "polyglass: software backend is not complete yet (raster core ready, GpuDevice binding is milestone S2)",
                BackendCreationException.Reason.OTHER);
    }
}
