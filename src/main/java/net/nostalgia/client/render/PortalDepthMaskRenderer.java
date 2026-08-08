package net.nostalgia.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.textures.FilterMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.UniformType;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.OptionalDouble;
import java.util.OptionalInt;

@Environment(EnvType.CLIENT)
public class PortalDepthMaskRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final net.minecraft.resources.Identifier RIFT_TEXTURE_ID = net.minecraft.resources.Identifier.fromNamespaceAndPath("nostalgia", "textures/environment/rift_data.png");

    public static final RenderPipeline PIPELINE = RenderPipeline.builder()
            .withLocation(net.minecraft.resources.Identifier.fromNamespaceAndPath("nostalgia", "depth_mask"))
            .withVertexShader(net.minecraft.resources.Identifier.fromNamespaceAndPath("nostalgia", "core/portal_sky_rip_v2"))
            .withFragmentShader(net.minecraft.resources.Identifier.fromNamespaceAndPath("nostalgia", "core/depth_mask"))
            .withSampler("Sampler1")
            .withSampler("Sampler2")
            .withUniform("WhiteoutData", UniformType.UNIFORM_BUFFER)
            // 26.1 built pipeline state from ColorTargetState/DepthStencilState objects;
            // 1.21.11 sets the same state directly on the builder.
            // ColorTargetState(Optional.empty(), WRITE_NONE) == write no colour at all:
            // this pass only writes depth, which is the whole point of a depth mask.
            .withoutBlend()
            .withColorWrite(false)
            // CompareOp.ALWAYS_PASS == NO_DEPTH_TEST; the trailing flag was depth write.
            .withDepthTestFunction(com.mojang.blaze3d.platform.DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(true)
            .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
            .build();

    private static MappableRingBuffer uboBuffer;

    private static void initUbo() {
        uboBuffer = new MappableRingBuffer(() -> "Nostalgia Depth Mask UBO", 130, 128);
    }

    public static boolean shouldRender() {
        return net.nostalgia.client.events.core.ClientRitualEventRegistry.activeSkyPortal() != null;
    }

    public static void render(RenderTarget target, DeltaTracker tracker) {
        if (target == null || target.getColorTextureView() == null || target.getDepthTextureView() == null) {
            return;
        }

        if (uboBuffer == null) {
            initUbo();
        }

        Minecraft mc = Minecraft.getInstance();
        net.minecraft.client.Camera camera = mc.gameRenderer.getMainCamera();
        
        // Camera matrix access is centralised in UboShaderUtil: 1.21.11 removed the
        // Camera matrix getters, so the view matrix is rebuilt from Camera.rotation().
        org.joml.Matrix4f invViewProj;
        if (PortalSkyRenderer.capturedProjectionMatrix != null && PortalSkyRenderer.capturedModelViewMatrix != null) {
            invViewProj = new org.joml.Matrix4f(PortalSkyRenderer.capturedProjectionMatrix).mul(PortalSkyRenderer.capturedModelViewMatrix).invert();
        } else {
            invViewProj = UboShaderUtil.getInverseViewProjMatrix(camera, PortalSkyRenderer.capturedProjectionMatrix);
        }

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView viewMapped = encoder.mapBuffer(uboBuffer.currentBuffer(), false, true)) {
            Std140Builder builder = Std140Builder.intoBuffer(viewMapped.data());
            
            net.minecraft.world.phys.Vec3 camPos = camera.position();
            
            net.nostalgia.alphalogic.ritual.event.SkyPortalEvent skyPortal = net.nostalgia.client.events.core.ClientRitualEventRegistry.activeSkyPortal();
            net.nostalgia.alphalogic.ritual.event.ClientEchoRitualView transition = net.nostalgia.client.events.core.ClientRitualEventRegistry.activeTransition();
            net.minecraft.core.BlockPos center = skyPortal != null ? skyPortal.center() : transition.ritualCenter();
            double trueCenterX = center.getX();
            double trueCenterY = center.getY();
            double trueCenterZ = center.getZ();

            float tTime = skyPortal != null ? skyPortal.time() : transition.transitionTimeSeconds();

            builder.putVec4(
                    (float) (trueCenterX + 0.5d - camPos.x),
                    (float) (trueCenterY + 0.5d - camPos.y),
                    (float) (trueCenterZ + 0.5d - camPos.z),
                    tTime 
            );
            builder.putMat4f(invViewProj);
            
            float camY = (float) camPos.y;
            int cPlaneY = 256;
            if (skyPortal != null) {
                boolean isTarget = mc.level != null && mc.level.dimension().identifier().toString().equals(net.nostalgia.client.render.PortalSkyRenderer.originalTargetDimension);
                cPlaneY = isTarget ? net.nostalgia.client.render.PortalSkyRenderer.crackPlaneYTarget : net.nostalgia.client.render.PortalSkyRenderer.crackPlaneY;
            } else if (transition.ritualCenter() != null) {
                cPlaneY = transition.ritualCenter().getY() + 90;
            }
            builder.putVec4((float) (cPlaneY - camPos.y), camY, 0.0f, 0.0f); 
            float portalRadius = net.nostalgia.client.events.echo.RitualVisualManager.getAlphaRadius();
            if (portalRadius < 5.0f) portalRadius = 5.0f;
            builder.putVec4(0.0f, 0.0f, 0.0f, portalRadius); 
            builder.putVec4(0.0f, 0.0f, 0.0f, 1.0f); 
        } catch (Exception e) {
            LOGGER.error("Failed to map Depth Mask UBO", e);
            return;
        }

        RenderSystem.backupProjectionMatrix();
        
        net.minecraft.client.renderer.texture.AbstractTexture riftTexture = mc.getTextureManager().getTexture(RIFT_TEXTURE_ID);

        try (RenderPass pass = encoder.createRenderPass(
                () -> "Nostalgia Depth Mask",
                target.getColorTextureView(),
                OptionalInt.empty(),
                target.getDepthTextureView(),
                OptionalDouble.empty() 
        )) {
            pass.setPipeline(PIPELINE);
            pass.bindTexture("Sampler2", riftTexture.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setUniform("WhiteoutData", uboBuffer.currentBuffer());
            
            pass.draw(0, 3);
        }

        uboBuffer.rotate();
        RenderSystem.restoreProjectionMatrix();
    }
}
