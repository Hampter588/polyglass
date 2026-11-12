package dev.polyglass.backend.null_backend;

import dev.polyglass.api.Buffer;
import dev.polyglass.api.BufferDesc;
import dev.polyglass.api.Capabilities;
import dev.polyglass.api.CommandList;
import dev.polyglass.api.Device;
import dev.polyglass.api.DeviceConfig;
import dev.polyglass.api.GraphicsApi;
import dev.polyglass.api.Pipeline;
import dev.polyglass.api.PipelineDesc;
import dev.polyglass.api.Swapchain;
import dev.polyglass.api.SwapchainConfig;
import dev.polyglass.api.Texture;
import dev.polyglass.api.TextureDesc;

/**
 * A headless device that accepts every call and draws nothing.
 *
 * <p>This is not a placeholder — it is a working backend. It exists so the
 * renderer can be driven end-to-end with no GPU: CI smoke tests, screenshot
 * comparison harnesses, dedicated servers, and bisecting "is this a renderer
 * bug or a driver bug".
 *
 * <p>It is never picked automatically. Ask for it by name
 * ({@code "backend": "null"}) or put it in a test-only fallback chain.
 */
public final class NullDevice implements Device {

    private static final int FAKE_MAX_TEXTURE_SIZE = 16_384;

    private final DeviceConfig config;
    private final Capabilities capabilities;
    private long frames;
    private boolean closed;

    public NullDevice(DeviceConfig config) {
        this.config = config;
        this.capabilities = new Capabilities(
                GraphicsApi.NULL,
                "Polyglass null device",
                "headless",
                false,  // compute
                false,  // mesh shaders
                false,  // bindless
                false,  // timestamp queries
                FAKE_MAX_TEXTURE_SIZE,
                1);
    }

    public DeviceConfig config() {
        return config;
    }

    /** Frames presented so far. Assert against this in tests. */
    public long frameCount() {
        return frames;
    }

    @Override
    public GraphicsApi api() {
        return GraphicsApi.NULL;
    }

    @Override
    public String name() {
        return capabilities.deviceName();
    }

    @Override
    public Capabilities capabilities() {
        return capabilities;
    }

    @Override
    public Swapchain createSwapchain(SwapchainConfig config) {
        return new NullSwapchain(config.width(), config.height());
    }

    @Override
    public CommandList beginFrame() {
        return new NullCommandList();
    }

    @Override
    public void submit(CommandList commands) {
        // Nothing to submit; the list already recorded its own statistics.
    }

    @Override
    public void endFrame() {
        frames++;
    }

    @Override
    public Buffer createBuffer(BufferDesc desc) {
        return new NullBuffer(desc.size());
    }

    @Override
    public Texture createTexture(TextureDesc desc) {
        return new NullTexture(desc.width(), desc.height(), desc.mipLevels());
    }

    @Override
    public Pipeline createPipeline(PipelineDesc desc) {
        return new NullPipeline();
    }

    @Override
    public void waitIdle() {
        // Already idle.
    }

    @Override
    public void close() {
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    /** Records what would have been drawn, so tests can assert on it. */
    public static final class NullCommandList implements CommandList {

        private long drawCalls;
        private long vertices;
        private boolean recording;

        public long drawCalls() {
            return drawCalls;
        }

        public long vertices() {
            return vertices;
        }

        @Override
        public void begin() {
            recording = true;
        }

        @Override
        public void end() {
            recording = false;
        }

        @Override
        public void setPipeline(Pipeline pipeline) {
        }

        @Override
        public void setVertexBuffer(Buffer buffer, long offset) {
        }

        @Override
        public void setIndexBuffer(Buffer buffer, long offset) {
        }

        @Override
        public void draw(int vertexCount, int instanceCount, int firstVertex, int firstInstance) {
            drawCalls++;
            vertices += (long) vertexCount * instanceCount;
        }

        @Override
        public void drawIndexed(int indexCount, int instanceCount, int firstIndex, int vertexOffset, int firstInstance) {
            drawCalls++;
            vertices += (long) indexCount * instanceCount;
        }

        @Override
        public long nativeHandle() {
            return 0L;
        }

        @Override
        public void close() {
            recording = false;
        }

        public boolean isRecording() {
            return recording;
        }
    }

    public static final class NullSwapchain implements Swapchain {

        private int width;
        private int height;

        NullSwapchain(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public int width() {
            return width;
        }

        @Override
        public int height() {
            return height;
        }

        @Override
        public void acquire() {
        }

        @Override
        public void present() {
        }

        @Override
        public void resize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public void close() {
        }
    }

    public static final class NullBuffer implements Buffer {

        private final long size;

        NullBuffer(long size) {
            this.size = size;
        }

        @Override
        public long size() {
            return size;
        }

        @Override
        public long map() {
            return 0L;
        }

        @Override
        public void unmap() {
        }

        @Override
        public void close() {
        }
    }

    public static final class NullTexture implements Texture {

        private final int width;
        private final int height;
        private final int mipLevels;

        NullTexture(int width, int height, int mipLevels) {
            this.width = width;
            this.height = height;
            this.mipLevels = mipLevels;
        }

        @Override
        public int width() {
            return width;
        }

        @Override
        public int height() {
            return height;
        }

        @Override
        public int mipLevels() {
            return mipLevels;
        }

        @Override
        public void close() {
        }
    }

    public static final class NullPipeline implements Pipeline {

        @Override
        public void close() {
        }
    }
}
