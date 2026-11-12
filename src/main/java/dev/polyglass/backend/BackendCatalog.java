package dev.polyglass.backend;

import com.mojang.blaze3d.opengl.GlBackend;
import com.mojang.blaze3d.systems.GpuBackend;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vulkan.VulkanBackend;
import dev.polyglass.Probe;
import dev.polyglass.backend.software.SoftBackend;
import dev.polyglass.config.PolyglassConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the array returned by {@code PreferredGraphicsApi.getBackendsToTry()}.
 *
 * <p>Everything here is <em>additive</em>: instances another mod already put in
 * the array are kept and reused rather than replaced. That matters because
 * DirectXMod injects at the same return point, so whichever mixin runs second
 * sees the other's array. Merging makes the two orderings converge instead of
 * one silently deleting the other's backend.</p>
 *
 * <p>The result of the last {@link #order(GpuBackend[])} call is kept so the
 * picker can show what actually happened at boot. That call happens before mod
 * entrypoints, in the same class loader, so the state is safe to read later.</p>
 */
public final class BackendCatalog {

    public static final String VULKAN = "vulkan";
    public static final String OPENGL = "opengl";
    public static final String DX12 = "dx12";
    public static final String SOFTWARE = "software";

    /**
     * DirectXMod's backend, referenced by name only. Polyglass does not bundle
     * it, so the class is absent unless the user installed that mod.
     */
    public static final String DX12_CLASS = "com.xgdt.dx12.dx12.Dx12Backend";

    /** DirectXMod's Fabric mod id, used for the soft {@code recommends} entry. */
    public static final String DX12_MOD_ID = "gl4dx12";

    private static final Logger LOG = LoggerFactory.getLogger("polyglass");

    private static volatile List<String> lastPlan = List.of();
    private static volatile List<String> lastAvailable = List.of();

    private BackendCatalog() {
    }

    public static GpuBackend[] order(GpuBackend[] current) {
        Map<String, GpuBackend> pool = new LinkedHashMap<>();
        List<GpuBackend> foreign = new ArrayList<>();

        if (current != null) {
            for (GpuBackend backend : current) {
                if (backend == null) {
                    continue;
                }
                String id = idOf(backend);
                if (id == null) {
                    // Some other mod's backend. Never drop it, just keep it last.
                    foreign.add(backend);
                } else {
                    pool.putIfAbsent(id, backend);
                }
            }
        }

        PolyglassConfig config = PolyglassConfig.get();

        putIfAbsent(pool, OPENGL, GlBackend::new);
        putIfAbsent(pool, VULKAN, VulkanBackend::new);
        if (Probe.isWindows()) {
            putIfAbsent(pool, DX12, BackendCatalog::newDx12);
        }
        if (config.softwareEnabled()) {
            putIfAbsent(pool, SOFTWARE, SoftBackend::new);
        }

        List<String> available = new ArrayList<>(pool.keySet());
        lastAvailable = List.copyOf(available);

        List<String> plan = BackendPlan.resolve(config.backend(), config.order(), available);
        if (plan.isEmpty()) {
            LOG.warn("[polyglass] config matched no backend (backend={}, order={}); leaving the vanilla order alone",
                    config.backend(), config.order());
            lastPlan = List.of();
            return current == null ? new GpuBackend[0] : current;
        }

        List<GpuBackend> result = new ArrayList<>(plan.size() + foreign.size());
        for (String id : plan) {
            result.add(pool.get(id));
        }
        result.addAll(foreign);

        lastPlan = List.copyOf(plan);

        if (config.debug()) {
            LOG.info("[polyglass] backend order: {} (available: {}, foreign: {})",
                    plan, pool.keySet(), foreign.size());
        } else {
            LOG.info("[polyglass] backend order: {}", plan);
        }
        return result.toArray(new GpuBackend[0]);
    }

    /** The ids Polyglass will ask the game to try this session, most preferred first. */
    public static List<String> lastPlan() {
        return lastPlan;
    }

    /** The ids that could be constructed this session, before ordering. */
    public static List<String> lastAvailable() {
        return lastAvailable;
    }

    /** Maps a live backend instance to the id used in {@code polyglass.json}. */
    public static String idOf(GpuBackend backend) {
        if (backend.getClass().getName().equals(DX12_CLASS)) {
            return DX12;
        }
        if (backend instanceof GlBackend) {
            return OPENGL;
        }
        if (backend instanceof VulkanBackend) {
            return VULKAN;
        }
        if (backend instanceof SoftBackend) {
            return SOFTWARE;
        }
        return null;
    }

    /**
     * Whether an id <em>could</em> be offered on this machine. This is a static
     * capability check, not proof that a device can be created — driver and
     * hardware failures only show up when the game actually boots.
     */
    public static boolean isOffered(String id) {
        return switch (BackendPlan.normalize(id)) {
            case VULKAN, OPENGL -> true;
            case DX12 -> Probe.isWindows() && Probe.hasClass(DX12_CLASS);
            case SOFTWARE -> PolyglassConfig.get().softwareEnabled();
            default -> false;
        };
    }

    /** Whether DirectXMod is installed, i.e. whether the {@code dx12} entry can exist. */
    public static boolean directXModInstalled() {
        return Probe.hasClass(DX12_CLASS);
    }

    /**
     * Human-readable description of the device the game actually created, e.g.
     * {@code "OpenGL 4.6"} or the Vulkan driver string. Never throws.
     */
    public static String runningBackend() {
        try {
            String description = RenderSystem.getBackendDescription();
            return description == null || description.isBlank() ? "unknown" : description;
        } catch (Throwable t) {
            return "not initialised";
        }
    }

    private static GpuBackend newDx12() throws ReflectiveOperationException {
        Class<?> type = Class.forName(DX12_CLASS);
        return (GpuBackend) type.getDeclaredConstructor().newInstance();
    }

    @FunctionalInterface
    private interface BackendFactory {
        GpuBackend create() throws Exception;
    }

    private static void putIfAbsent(Map<String, GpuBackend> pool, String id, BackendFactory factory) {
        if (pool.containsKey(id)) {
            return;
        }
        try {
            pool.put(id, factory.create());
        } catch (Throwable t) {
            // Not installed, wrong platform, or the driver cannot even enumerate.
            // Missing ids simply never reach the plan.
            LOG.debug("[polyglass] backend '{}' unavailable: {}", id, t.toString());
        }
    }
}
