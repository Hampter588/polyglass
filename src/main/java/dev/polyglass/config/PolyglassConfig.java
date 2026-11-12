package dev.polyglass.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.polyglass.Probe;
import dev.polyglass.backend.BackendPlan;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code config/polyglass.json}.
 *
 * <p>Read lazily from the mixin, which runs before {@code ClientModInitializer},
 * so nothing here may assume the client is initialised.</p>
 */
public final class PolyglassConfig {

    private static final String FILE_NAME = "polyglass.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile PolyglassConfig instance;

    /** Explicit backend id, or {@code auto}. */
    private String backend = BackendPlan.AUTO;

    /** Preference order for {@code auto}. Null means "use the platform default". */
    private List<String> order;

    /** Opt in to the CPU rasteriser. It is a raster core, not a display driver. */
    private boolean enableSoftware = false;

    /** Extra logging of the resolved plan and the running backend. */
    private boolean debug = false;

    private PolyglassConfig() {
    }

    public static PolyglassConfig get() {
        PolyglassConfig local = instance;
        if (local != null) {
            return local;
        }
        synchronized (PolyglassConfig.class) {
            if (instance == null) {
                instance = load();
            }
            return instance;
        }
    }

    private static PolyglassConfig load() {
        Path file = path();
        if (!Files.isRegularFile(file)) {
            PolyglassConfig fresh = new PolyglassConfig();
            fresh.save();
            return fresh;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            PolyglassConfig loaded = GSON.fromJson(reader, PolyglassConfig.class);
            return loaded == null ? new PolyglassConfig() : loaded;
        } catch (IOException | RuntimeException e) {
            System.err.println("[polyglass] could not read " + file + ": " + e);
            return new PolyglassConfig();
        }
    }

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public String backend() {
        return backend == null || backend.isBlank() ? BackendPlan.AUTO : backend;
    }

    /**
     * The configured preference order, normalised and de-duplicated. Unknown ids
     * are kept: a backend may become available later after a mod is installed,
     * and dropping it here would silently forget the user's preference.
     */
    public List<String> order() {
        if (order == null || order.isEmpty()) {
            return defaultOrder();
        }
        List<String> normalised = new ArrayList<>(order.size());
        for (String entry : order) {
            String id = BackendPlan.normalize(entry);
            if (!id.isEmpty() && !normalised.contains(id)) {
                normalised.add(id);
            }
        }
        return normalised;
    }

    public boolean softwareEnabled() {
        return enableSoftware;
    }

    public boolean debug() {
        return debug;
    }

    public void setBackend(String id) {
        this.backend = id == null || id.isBlank() ? BackendPlan.AUTO : id;
        save();
    }

    public void setSoftwareEnabled(boolean enabled) {
        this.enableSoftware = enabled;
        save();
    }

    public void setDebug(boolean enabled) {
        this.debug = enabled;
        save();
    }

    /** Back to the shipped defaults, keeping the debug flag. */
    public void reset() {
        this.backend = BackendPlan.AUTO;
        this.order = null;
        this.enableSoftware = false;
        save();
    }

    public void save() {
        Path file = path();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("[polyglass] could not write " + file + ": " + e);
        }
    }

    /**
     * Vulkan first everywhere it exists. DX12 only makes sense on Windows and
     * only when DirectXMod is installed, but listing it costs nothing: an
     * unavailable id is dropped before the list reaches the game.
     */
    public static List<String> defaultOrder() {
        if (Probe.isWindows()) {
            return List.of("vulkan", "dx12", "opengl", "software");
        }
        return List.of("vulkan", "opengl", "software");
    }
}
