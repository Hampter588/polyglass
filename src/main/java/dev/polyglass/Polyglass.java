package dev.polyglass;

import com.mojang.blaze3d.platform.InputConstants;
import dev.polyglass.backend.BackendCatalog;
import dev.polyglass.config.PolyglassConfig;
import dev.polyglass.menu.PolyglassScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client entry point.
 *
 * <p>The backend decision itself happens earlier, in
 * {@code PreferredGraphicsApiMixin}, because the game builds its device before
 * mod entrypoints run. All this class does is report the outcome and expose the
 * picker screen.</p>
 */
public final class Polyglass implements ClientModInitializer {

    public static final String MOD_ID = "polyglass";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    private static final String DX12_CLASS = BackendCatalog.DX12_CLASS;

    private KeyMapping menuKey;
    private final AtomicBoolean reported = new AtomicBoolean();

    @Override
    public void onInitializeClient() {
        PolyglassConfig config = PolyglassConfig.get();
        LOG.info("[polyglass] config {} — backend={}, order={}, debug={}",
                PolyglassConfig.path(), config.backend(), config.order(), config.debug());
        LOG.info("[polyglass] DirectXMod present: {}, platform: {}",
                Probe.hasClass(DX12_CLASS), System.getProperty("os.name"));

        menuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.polyglass.menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F9,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"))));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // The device is created after mod entrypoints, so the first tick is
            // the earliest point at which the real backend is known.
            if (reported.compareAndSet(false, true)) {
                LOG.info("[polyglass] offered: {}, asked for: {}, running on: {}",
                        BackendCatalog.lastAvailable(), BackendCatalog.lastPlan(),
                        BackendCatalog.runningBackend());
            }
            while (menuKey.consumeClick()) {
                client.setScreenAndShow(new PolyglassScreen());
            }
        });
    }
}
