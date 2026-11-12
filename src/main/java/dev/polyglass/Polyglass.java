package dev.polyglass;

import com.mojang.blaze3d.platform.InputConstants;
import dev.polyglass.config.PolyglassConfig;
import dev.polyglass.menu.PolyglassScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private KeyMapping menuKey;

    @Override
    public void onInitializeClient() {
        PolyglassConfig config = PolyglassConfig.get();
        LOG.info("[polyglass] config {} — backend={}, order={}, software={}",
                PolyglassConfig.path(), config.backend(), config.order(), config.softwareEnabled());
        LOG.info("[polyglass] DirectXMod present: {}, platform: {}",
                Probe.hasClass("com.xgdt.dx12.dx12.Dx12Backend"), System.getProperty("os.name"));

        menuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.polyglass.menu",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_F9,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"))));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.consumeClick()) {
                client.setScreenAndShow(new PolyglassScreen());
            }
        });
    }
}
