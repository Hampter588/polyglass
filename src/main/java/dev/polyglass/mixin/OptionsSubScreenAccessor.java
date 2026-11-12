package dev.polyglass.mixin;

import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * {@code OptionsSubScreen#list} is protected, so a mixin on a subclass cannot
 * {@code @Shadow} it — Mixin only resolves shadow fields declared on the target
 * class itself. An accessor on the declaring class is the supported way in.
 */
@Mixin(OptionsSubScreen.class)
public interface OptionsSubScreenAccessor {

    @Accessor("list")
    OptionsList polyglass$list();
}
