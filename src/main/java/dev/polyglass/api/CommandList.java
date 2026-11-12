package dev.polyglass.api;

/**
 * Records GPU work. One per frame in flight; recording must not cross frames.
 */
public interface CommandList extends AutoCloseable {

    void begin();

    void end();

    void setPipeline(Pipeline pipeline);

    void setVertexBuffer(Buffer buffer, long offset);

    void setIndexBuffer(Buffer buffer, long offset);

    void draw(int vertexCount, int instanceCount, int firstVertex, int firstInstance);

    void drawIndexed(int indexCount, int instanceCount, int firstIndex, int vertexOffset, int firstInstance);

    /** Returns the backend-native command buffer handle. Used only by backend-internal code. */
    long nativeHandle();

    @Override
    void close();
}
