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
 *
 * <p>This class also reads vanilla's {@code preferredGraphicsBackend} straight
 * out of {@code options.txt}. Deliberately no {@code Options} import: the mixin
 * may run before the game has finished constructing it, and a raw line read has
 * no ordering requirements.</p>
 */
public final class PolyglassConfig {

    private static final String FILE_NAME = "polyglass.json";
    private static final String VANILLA_KEY = "preferredGraphicsBackend";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile PolyglassConfig instance;

    /** Explicit backend id, or {@code auto}. */
    private String backend = BackendPlan.AUTO;

    /** Preference order for {@code auto}. Null means "use the platform default". */
    private List<String> order;

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

    public boolean debug() {
        return debug;
    }

    public void setBackend(String id) {
        this.backend = id == null || id.isBlank() ? BackendPlan.AUTO : id;
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
     * Vanilla's own Graphics API setting, as written in {@code options.txt}:
     * {@code "default"}, {@code "opengl"} or {@code "vulkan"}.
     */
    public static String vanillaPreference() {
        Path file = FabricLoader.getInstance().getGameDir().resolve("options.txt");
        if (!Files.isRegularFile(file)) {
            return "default";
        }
        try {
            return parseVanillaPreference(Files.readAllLines(file, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            return "default";
        }
    }

    /** Pure parser so the format can be tested without a game directory. */
    static String parseVanillaPreference(List<String> optionsLines) {
        for (String line : optionsLines) {
            if (line == null || !line.startsWith(VANILLA_KEY + ":")) {
                continue;
            }
            String value = line.substring(VANILLA_KEY.length() + 1).trim();
            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            return switch (BackendPlan.normalize(value)) {
                case "opengl" -> "opengl";
                case "vulkan" -> "vulkan";
                default -> "default";
            };
        }
        return "default";
    }

    /**
     * The default order follows vanilla's Graphics API setting when the user has
     * not written an explicit {@code order}. That keeps the vanilla dropdown
     * meaningful: Polyglass extends it rather than ignoring it.
     */
    public static List<String> defaultOrder() {
        boolean preferOpenGl = "opengl".equals(vanillaPreference());
        if (Probe.isWindows()) {
            return preferOpenGl
                    ? List.of("opengl", "vulkan", "dx12")
                    : List.of("vulkan", "dx12", "opengl");
        }
        return preferOpenGl ? List.of("opengl", "vulkan") : List.of("vulkan", "opengl");
    }
}
