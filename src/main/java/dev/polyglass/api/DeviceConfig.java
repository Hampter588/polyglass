package dev.polyglass.api;

/**
 * Device creation parameters.
 *
 * @param width      initial framebuffer width in pixels
 * @param height     initial framebuffer height in pixels
 * @param vsync      request a vsynced swapchain
 * @param debug      enable backend debug naming / extra logging
 * @param validation enable API validation layers where available
 */
public record DeviceConfig(int width, int height, boolean vsync, boolean debug, boolean validation) {

    public DeviceConfig {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("framebuffer size must be positive: " + width + "x" + height);
        }
    }
}
