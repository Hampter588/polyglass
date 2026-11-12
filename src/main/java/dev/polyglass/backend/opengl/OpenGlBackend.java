package dev.polyglass.backend.opengl;

import dev.polyglass.api.GraphicsApi;
import dev.polyglass.backend.AbstractBackend;

/**
 * Reference backend. The game already owns a GL context, so this one is always
 * available and must stay at feature parity — every other backend is validated
 * by rendering the same frame through this one.
 *
 * <p>Bindings: LWJGL, which Minecraft already ships. Nothing extra to depend on.
 */
public final class OpenGlBackend extends AbstractBackend {

    public OpenGlBackend() {
        super("opengl", GraphicsApi.OPENGL, 100);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
