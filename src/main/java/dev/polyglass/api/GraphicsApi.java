package dev.polyglass.api;

/** The low-level graphics API that a {@link GraphicsBackend} drives. */
public enum GraphicsApi {
    OPENGL("OpenGL"),
    GLES("OpenGL ES"),
    VULKAN("Vulkan"),
    D3D12("Direct3D 12"),
    METAL("Metal"),
    /** No GPU at all. Only the headless {@code null} backend reports this. */
    NULL("None");

    private final String displayName;

    GraphicsApi(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
