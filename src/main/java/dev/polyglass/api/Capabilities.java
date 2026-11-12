package dev.polyglass.api;

/**
 * What the selected device and driver can do. Probed once at device creation;
 * the renderer keys its pipeline variants off this instead of querying the API
 * ad hoc.
 *
 * @param api               API that produced these capabilities
 * @param deviceName        adapter/GPU name as reported by the driver
 * @param driverInfo        driver version / build string
 * @param compute           compute pipelines are usable
 * @param meshShaders       mesh shaders are usable (NV_mesh_shader / VK_EXT_mesh_shader)
 * @param bindless          bindless or descriptor-indexed resource access is usable
 * @param timestampQueries  GPU timestamps are available for profiling
 * @param maxTextureSize    2D texture limit, per side
 * @param maxFramesInFlight swapchain images the present queue can hold
 */
public record Capabilities(
        GraphicsApi api,
        String deviceName,
        String driverInfo,
        boolean compute,
        boolean meshShaders,
        boolean bindless,
        boolean timestampQueries,
        int maxTextureSize,
        int maxFramesInFlight) {

    /** Placeholder used by backends that have not been implemented yet. */
    public static Capabilities unknown(GraphicsApi api) {
        return new Capabilities(api, "unknown", "unknown", false, false, false, false, 0, 1);
    }
}
