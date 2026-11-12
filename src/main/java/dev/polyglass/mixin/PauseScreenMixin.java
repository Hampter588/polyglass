package dev.polyglass.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.polyglass.backend.BackendCatalog;
import dev.polyglass.config.PolyglassConfig;
import dev.polyglass.menu.PolyglassScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a full-width "Polyglass…" row to the bottom of the pause menu.
 *
 * <p>Why here and not in Video Settings: DirectXMod replaces
 * {@code VideoSettingsScreen} with its own {@code Dx12VideoSettingsScreen},
 * which draws and hit-tests its rows by hand and never calls the vanilla
 * {@code addOptions}. Nothing injected into the vanilla screen survives that.
 * The pause menu is untouched by DirectXMod and is one click from the game.</p>
 *
 * <p>The injection point is the single {@code GridLayout.arrangeElements()} call
 * at the end of {@code createPauseMenu}, so every vanilla button has already
 * been added to the {@code RowHelper} and ours simply becomes the last row.</p>
 */
@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin {

    @Inject(
            method = "createPauseMenu",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/GridLayout;arrangeElements()V"))
    private void polyglass$addRow(CallbackInfo ci,
                                  @Local(ordinal = 0) GridLayout.RowHelper rows) {
        Minecraft minecraft = Minecraft.getInstance();
        PauseScreen self = (PauseScreen) (Object) this;
        Button button = Button.builder(
                        Component.literal("Polyglass: "
                                + BackendCatalog.displayName(PolyglassConfig.get().backend()) + "…"),
                        b -> minecraft.setScreenAndShow(new PolyglassScreen(self)))
                .width(204)
                .build();
        rows.addChild(button, 2);
    }
}
