package dev.polyglass.api;

/** A GPU texture + view. Backed by VkImage, ID3D12Resource, MTLTexture or a GL texture. */
public interface Texture extends AutoCloseable {

    int width();

    int height();

    int mipLevels();

    @Override
    void close();
}
