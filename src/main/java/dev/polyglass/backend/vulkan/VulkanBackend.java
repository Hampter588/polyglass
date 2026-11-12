package dev.polyglass.backend.vulkan;

import dev.polyglass.Probe;
import dev.polyglass.api.GraphicsApi;
import dev.polyglass.backend.AbstractBackend;

/**
 * Primary target on Windows and Linux.
 *
 * <p>Bindings: {@code org.lwjgl:lwjgl-vulkan} plus a Vulkan memory allocator.
 * These are NOT bundled with the game — see the commented dependency block in
 * build.gradle.kts. Until they are added, this backend reports unavailable and
 * selection falls through to the next entry in the chain.
 *
 * <p>macOS: there is no native Vulkan driver. LWJGL's Vulkan module bundles
 * MoltenVK, so the loader can be used there — but the instance must be created
 * with {@code VK_KHR_portability_enumeration} enabled and the
 * {@code VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR} flag set, otherwise
 * {@code vkEnumeratePhysicalDevices} returns nothing. MoltenVK is a Vulkan 1.4
 * subset, so treat macOS-Vulkan as a second-class path.
 */
public final class VulkanBackend extends AbstractBackend {

    public VulkanBackend() {
        super("vulkan", GraphicsApi.VULKAN, 10);
    }

    @Override
    public boolean isAvailable() {
        return Probe.hasClass("org.lwjgl.vulkan.VK10");
    }

    @Override
    public String unavailableReason() {
        return isAvailable() ? null : "LWJGL Vulkan bindings are not on the classpath";
    }
}
