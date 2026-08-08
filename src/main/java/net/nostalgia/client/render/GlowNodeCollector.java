package net.nostalgia.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BlockStateModel;
// 1.21.11 keeps BakedQuad next to the other block model classes.
// The 26.1 package net.minecraft.client.resources.model.geometry does not exist here.
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Wraps a submit node collector and forces a purple glow tint onto everything that
 * carries a tint colour, leaving geometry and ordering untouched.
 *
 * The method list below follows the real 1.21.11 OrderedSubmitNodeCollector contract:
 *  - submitBlock(...) is new here and simply forwards.
 *  - submitBlockModel now takes a BlockStateModel plus three tint floats instead of
 *    26.1's list of model parts.
 *  - submitItem gained a RenderType argument.
 *  - 26.1's submitBreakingBlockModel no longer exists and is gone.
 */
public class GlowNodeCollector implements SubmitNodeCollector {

    private final SubmitNodeCollector parent;
    public final int overrideColor;

    public GlowNodeCollector(SubmitNodeCollector parent, float alpha) {
        this.parent = parent;
        int alphaInt = (int)(alpha * 255.0f);
        this.overrideColor = ARGB.color(alphaInt, 0xAA, 0x00, 0xFF);
    }

    @Override public OrderedSubmitNodeCollector order(int order) { return new GlowOrderedCollector(parent.order(order), overrideColor); }
    @Override public void submitShadow(PoseStack p, float r, List<EntityRenderState.ShadowPiece> pieces) { parent.submitShadow(p, r, pieces); }
    @Override public void submitNameTag(PoseStack p, @Nullable Vec3 a, int o, Component n, boolean s, int l, double d, CameraRenderState c) { parent.submitNameTag(p, a, o, n, s, l, d, c); }
    @Override public void submitText(PoseStack p, float x, float y, FormattedCharSequence s, boolean ds, Font.DisplayMode dm, int l, int c, int bg, int oc) { parent.submitText(p, x, y, s, ds, dm, l, c, bg, oc); }
    @Override public void submitFlame(PoseStack p, EntityRenderState rs, Quaternionf rot) { parent.submitFlame(p, rs, rot); }
    @Override public void submitLeash(PoseStack p, EntityRenderState.LeashState ls) { parent.submitLeash(p, ls); }
    @Override public <S> void submitModel(Model<? super S> model, S state, PoseStack p, RenderType rt, int l, int ov, int tc, @Nullable TextureAtlasSprite sp, int oc, ModelFeatureRenderer.CrumblingOverlay cr) { parent.submitModel(model, state, p, rt, l, ov, overrideColor, sp, oc, cr); }
    @Override public void submitModelPart(ModelPart mp, PoseStack p, RenderType rt, int l, int ov, @Nullable TextureAtlasSprite sp, boolean sh, boolean hf, int tc, ModelFeatureRenderer.CrumblingOverlay cr, int oc) { parent.submitModelPart(mp, p, rt, l, ov, sp, sh, hf, overrideColor, cr, oc); }
    @Override public void submitBlock(PoseStack p, BlockState bs, int l, int ov, int tc) { parent.submitBlock(p, bs, l, ov, tc); }
    @Override public void submitMovingBlock(PoseStack p, MovingBlockRenderState mb) { parent.submitMovingBlock(p, mb); }
    @Override public void submitBlockModel(PoseStack p, RenderType rt, BlockStateModel m, float r, float g, float b, int l, int ov, int tc) { parent.submitBlockModel(p, rt, m, r, g, b, l, ov, tc); }
    @Override public void submitItem(PoseStack p, ItemDisplayContext dc, int l, int ov, int tc, int[] tl, List<BakedQuad> q, RenderType rt, ItemStackRenderState.FoilType ft) { parent.submitItem(p, dc, l, ov, overrideColor, tl, q, rt, ft); }
    @Override public void submitCustomGeometry(PoseStack p, RenderType rt, SubmitNodeCollector.CustomGeometryRenderer cgr) { parent.submitCustomGeometry(p, rt, cgr); }
    @Override public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer pgr) { parent.submitParticleGroup(pgr); }

    private static class GlowOrderedCollector implements OrderedSubmitNodeCollector {
        private final OrderedSubmitNodeCollector parent;
        private final int overrideColor;
        public GlowOrderedCollector(OrderedSubmitNodeCollector parent, int overrideColor) { this.parent = parent; this.overrideColor = overrideColor; }
        @Override public void submitShadow(PoseStack p, float r, List<EntityRenderState.ShadowPiece> pieces) { parent.submitShadow(p, r, pieces); }
        @Override public void submitNameTag(PoseStack p, @Nullable Vec3 a, int o, Component n, boolean s, int l, double d, CameraRenderState c) { parent.submitNameTag(p, a, o, n, s, l, d, c); }
        @Override public void submitText(PoseStack p, float x, float y, FormattedCharSequence s, boolean ds, Font.DisplayMode dm, int l, int c, int bg, int oc) { parent.submitText(p, x, y, s, ds, dm, l, c, bg, oc); }
        @Override public void submitFlame(PoseStack p, EntityRenderState rs, Quaternionf rot) { parent.submitFlame(p, rs, rot); }
        @Override public void submitLeash(PoseStack p, EntityRenderState.LeashState ls) { parent.submitLeash(p, ls); }
        @Override public <S> void submitModel(Model<? super S> model, S state, PoseStack p, RenderType rt, int l, int ov, int tc, @Nullable TextureAtlasSprite sp, int oc, ModelFeatureRenderer.CrumblingOverlay cr) { parent.submitModel(model, state, p, rt, l, ov, overrideColor, sp, oc, cr); }
        @Override public void submitModelPart(ModelPart mp, PoseStack p, RenderType rt, int l, int ov, @Nullable TextureAtlasSprite sp, boolean sh, boolean hf, int tc, ModelFeatureRenderer.CrumblingOverlay cr, int oc) { parent.submitModelPart(mp, p, rt, l, ov, sp, sh, hf, overrideColor, cr, oc); }
        @Override public void submitBlock(PoseStack p, BlockState bs, int l, int ov, int tc) { parent.submitBlock(p, bs, l, ov, tc); }
        @Override public void submitMovingBlock(PoseStack p, MovingBlockRenderState mb) { parent.submitMovingBlock(p, mb); }
        @Override public void submitBlockModel(PoseStack p, RenderType rt, BlockStateModel m, float r, float g, float b, int l, int ov, int tc) { parent.submitBlockModel(p, rt, m, r, g, b, l, ov, tc); }
        @Override public void submitItem(PoseStack p, ItemDisplayContext dc, int l, int ov, int tc, int[] tl, List<BakedQuad> q, RenderType rt, ItemStackRenderState.FoilType ft) { parent.submitItem(p, dc, l, ov, overrideColor, tl, q, rt, ft); }
        @Override public void submitCustomGeometry(PoseStack p, RenderType rt, SubmitNodeCollector.CustomGeometryRenderer cgr) { parent.submitCustomGeometry(p, rt, cgr); }
        @Override public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer pgr) { parent.submitParticleGroup(pgr); }
    }
}
