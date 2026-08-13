package net.nostalgia.mixin.rd132211;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.caffeinemc.mods.sodium.client.render.model.AmbientOcclusionMode;
import net.caffeinemc.mods.sodium.fabric.block.FabricBlockAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.nostalgia.world.dimension.ModDimensions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// rd-132211 predates directional face shading: every face of every block was
// drawn at full brightness. 26.1 expressed per-face brightness as a six-float
// "cardinal lighting" object; 1.21.11 has no such type and asks the level for
// one face at a time through getShade, so flat lighting is now a constant 1.0F.
@Mixin(ClientLevel.class)
abstract class RdFlatCardinalMixin {

    @Inject(method = "getShade", at = @At("HEAD"), cancellable = true)
    private void nostalgia$flatInRD(Direction direction, boolean shade, CallbackInfoReturnable<Float> cir) {
        ClientLevel self = (ClientLevel) (Object) this;
        if (self.dimension().equals(ModDimensions.RD_132211_LEVEL_KEY)) {
            cir.setReturnValue(1.0F);
        }
    }
}

@Mixin(EntityRenderer.class)
abstract class RdNoShadowMixin {
    @Inject(method = "extractShadow", at = @At("HEAD"), cancellable = true)
    private void nostalgia$killShadowInRD(EntityRenderState state, Minecraft mc, net.minecraft.world.level.Level level, CallbackInfo ci) {
        if (level.dimension().equals(ModDimensions.RD_132211_LEVEL_KEY)) {
            state.shadowPieces.clear();
            state.shadowRadius = 0.0F;
            ci.cancel();
        }
    }
}

@Mixin(EntityRenderer.class)
abstract class RdFullBrightEntityMixin {
    @Inject(method = "getPackedLightCoords", at = @At("HEAD"), cancellable = true)
    private void nostalgia$fullBrightEntityInRD(Entity entity, float partialTickTime, CallbackInfoReturnable<Integer> cir) {
        if (entity.level().dimension().equals(ModDimensions.RD_132211_LEVEL_KEY)) {
            // identical to the old pack(15, 15): full sky light and full block light
            cir.setReturnValue(LightTexture.FULL_BRIGHT);
        }
    }
}

@Mixin(net.minecraft.client.renderer.LightTexture.class)
abstract class RdFullBrightLightmapMixin {
    @Shadow @Final private GpuTexture texture;

    @Inject(method = "updateLightTexture", at = @At("HEAD"), cancellable = true)
    private void nostalgia$fullBrightInRD(float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.dimension().equals(ModDimensions.RD_132211_LEVEL_KEY)) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.texture, -1);
            ci.cancel();
        }
    }
}

// Sodium moved the ambient occlusion decision out of LevelSlice and into
// PlatformBlockAccess.usesAmbientOcclusion, which returns an AmbientOcclusionMode
// rather than a boolean. FabricBlockAccess is the Fabric implementation of it.
@Mixin(value = FabricBlockAccess.class, remap = false)
abstract class RdSodiumAOMixin {

    @Inject(
            method = "usesAmbientOcclusion",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void nostalgia$disableAOInRD(CallbackInfoReturnable<AmbientOcclusionMode> cir) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && level.dimension().equals(ModDimensions.RD_132211_LEVEL_KEY)) {
            cir.setReturnValue(AmbientOcclusionMode.DISABLED);
        }
    }
}
