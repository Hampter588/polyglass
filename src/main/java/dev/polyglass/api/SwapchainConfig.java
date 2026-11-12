package dev.polyglass.api;

/**
 * Presentation surface.
 *
 * @param nativeWindowHandle platform handle to the GLFW window (HWND, NSView, X11 window)
 * @param width              backbuffer width in pixels
 * @param height             backbuffer height in pixels
 * @param vsync              request vsync / FIFO present mode
 */
public record SwapchainConfig(long nativeWindowHandle, int width, int height, boolean vsync) {

    public SwapchainConfig {
        if (nativeWindowHandle == 0L) {
            throw new IllegalArgumentException("nativeWindowHandle must not be 0");
        }
    }
}
