package net.nostalgia.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.nostalgia.entity.SkyPortalBeamEntity;

public class SkyPortalBeamRenderer extends EntityRenderer<SkyPortalBeamEntity, SkyPortalBeamRenderer.BeamRenderState> {

    public static final Identifier BEAM_LOCATION = Identifier.withDefaultNamespace("textures/entity/beacon/beacon_beam.png");

    public SkyPortalBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public BeamRenderState createRenderState() { return new BeamRenderState(); }

    @Override
    public void extractRenderState(SkyPortalBeamEntity entity, BeamRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.gameTime = entity.level().getGameTime();
        state.partialTick = partialTick;
        int color = 0xAA00AA;
        net.nostalgia.alphalogic.ritual.SkyPortalEventInstance active = net.nostalgia.alphalogic.ritual.SkyPortalManager.getActive();
        if (active != null) {
            String target = active.targetDimension();
            if (target != null) {
                if (target.contains("alpha")) color = 0x00D6D6;
                else if (target.contains("rd")) color = 0xCC66FF;
                else if (target.contains("overworld")) color = 0x88FF00;
            }
        }
        state.color = color;
    }

    @Override
    public void submit(BeamRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        float animationTime = Math.floorMod(state.gameTime, 40) + state.partialTick;
        int color = state.color;
        poseStack.pushPose();
        int beamStart = 0, height = 320;
        float beamGlowRadius = 0.25F;
        float scroll = -animationTime;
        float texVOff = Mth.frac(scroll * 0.2F - Mth.floor(scroll * 0.1F));
        final float glowVv2 = -1.0F + texVOff;
        final float glowVv1 = height + glowVv2;
        submitNodeCollector.submitCustomGeometry(
            poseStack,
            RenderTypes.beaconBeam(BEAM_LOCATION, true),
            (pose, buffer) -> renderPart(pose, buffer, ARGB.color(32, color), beamStart, beamStart + height,
                -beamGlowRadius, -beamGlowRadius, beamGlowRadius, -beamGlowRadius,
                -beamGlowRadius, beamGlowRadius, beamGlowRadius, beamGlowRadius,
                0.0F, 1.0F, glowVv1, glowVv2)
        );
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private static void renderPart(PoseStack.Pose pose, VertexConsumer builder, int color, int beamStart, int beamEnd,
      float wnx, float wnz, float enx, float enz, float wsx, float wsz, float esx, float esz,
      float uu1, float uu2, float vv1, float vv2) {
        renderQuad(pose,builder,color,beamStart,beamEnd,wnx,wnz,enx,enz,uu1,uu2,vv1,vv2);
        renderQuad(pose,builder,color,beamStart,beamEnd,esx,esz,wsx,wsz,uu1,uu2,vv1,vv2);
        renderQuad(pose,builder,color,beamStart,beamEnd,enx,enz,esx,esz,uu1,uu2,vv1,vv2);
        renderQuad(pose,builder,color,beamStart,beamEnd,wsx,wsz,wnx,wnz,uu1,uu2,vv1,vv2);
    }

    private static void renderQuad(PoseStack.Pose pose, VertexConsumer builder, int color, int beamStart, int beamEnd,
      float wnx, float wnz, float enx, float enz, float uu1, float uu2, float vv1, float vv2) {
        addVertex(pose,builder,color,beamEnd,wnx,wnz,uu2,vv1);
        addVertex(pose,builder,color,beamStart,wnx,wnz,uu2,vv2);
        addVertex(pose,builder,color,beamStart,enx,enz,uu1,vv2);
        addVertex(pose,builder,color,beamEnd,enx,enz,uu1,vv1);
    }

    private static void addVertex(PoseStack.Pose pose, VertexConsumer builder, int color, int y, float x, float z, float u, float v) {
        builder.addVertex(pose, x, (float)y, z).setColor(color).setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    public static class BeamRenderState extends EntityRenderState {
        public long gameTime;
        public float partialTick;
        public int color;
    }
}
