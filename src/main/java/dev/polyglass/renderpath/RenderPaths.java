package dev.polyglass.renderpath;

import dev.polyglass.Polyglass;
import dev.polyglass.api.Capabilities;
import dev.polyglass.api.GraphicsApi;
import dev.polyglass.api.RenderPath;
import dev.polyglass.api.Selection;

/**
 * Decides whether a requested {@link RenderPath} is actually usable once the
 * device capabilities are known.
 *
 * <p>The mesh path needs GPU mesh shaders:
 * <ul>
 *   <li>OpenGL — {@code GL_NV_mesh_shader} (NVIDIA only), the technique Nvidium uses</li>
 *   <li>Vulkan — {@code VK_EXT_mesh_shader}</li>
 *   <li>D3D12 — mesh shader pipeline stages (DXR-era hardware)</li>
 *   <li>Metal — {@code MTLMeshRenderPipelineDescriptor} (Apple7+, drivers vary)</li>
 * </ul>
 */
public final class RenderPaths {

    private RenderPaths() {
    }

    public static boolean supportsMesh(GraphicsApi api, Capabilities caps) {
        // The API only matters for which extension was probed; the capability flag
        // is already the answered question. Kept as a hook for per-API quirks.
        return api != null && caps != null && caps.meshShaders();
    }

    /**
     * Downgrades {@link RenderPath#MESH} to {@link RenderPath#STANDARD} when the
     * device cannot do it, so the game still starts.
     */
    public static Selection resolve(Selection selection, Capabilities caps) {
        if (selection.path() != RenderPath.MESH) {
            return selection;
        }
        if (supportsMesh(selection.backend().api(), caps)) {
            return selection;
        }
        Polyglass.LOGGER.warn(
                "Mesh render path requested but '{}' ({}) has no usable mesh shaders; using the standard path",
                caps == null ? "no device" : caps.deviceName(),
                selection.backend().id());
        return new Selection(selection.backend(), RenderPath.STANDARD, selection.requested());
    }
}
