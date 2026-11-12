package dev.polyglass.api;

/** A GPU buffer. Backed by VkBuffer, ID3D12Resource, MTLBuffer or a GL buffer object. */
public interface Buffer extends AutoCloseable {

    long size();

    /** Maps persistently coherent memory, or returns 0 if the backend has no direct mapping. */
    long map();

    void unmap();

    @Override
    void close();
}
