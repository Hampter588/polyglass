package dev.polyglass.api;

/** Presentation surface for one window. Recreated on resize. */
public interface Swapchain extends AutoCloseable {

    int width();

    int height();

    /** Acquires the next backbuffer image. Blocks at most one frame. */
    void acquire();

    /** Presents the acquired image. */
    void present();

    /** Recreates the swapchain for a new backbuffer size. */
    void resize(int width, int height);

    @Override
    void close();
}
