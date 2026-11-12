package dev.polyglass.backend.angle;

import dev.polyglass.Probe;
import dev.polyglass.api.GraphicsApi;
import dev.polyglass.backend.AbstractBackend;

/**
 * Compatibility tier: OpenGL ES 3.1 through ANGLE.
 *
 * <p>ANGLE translates GL ES to a native API — Direct3D 11 on Windows, Metal on
 * macOS, Vulkan or GL on Linux. That makes it a single integration that covers
 * the machines the native backends miss: Windows VMs, RDP sessions, old Intel
 * iGPUs without Vulkan drivers, and macOS (where Apple caps desktop GL at 4.1).
 *
 * <p>Two things it is not:
 * <ul>
 *   <li>It is <b>GL ES</b>, not desktop GL. Shaders and state for this tier must
 *       stay within ES 3.1; Sodium and Iris will not run on it.</li>
 *   <li>It is not a substitute for the real backends. Expect a translation
 *       layer's overhead and a smaller feature set.</li>
 * </ul>
 *
 * <p>Detection is deliberately strict: Polyglass requires its own ANGLE natives
 * ({@code libEGL} + {@code libGLESv2}) rather than whatever system EGL happens
 * to be installed. On Linux {@code libEGL.so.1} is usually Mesa, and creating a
 * context against it would silently not be ANGLE at all.
 *
 * <p>Status: backend registration and probing are implemented; device creation
 * is milestone M3.5 (see ROADMAP.md).
 */
public final class AngleBackend extends AbstractBackend {

    public static final String ID = "angle";

    /** ANGLE's EGL entry point library, without platform prefix/suffix. */
    public static final String EGL_LIBRARY = "EGL";

    /** ANGLE's GL ES implementation library, without platform prefix/suffix. */
    public static final String GLES_LIBRARY = "GLESv2";

    public AngleBackend() {
        // After the native APIs, before raw desktop GL.
        super(ID, GraphicsApi.GLES, 50);
    }

    @Override
    public boolean isAvailable() {
        boolean bindings = Probe.hasClass("org.lwjgl.egl.EGL")
                && Probe.hasClass("org.lwjgl.opengles.GLES30");
        return bindings
                && Probe.hasLibraryOnPath(EGL_LIBRARY)
                && Probe.hasLibraryOnPath(GLES_LIBRARY);
    }

    @Override
    public String unavailableReason() {
        if (!Probe.hasClass("org.lwjgl.egl.EGL") || !Probe.hasClass("org.lwjgl.opengles.GLES30")) {
            return "LWJGL EGL/GLES bindings are not on the classpath";
        }
        if (!Probe.hasLibraryOnPath(EGL_LIBRARY) || !Probe.hasLibraryOnPath(GLES_LIBRARY)) {
            return "ANGLE natives (libEGL + libGLESv2) are not on java.library.path";
        }
        return null;
    }
}
