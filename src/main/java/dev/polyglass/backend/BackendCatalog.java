package dev.polyglass.backend;

import com.mojang.renderpearl.api.device.GpuBackend;
import com.mojang.renderpearl.backend.opengl.GlBackend;
import com.mojang.renderpearl.backend.vulkan.VulkanBackend;
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
    private static final String DX12_CLASS = "com.xgdt.dx12.dx12.Dx12Backend";

    private static final Logger LOG = LoggerFactory.getLogger("polyglass");

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

        List<String> plan = BackendPlan.resolve(config.backend(), config.order(), new ArrayList<>(pool.keySet()));
        if (plan.isEmpty()) {
            LOG.warn("[polyglass] config matched no backend (backend={}, order={}); leaving the vanilla order alone",
                    config.backend(), config.order());
            return current == null ? new GpuBackend[0] : current;
        }

        List<GpuBackend> result = new ArrayList<>(plan.size() + foreign.size());
        for (String id : plan) {
            result.add(pool.get(id));
        }
        result.addAll(foreign);

        if (config.debug()) {
            LOG.info("[polyglass] backend order: {} (available: {}, extra: {})",
                    plan, pool.keySet(), foreign.size());
        }
        return result.toArray(new GpuBackend[0]);
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
