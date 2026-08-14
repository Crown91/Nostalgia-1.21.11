package net.nostalgia.client.render;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.nostalgia.block.AlphaBlocks;

/**
 * Assigns chunk render layers to the alpha blocks.
 *
 * <p>Up to 26.1 the mod relied on the {@code "render_type"} field inside the block model JSON.
 * That field no longer exists in 1.21.11: no vanilla model declares it and no Fabric API jar even
 * contains the string. The layer is resolved by
 * {@link net.minecraft.client.renderer.ItemBlockRenderTypes#getChunkRenderType} from a private
 * static map, so anything not registered here silently falls back to
 * {@link ChunkSectionLayer#SOLID}. For a cutout texture that means the transparent pixels get
 * filled with the neighbouring colour bled in by the mipmap generator, which is why the flowers,
 * mushrooms and sugar cane showed up as flat opaque squares.
 */
public final class AlphaRenderLayers implements ClientModInitializer {

        @Override
        public void onInitializeClient() {
                register();
                AlphaFluidRendering.register();
        }

        public static void register() {
                BlockRenderLayerMap.putBlocks(
                                ChunkSectionLayer.CUTOUT,
                                AlphaBlocks.ALPHA_SAPLING,
                                AlphaBlocks.ALPHA_RED_FLOWER,
                                AlphaBlocks.ALPHA_RED_FLOWER_FLIPPED,
                                AlphaBlocks.ALPHA_YELLOW_FLOWER,
                                AlphaBlocks.ALPHA_YELLOW_FLOWER_FLIPPED,
                                AlphaBlocks.ALPHA_RED_MUSHROOM,
                                AlphaBlocks.ALPHA_BROWN_MUSHROOM,
                                AlphaBlocks.ALPHA_SUGAR_CANE,
                                AlphaBlocks.ALPHA_SUGAR_CANE_FLIPPED,
                                AlphaBlocks.ALPHA_COBWEB,
                                AlphaBlocks.ALPHA_WHEAT_CROP,
                                AlphaBlocks.ALPHA_LADDER,
                                AlphaBlocks.ALPHA_LEVER,
                                AlphaBlocks.ALPHA_WOODEN_DOOR,
                                AlphaBlocks.ALPHA_IRON_DOOR,
                                AlphaBlocks.ALPHA_CACTUS,
                                AlphaBlocks.ALPHA_CACTUS_FLIPPED,
                                AlphaBlocks.ALPHA_LEAVES,
                                AlphaBlocks.ALPHA_GLASS);

                BlockRenderLayerMap.putBlock(AlphaBlocks.ALPHA_ICE, ChunkSectionLayer.TRANSLUCENT);
        }
}
