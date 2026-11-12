package dev.polyglass.backend.null_backend;

import dev.polyglass.api.Device;
import dev.polyglass.api.DeviceConfig;
import dev.polyglass.api.GraphicsApi;
import dev.polyglass.backend.AbstractBackend;

/**
 * Headless backend. Always available, never chosen automatically.
 *
 * <p>Use it for CI, screenshot harnesses, dedicated servers, and for proving
 * that the renderer's command submission path is sound before blaming a driver.
 */
public final class NullBackend extends AbstractBackend {

    public static final String ID = "null";

    public NullBackend() {
        // Lowest priority: even when it is in a fallback chain it goes last.
        super(ID, GraphicsApi.NULL, 1_000);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Device createDevice(DeviceConfig config) {
        return new NullDevice(config);
    }
}
