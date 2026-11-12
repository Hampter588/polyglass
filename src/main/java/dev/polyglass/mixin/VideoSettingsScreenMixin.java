package dev.polyglass.mixin;

import dev.polyglass.backend.BackendCatalog;
import dev.polyglass.config.PolyglassConfig;
import dev.polyglass.menu.PolyglassScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Puts Polyglass where the setting it controls actually lives: a row at the end
 * of Video Settings. Clicking it opens {@link PolyglassScreen}, which returns
 * here on Done rather than dropping the player back into the game.
 *
 * <p>Additive on purpose — vanilla's own Graphics API row stays, and Polyglass
 * keeps it in sync (see {@code PolyglassScreen#syncVanilla}). Replacing the row
 * would mean rebuilding an {@code OptionsList.Entry}, which is far more fragile
 * than appending a button.</p>
 *
 * <p>Note: DirectXMod replaces the whole screen with its own
 * {@code Dx12VideoSettingsScreen}, which overrides {@code init()} without
 * calling {@code super}. That screen has no {@code OptionsList}, so this row is
 * not present when both mods are installed; the F9 key still is.</p>
 */
@Mixin(VideoSettingsScreen.class)
public abstract class VideoSettingsScreenMixin {

    @Inject(method = "addOptions", at = @At("TAIL"))
    private void polyglass$addEntry(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen self = (Screen) (Object) this;
        ((OptionsSubScreenAccessor) this).polyglass$list().addBig(
                Button.builder(polyglass$label(),
                                button -> minecraft.setScreenAndShow(new PolyglassScreen(self)))
                        .build());
    }

    private static Component polyglass$label() {
        return Component.literal("Graphics API: "
                + BackendCatalog.displayName(PolyglassConfig.get().backend())
                + " — Polyglass…");
    }
}
