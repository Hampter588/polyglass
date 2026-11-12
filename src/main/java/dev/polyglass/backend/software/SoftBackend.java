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
 * <p>Status: the raster core ({@link SoftRaster}) and a complete offscreen
 * frame ({@link SoftFrameDump}) work and are tested, but {@link #createDevice}
 * still refuses. That is deliberate and load-bearing — Minecraft treats
 * {@code BackendCreationException} as "try the next candidate", so the entry can
 * sit in the preference list without ever booting into a half-bound device.</p>
 *
 * <p>It cannot be removed without a real {@code GpuDeviceBackend}: textures,
 * buffers, samplers, render passes and pipeline compilation all have to exist
 * before the frontend will accept the device. Until then the software tier is a
 * rasteriser that renders to memory, not a display driver. See ROADMAP S2/S3.</p>
 */
public final class SoftBackend implements GpuBackend {

    public static final String NAME = "polyglass-software";

    public static final String UNAVAILABLE =
            "polyglass: software backend has no GpuDevice binding yet "
                    + "(raster core only; see the CPU raster test in the picker)";

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
        throw new BackendCreationException(UNAVAILABLE, BackendCreationException.Reason.OTHER);
    }
}
