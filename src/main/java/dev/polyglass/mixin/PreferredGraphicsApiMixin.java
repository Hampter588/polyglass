package dev.polyglass.mixin;

import com.mojang.blaze3d.systems.GpuBackend;
import dev.polyglass.backend.BackendCatalog;
import net.minecraft.client.PreferredGraphicsApi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The whole integration.
 *
 * <p>Minecraft 26.x already owns the hard part: {@code PreferredGraphicsApi}
 * returns the candidate backends, and the game's boot loop tries them in order,
 * catching {@code BackendCreationException} and moving to the next. Polyglass
 * only rewrites that array, so it does not need its own renderer abstraction,
 * its own device lifecycle, or any of the game's rendering classes.</p>
 *
 * <p>Ordering is merged rather than replaced — see
 * {@link BackendCatalog#order(GpuBackend[])}.</p>
 *
 * <p><b>Priority 500.</b> DirectXMod injects at the same {@code RETURN} with
 * the default priority (1000) and <em>replaces</em> the array outright. Mixins
 * with a lower priority are applied first, and the first {@code RETURN} handler
 * to call {@code setReturnValue} wins: Mixin guards every subsequent handler
 * with an {@code isCancelled()} check, so once the return value is set the
 * remaining handlers never run. Applying first therefore guarantees Polyglass
 * decides the order. Without it DirectXMod won the race and the config was
 * silently ignored — that was observed in a real client log. Our merge still
 * adds DirectXMod's {@code dx12} instance (by reflection), it just places it
 * where the config says.</p>
 */
@Mixin(value = PreferredGraphicsApi.class, priority = 500)
public abstract class PreferredGraphicsApiMixin {

    @Inject(method = "getBackendsToTry", at = @At("RETURN"), cancellable = true)
    private void polyglass$orderBackends(CallbackInfoReturnable<GpuBackend[]> cir) {
        GpuBackend[] ordered = BackendCatalog.order(cir.getReturnValue());
        cir.setReturnValue(ordered);
    }
}
