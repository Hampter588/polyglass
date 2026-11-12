package dev.polyglass.api;

/** A compiled graphics pipeline (VS/FS + fixed state, or a mesh-shader pipeline). */
public interface Pipeline extends AutoCloseable {

    @Override
    void close();
}
