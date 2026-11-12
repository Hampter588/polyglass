package dev.polyglass.api;

/**
 * The one object the rest of the renderer talks to. It is a thin, API-neutral
 * façade over a Vulkan device / D3D12 device / MTLDevice / GL context.
 *
 * <p>Threading: all methods are render-thread only unless stated otherwise.
 */
public interface Device extends AutoCloseable {

    GraphicsApi api();

    /** Adapter name, e.g. {@code NVIDIA GeForce RTX 5080}. */
    String name();

    Capabilities capabilities();

    Swapchain createSwapchain(SwapchainConfig config);

    /** Opens a command list for the frame currently being built. */
    CommandList beginFrame();

    void submit(CommandList commands);

    /** Presents the frame and advances the swapchain. */
    void endFrame();

    Buffer createBuffer(BufferDesc desc);

    Texture createTexture(TextureDesc desc);

    Pipeline createPipeline(PipelineDesc desc);

    /** Blocks until the GPU is idle. Called on resize, shutdown and screenshots. */
    void waitIdle();

    @Override
    void close();
}
