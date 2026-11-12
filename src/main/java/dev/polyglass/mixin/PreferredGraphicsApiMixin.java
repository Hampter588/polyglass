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
 */
@Mixin(PreferredGraphicsApi.class)
public abstract class PreferredGraphicsApiMixin {

    @Inject(method = "getBackendsToTry", at = @At("RETURN"), cancellable = true)
    private void polyglass$orderBackends(CallbackInfoReturnable<GpuBackend[]> cir) {
        GpuBackend[] ordered = BackendCatalog.order(cir.getReturnValue());
        cir.setReturnValue(ordered);
    }
}
