package dev.polyglass;

import dev.polyglass.api.Backends;
import dev.polyglass.api.CommandList;
import dev.polyglass.api.Device;
import dev.polyglass.api.DeviceConfig;
import dev.polyglass.api.GraphicsApi;
import dev.polyglass.api.RenderPath;
import dev.polyglass.api.Selection;
import dev.polyglass.api.Swapchain;
import dev.polyglass.api.SwapchainConfig;
import dev.polyglass.backend.null_backend.NullDevice;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the selection path and the headless device. Needs no GPU and no
 * Minecraft classes, which is the point of the null backend.
 */
class NullBackendTest {

    @Test
    void explicitNullIsSelected() {
        Selection selection = Backends.select("null", List.of());

        assertEquals("null", selection.backend().id());
        assertSame(GraphicsApi.NULL, selection.backend().api());
        assertSame(RenderPath.STANDARD, selection.path());
    }

    @Test
    void unknownBackendFallsThroughToTheChain() {
        Selection selection = Backends.select("does-not-exist", List.of("null"));

        assertEquals("null", selection.backend().id());
    }

    @Test
    void nullDeviceDrivesAFrameAndCountsIt() {
        Selection selection = Backends.select("null", List.of());

        try (Device device = selection.backend().createDevice(new DeviceConfig(320, 240, false, false, false))) {
            Swapchain swapchain = device.createSwapchain(new SwapchainConfig(1L, 320, 240, false));
            swapchain.acquire();

            CommandList commands = device.beginFrame();
            commands.begin();
            commands.draw(3, 1, 0, 0);
            commands.end();

            device.submit(commands);
            device.endFrame();
            swapchain.present();

            NullDevice nullDevice = (NullDevice) device;
            assertEquals(1L, nullDevice.frameCount());

            NullDevice.NullCommandList recorded = (NullDevice.NullCommandList) commands;
            assertEquals(1L, recorded.drawCalls());
            assertEquals(3L, recorded.vertices());
        }
    }

    @Test
    void nullCapabilitiesAreHonest() {
        Device device = Backends.select("null", List.of())
                .backend()
                .createDevice(new DeviceConfig(16, 16, false, false, false));

        assertTrue(!device.capabilities().meshShaders());
        assertTrue(!device.capabilities().compute());
        assertEquals(GraphicsApi.NULL, device.capabilities().api());
        device.close();
    }
}
