package net.nostalgia.mixin.client;

import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.nostalgia.client.render.CloudDepthResetRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererCloudResetMixin {

    @Shadow @Final private LevelTargetBundle targets;

    // PORT NOTE: 1.21.11 signatures, both confirmed against the mapped jar:
    //   renderLevel(GraphicsResourceAllocator, DeltaTracker, boolean, Camera,
    //               Matrix4f, Matrix4f, Matrix4f, GpuBufferSlice, Vector4f, boolean)
    //   addMainPass(FrameGraphBuilder, Frustum, Matrix4f, GpuBufferSlice, boolean,
    //               LevelRenderState, DeltaTracker, ProfilerFiller)
    // 26.1 replaces Camera with CameraRenderState, folds the matrices into one
    // Matrix4fc and appends ChunkSectionsToRender. The matrices are unused here,
    // so they keep positional names rather than guessed ones.
    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;addMainPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/culling/Frustum;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;ZLnet/minecraft/client/renderer/state/LevelRenderState;Lnet/minecraft/client/DeltaTracker;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            shift = At.Shift.AFTER
        )
    )
    private void nost$addCloudDepthResetPass(
            com.mojang.blaze3d.resource.GraphicsResourceAllocator resourceAllocator,
            net.minecraft.client.DeltaTracker deltaTracker,
            boolean renderOutline,
            net.minecraft.client.Camera camera,
            org.joml.Matrix4f matrix0,
            org.joml.Matrix4f matrix1,
            org.joml.Matrix4f matrix2,
            com.mojang.blaze3d.buffers.GpuBufferSlice terrainFog,
            org.joml.Vector4f fogColor,
            boolean shouldRenderSky,
            CallbackInfo ci,
            @com.llamalad7.mixinextras.sugar.Local FrameGraphBuilder frame
    ) {
        if (!CloudDepthResetRenderer.shouldRender()) return;
        FramePass pass = frame.addPass("nostalgia_cloud_depth_reset");
        com.mojang.blaze3d.resource.ResourceHandle<RenderTarget> handle = pass.readsAndWrites(this.targets.main);
        this.targets.main = handle;
        pass.executes(() -> {
            RenderTarget target = handle.get();
            CloudDepthResetRenderer.render(target, deltaTracker);
        });
    }
}
