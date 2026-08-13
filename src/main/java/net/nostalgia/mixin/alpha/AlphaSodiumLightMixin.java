package net.nostalgia.mixin.alpha;

import net.caffeinemc.mods.sodium.client.render.model.AmbientOcclusionMode;
import net.caffeinemc.mods.sodium.fabric.block.FabricBlockAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.nostalgia.world.dimension.ModDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Alpha 1.1.2_01 had no smooth lighting at all, so it must be forced off inside
// the alpha dimension regardless of the video settings.
@Mixin(value = FabricBlockAccess.class, remap = false)
public abstract class AlphaSodiumLightMixin {

    @Inject(
            method = "usesAmbientOcclusion",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void disableSmoothLightingInAlpha(CallbackInfoReturnable<AmbientOcclusionMode> cir) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && level.dimension().equals(ModDimensions.ALPHA_112_01_LEVEL_KEY)) {
            cir.setReturnValue(AmbientOcclusionMode.DISABLED);
        }
    }
}
