package dev.polyglass.api;

/**
 * How geometry reaches the GPU. Deliberately separate from {@link GraphicsApi}:
 * a render path describes <em>what the renderer does</em>, not <em>which API it
 * does it with</em>.
 *
 * <p>{@link #STANDARD} is the classic chunk pipeline: packed vertex/index
 * buffers plus (optionally) indirect multi-draw.
 *
 * <p>{@link #MESH} renders chunk sections with GPU mesh shaders. This is the
 * technique Nvidium uses — {@code GL_NV_mesh_shader} on top of OpenGL, and
 * {@code VK_EXT_mesh_shader} on top of Vulkan. It is an extension of an API,
 * not an API of its own, which is why it is modelled here rather than as a
 * fifth backend.
 */
public enum RenderPath {
    STANDARD,
    MESH
}
