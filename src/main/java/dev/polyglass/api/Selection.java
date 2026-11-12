package dev.polyglass.api;

/**
 * The outcome of backend selection.
 *
 * @param backend   the backend that will be used
 * @param path      render path requested alongside it
 * @param requested the raw config value the user asked for ({@code auto}, {@code vulkan}, {@code mesh}, ...)
 */
public record Selection(GraphicsBackend backend, RenderPath path, String requested) {
}
