package dev.polyglass;

import dev.polyglass.api.Backends;
import dev.polyglass.api.RenderPath;
import dev.polyglass.api.Selection;
import dev.polyglass.config.PolyglassConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entrypoint: picks a backend and a render path, and hands them to the
 * renderer.
 *
 * <p>Milestone M0 only does selection and logging. From M1 on, device creation
 * happens when the game window is created, and the renderer drives
 * {@link dev.polyglass.api.Device} instead of {@code RenderSystem}.
 */
public final class Polyglass implements ClientModInitializer {

    public static final String MOD_ID = "polyglass";
    public static final Logger LOGGER = LoggerFactory.getLogger("Polyglass");

    private static volatile PolyglassConfig config;
    private static volatile Selection selection;

    @Override
    public void onInitializeClient() {
        PolyglassConfig loaded = PolyglassConfig.load(FabricLoader.getInstance().getConfigDir());
        config = loaded;

        LOGGER.info("Selecting graphics backend (requested '{}', fallback {})",
                loaded.backend(), loaded.fallback());

        Selection chosen = Backends.select(loaded.backend(), loaded.fallback());

        if (chosen.path() == RenderPath.MESH && !loaded.meshPath) {
            LOGGER.info("Mesh render path is disabled in polyglass.json; using the standard path");
            chosen = new Selection(chosen.backend(), RenderPath.STANDARD, chosen.requested());
        }

        selection = chosen;
        LOGGER.info("Using {} ({}) with the {} render path",
                chosen.backend().id(), chosen.backend().api().displayName(), chosen.path());
    }

    public static Selection selection() {
        Selection current = selection;
        if (current == null) {
            throw new IllegalStateException("Polyglass has not initialised yet");
        }
        return current;
    }

    public static PolyglassConfig config() {
        PolyglassConfig current = config;
        if (current == null) {
            throw new IllegalStateException("Polyglass has not initialised yet");
        }
        return current;
    }
}
