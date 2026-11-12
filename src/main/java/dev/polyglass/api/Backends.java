package dev.polyglass.api;

import dev.polyglass.Polyglass;
import dev.polyglass.backend.angle.AngleBackend;
import dev.polyglass.backend.d3d12.D3D12Backend;
import dev.polyglass.backend.metal.MetalBackend;
import dev.polyglass.backend.null_backend.NullBackend;
import dev.polyglass.backend.opengl.OpenGlBackend;
import dev.polyglass.backend.vulkan.VulkanBackend;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Backend registry and selection policy.
 *
 * <p>Selection is: explicit choice → configured fallback chain → priority order.
 * Anything that cannot run on this machine is skipped with a logged reason.
 */
public final class Backends {

    /** Selecting this id means "OpenGL + mesh-shader render path". */
    public static final String MESH_ID = "mesh";

    private static final Map<String, GraphicsBackend> REGISTRY = new LinkedHashMap<>();

    static {
        register(new VulkanBackend());
        register(new D3D12Backend());
        register(new MetalBackend());
        register(new AngleBackend());
        register(new OpenGlBackend());
        // Never auto-selected in practice: OpenGL is always available and outranks it.
        register(new NullBackend());
    }

    private Backends() {
    }

    public static synchronized void register(GraphicsBackend backend) {
        GraphicsBackend previous = REGISTRY.put(backend.id(), backend);
        if (previous != null) {
            Polyglass.LOGGER.warn("Backend id '{}' was replaced by {}", backend.id(), backend.getClass().getName());
        }
    }

    public static Optional<GraphicsBackend> byId(String id) {
        return Optional.ofNullable(REGISTRY.get(id.toLowerCase(Locale.ROOT)));
    }

    /** All backends, most preferred first. */
    public static List<GraphicsBackend> byPriority() {
        return REGISTRY.values().stream()
                .sorted(Comparator.comparingInt(GraphicsBackend::priority))
                .toList();
    }

    /**
     * @param requested  config value ({@code auto}, an id, or {@code mesh})
     * @param fallback   ordered ids tried after an explicit choice fails
     * @throws BackendException if nothing is usable
     */
    public static Selection select(String requested, List<String> fallback) {
        Selection chosen = tryResolve(requested);
        if (chosen != null) {
            return chosen;
        }

        for (String id : fallback) {
            chosen = tryResolve(id);
            if (chosen != null) {
                return chosen;
            }
        }

        for (GraphicsBackend backend : byPriority()) {
            if (backend.isAvailable()) {
                Polyglass.LOGGER.warn("No requested backend was usable; falling back to {}", backend.id());
                return new Selection(backend, RenderPath.STANDARD, "auto");
            }
        }

        throw new BackendException("No usable graphics backend on this machine. "
                + "Every backend (vulkan, d3d12, metal, angle, opengl, null) reported itself unavailable.");
    }

    /**
     * Resolves one id, or returns {@code null} if it is unknown or unavailable.
     * {@code mesh} is an alias for OpenGL + {@link RenderPath#MESH}.
     */
    private static Selection tryResolve(String id) {
        if (id == null || id.isBlank() || id.equalsIgnoreCase("auto")) {
            return null;
        }

        String requested = id.trim().toLowerCase(Locale.ROOT);
        RenderPath path = requested.equals(MESH_ID) ? RenderPath.MESH : RenderPath.STANDARD;
        String backendId = path == RenderPath.MESH ? "opengl" : requested;

        GraphicsBackend backend = REGISTRY.get(backendId);
        if (backend == null) {
            Polyglass.LOGGER.warn("Unknown backend '{}' in polyglass.json", requested);
            return null;
        }
        if (!backend.isAvailable()) {
            Polyglass.LOGGER.warn("Backend '{}' unavailable: {}", backendId, backend.unavailableReason());
            return null;
        }
        return new Selection(backend, path, requested);
    }
}
