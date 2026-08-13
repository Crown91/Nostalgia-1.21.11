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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@Mixin(CloudRenderer.class)
public class CloudRendererDepthMixin {

    @Unique
    private static final Logger nostalgia$LOGGER = LoggerFactory.getLogger("nostalgia/clouds");

    @Unique
    private RenderPipeline nostalgia$noDepthClouds;
    @Unique
    private RenderPipeline nostalgia$noDepthFlatClouds;

    // PORT 26.1.2 -> 1.21.11: the pipeline snippet that 26.1 exposed as
    // RenderPipelines.CLOUDS_SNIPPET does not exist under that name in
    // 1.21.11. The previous implementation looked the field up by name on
    // every single frame and swallowed the resulting NoSuchFieldException with
    // printStackTrace(), which produced ~60 stack traces per second (3737 in a
    // 46-second session) and inflated latest.log to several megabytes.
    //
    // Resolution is now attempted exactly once. It first tries the 26.1 name,
    // then falls back to scanning RenderPipelines for any static
    // RenderPipeline.Snippet whose name mentions clouds, so a rename in a
    // future snapshot keeps working. If nothing suitable exists the feature
    // disables itself permanently after a single WARN line; the only effect is
    // that clouds keep writing depth during ritual transitions.
    @Unique
    private boolean nostalgia$pipelineInitDone;

    @Inject(
            method = "render(ILnet/minecraft/client/CloudStatus;FLnet/minecraft/world/phys/Vec3;JF)V",
            at = @At("HEAD")
    )
    private void nostalgia$initPipelines(int color, CloudStatus cloudStatus, float bottomY, net.minecraft.world.phys.Vec3 cameraPosition, long gameTime, float partialTicks, CallbackInfo ci) {
        if (nostalgia$pipelineInitDone) {
            return;
        }
        nostalgia$pipelineInitDone = true;

        RenderPipeline.Snippet snippet = nostalgia$findCloudSnippet();
        if (snippet == null) {
            nostalgia$LOGGER.warn("No cloud pipeline snippet found in {}; ritual clouds will keep writing depth. This is cosmetic only.", RenderPipelines.class.getName());
            return;
        }

        try {
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
        } catch (Throwable t) {
            nostalgia$noDepthClouds = null;
            nostalgia$noDepthFlatClouds = null;
            nostalgia$LOGGER.warn("Failed to build depth-less cloud pipelines; ritual clouds will keep writing depth.", t);
        }
    }

    @Unique
    private static RenderPipeline.Snippet nostalgia$findCloudSnippet() {
        // 1) the 26.1 name, if it ever comes back
        try {
            Field named = RenderPipelines.class.getDeclaredField("CLOUDS_SNIPPET");
            named.setAccessible(true);
            Object value = named.get(null);
            if (value instanceof RenderPipeline.Snippet found) {
                return found;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // fall through to the scan below
        }

        // 2) any static snippet whose name mentions clouds
        RenderPipeline.Snippet fallback = null;
        for (Field field : RenderPipelines.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (!RenderPipeline.Snippet.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(null);
                if (!(value instanceof RenderPipeline.Snippet snippet)) {
                    continue;
                }
                if (field.getName().toUpperCase(java.util.Locale.ROOT).contains("CLOUD")) {
                    return snippet;
                }
                if (fallback == null) {
                    fallback = snippet;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // keep scanning
            }
        }

        // 3) nothing cloud-specific: reuse the first snippet rather than crashing.
        // The builder overrides location, depth test and depth write anyway.
        return fallback;
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
