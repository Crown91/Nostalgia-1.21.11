package net.nostalgia.client.render;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.nostalgia.alphalogic.ritual.event.ClientEchoRitualView;
import net.nostalgia.client.events.core.ClientRitualEventRegistry;
import net.nostalgia.world.dimension.ModDimensions;

/**
 * Swaps the water sprites for the alpha ones inside the alpha dimension.
 *
 * <p>Replaces {@code AlphaWaterRendererMixin} and {@code AlphaSodiumWaterMixin}. Those two mixins
 * patched {@code FluidRenderer#tesselate} and Sodium's fluid render context to return a custom
 * {@code FluidModel}. In 1.21.11 there is no {@code FluidRenderer}, no {@code FluidModel} and no
 * {@code FluidStateModelSet}; water is drawn by {@code LiquidBlockRenderer}. What does exist is
 * {@code fabric-rendering-fluids-v1}, and Sodium honours it: its
 * {@code FluidRendererImpl$DefaultRenderContext} implements {@code FluidRendering.DefaultRenderer}
 * and takes its sprite array straight from the registered {@link FluidRenderHandler}. So a single
 * handler covers the vanilla renderer and the Sodium renderer at once, with no mixin and no
 * dependency on either one's internals.
 *
 * <p>The sprite array layout is the one {@link SimpleFluidRenderHandler} uses: still, flowing,
 * overlay.
 */
public final class AlphaFluidRendering implements FluidRenderHandler {

        private static final Identifier ALPHA_STILL =
                        Identifier.fromNamespaceAndPath("nostalgia", "block/alpha_water_still");
        private static final Identifier ALPHA_FLOWING =
                        Identifier.fromNamespaceAndPath("nostalgia", "block/alpha_water_flow");
        private static final Identifier ALPHA_OVERLAY =
                        Identifier.fromNamespaceAndPath("nostalgia", "block/alpha_water_overlay");

        private final TextureAtlasSprite[] vanillaSprites = new TextureAtlasSprite[3];
        private final TextureAtlasSprite[] alphaSprites = new TextureAtlasSprite[3];

        private AlphaFluidRendering() {
        }

        public static void register() {
                AlphaFluidRendering handler = new AlphaFluidRendering();
                FluidRenderHandlerRegistry.INSTANCE.register(Fluids.WATER, handler);
                FluidRenderHandlerRegistry.INSTANCE.register(Fluids.FLOWING_WATER, handler);
        }

        @Override
        public void reloadTextures(TextureAtlas atlas) {
                this.vanillaSprites[0] = atlas.getSprite(SimpleFluidRenderHandler.WATER_STILL);
                this.vanillaSprites[1] = atlas.getSprite(SimpleFluidRenderHandler.WATER_FLOWING);
                this.vanillaSprites[2] = atlas.getSprite(SimpleFluidRenderHandler.WATER_OVERLAY);

                this.alphaSprites[0] = atlas.getSprite(ALPHA_STILL);
                this.alphaSprites[1] = atlas.getSprite(ALPHA_FLOWING);
                this.alphaSprites[2] = atlas.getSprite(ALPHA_OVERLAY);
        }

        @Override
        public TextureAtlasSprite[] getFluidSprites(BlockAndTintGetter level, BlockPos pos,
                        FluidState fluidState) {
                return isAlphaWater(pos) ? this.alphaSprites : this.vanillaSprites;
        }

        @Override
        public int getFluidColor(BlockAndTintGetter level, BlockPos pos, FluidState fluidState) {
                // The 26.1 mixin handed the alpha model a BlockTintSource that always returned
                // 0xFFFFFF, because alpha water is not biome tinted. Everywhere else the vanilla
                // biome tint has to be kept, otherwise swamps and oceans lose their water colour.
                if (isAlphaWater(pos)) {
                        return 0xFFFFFF;
                }

                return BiomeColors.getAverageWaterColor(level, pos);
        }

        /**
         * Decides whether the water at this position belongs to the alpha world.
         *
         * <p>Logic copied unchanged from {@code AlphaWaterRendererMixin.java.orig}: inside the alpha
         * dimension water is alpha water; a hologram block shows the opposite dimension, so the
         * answer flips; and a block standing inside an active ritual sphere follows the sphere's
         * target dimension instead of the one the player is currently standing in.
         */
        private static boolean isAlphaWater(BlockPos pos) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.level == null) {
                        return false;
                }

                boolean inAlphaDimension =
                                minecraft.level.dimension().equals(ModDimensions.ALPHA_112_01_LEVEL_KEY);

                if (HologramRenderHelper.isBlockInverted(pos)) {
                        return !inAlphaDimension;
                }

                ClientEchoRitualView transition = ClientRitualEventRegistry.activeTransition();
                if (transition != null && !transition.isBystander() && transition.ritualCenter() != null) {
                        double distSq = pos.distSqr(transition.ritualCenter());
                        float radius = transition.alphaRadius();
                        if (distSq <= radius * radius) {
                                return transition.targetDimension() != null
                                                && transition.targetDimension().contains("alpha");
                        }
                }

                return inAlphaDimension;
        }
}
