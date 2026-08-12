package net.nostalgia.mixin.client.ritual;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.nostalgia.client.events.core.ClientRitualEventRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(CloudRenderer.class)
public class CloudRendererDepthMixin {

    @Unique
    private RenderPipeline nostalgia$noDepthClouds;
    @Unique
    private RenderPipeline nostalgia$noDepthFlatClouds;

    // PORT 26.1.2 -> 1.21.11: CloudRenderer.render lost its 'int range'
    // parameter, verified against javap:
    //   public void render(int, CloudStatus, float, Vec3, long, float)
    // The descriptor is spelled out so a future signature change fails the
    // build instead of silently not applying.
    @Inject(
            method = "render(ILnet/minecraft/client/CloudStatus;FLnet/minecraft/world/phys/Vec3;JF)V",
            at = @At("HEAD")
    )
    private void nostalgia$initPipelines(int color, CloudStatus cloudStatus, float bottomY, net.minecraft.world.phys.Vec3 cameraPosition, long gameTime, float partialTicks, CallbackInfo ci) {
        if (nostalgia$noDepthClouds == null) {
            try {
                Field snippetField = RenderPipelines.class.getDeclaredField("CLOUDS_SNIPPET");
                snippetField.setAccessible(true);
                RenderPipeline.Snippet snippet = (RenderPipeline.Snippet) snippetField.get(null);

                // 26.1 expressed this as a single DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false).
                // 1.21.11 splits the same two settings into separate builder calls:
                // the compare op became DepthTestFunction.LEQUAL_DEPTH_TEST and the
                // depth-write flag became withDepthWrite(false).
                nostalgia$noDepthClouds = RenderPipeline.builder(snippet)
                        .withLocation("pipeline/clouds_no_depth")
                        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                        .withDepthWrite(false)
                        .build();

                nostalgia$noDepthFlatClouds = RenderPipeline.builder(snippet)
                        .withLocation("pipeline/flat_clouds_no_depth")
                        .withCull(false)
                        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                        .withDepthWrite(false)
                        .build();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @ModifyVariable(
            method = "render(ILnet/minecraft/client/CloudStatus;FLnet/minecraft/world/phys/Vec3;JF)V",
            at = @At("STORE"),
            ordinal = 0
    )
    private RenderPipeline nostalgia$swapPipeline(RenderPipeline original) {
        if (ClientRitualEventRegistry.activeTransition() != null && nostalgia$noDepthClouds != null) {
            if (original == RenderPipelines.CLOUDS) {
                return nostalgia$noDepthClouds;
            } else if (original == RenderPipelines.FLAT_CLOUDS) {
                return nostalgia$noDepthFlatClouds;
            }
        }
        return original;
    }
}
