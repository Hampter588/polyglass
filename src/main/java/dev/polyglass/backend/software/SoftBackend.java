package dev.polyglass.backend.software;

import com.mojang.renderpearl.api.device.BackendCreationException;
import com.mojang.renderpearl.api.device.GpuBackend;
import com.mojang.renderpearl.api.device.GpuDebugOptions;
import com.mojang.renderpearl.api.device.GpuDevice;

/**
 * CPU rasteriser backend. No GPU, no driver, no platform window API.
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

    /** Nothing to load: the backend is pure Java plus the JDK's own buffers. */
    @Override
    public void loadLibrary() throws BackendCreationException {
    }

    @Override
    public void unloadLibrary() {
    }

    /**
     * No native window. Returns 0, which the game only turns into an input
     * handle once {@link #createDevice} has succeeded — and it never does yet.
     */
    @Override
    public long createWindow(String title, int width, int height, long shareWith) {
        return 0L;
    }

    @Override
    public GpuDevice createDevice(GpuDebugOptions options) throws BackendCreationException {
        throw new BackendCreationException(
                "polyglass: software backend is not complete yet (raster core ready, GpuDevice binding is milestone S2)",
                BackendCreationException.Reason.OTHER);
    }
}
