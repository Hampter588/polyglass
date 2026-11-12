package dev.polyglass.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.polyglass.Polyglass;
import dev.polyglass.Probe;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * User-facing backend selection. Written to {@code config/polyglass.json} on
 * first launch; unknown fields are dropped on save.
 */
public final class PolyglassConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "polyglass.json";

    /** {@code auto} | {@code opengl} | {@code vulkan} | {@code d3d12} | {@code metal} | {@code mesh}. */
    public String backend = "auto";

    /** Tried in order when {@link #backend} is unavailable or its device fails to initialise. */
    public List<String> fallback = defaultFallback();

    /** Allow the mesh-shader chunk path (GL_NV_mesh_shader / VK_EXT_mesh_shader). */
    public boolean meshPath = true;

    public boolean debug = false;

    /** Swapchain images the renderer keeps in flight. 2 is the safe default. */
    public int framesInFlight = 2;

    public static PolyglassConfig load(Path configDir) {
        Path file = configDir.resolve(FILE_NAME);
        try {
            if (Files.notExists(file)) {
                PolyglassConfig defaults = new PolyglassConfig();
                Files.createDirectories(configDir);
                Files.writeString(file, GSON.toJson(defaults));
                return defaults;
            }
            PolyglassConfig loaded = GSON.fromJson(Files.readString(file), PolyglassConfig.class);
            return loaded == null ? new PolyglassConfig() : loaded;
        } catch (Exception e) {
            Polyglass.LOGGER.warn("Could not read {}, falling back to defaults", file, e);
            return new PolyglassConfig();
        }
    }

    public String backend() {
        return backend == null || backend.isBlank() ? "auto" : backend.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public List<String> fallback() {
        return fallback == null || fallback.isEmpty() ? defaultFallback() : List.copyOf(fallback);
    }

    /**
     * Platform-aware default chain. Metal is listed first on macOS because it is
     * the only API with a first-party driver there; Vulkan is a thin layer over
     * Metal (MoltenVK) and OpenGL is deprecated.
     */
    private static List<String> defaultFallback() {
        if (Probe.isMacOs()) {
            return List.of("metal", "vulkan", "angle", "opengl");
        }
        if (Probe.isWindows()) {
            return List.of("vulkan", "d3d12", "angle", "opengl");
        }
        return List.of("vulkan", "angle", "opengl");
    }
}
