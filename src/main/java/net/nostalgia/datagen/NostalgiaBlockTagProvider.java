package net.nostalgia.datagen;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.nostalgia.block.AlphaBlocks;

public class NostalgiaBlockTagProvider extends FabricTagProvider.BlockTagProvider {

    public NostalgiaBlockTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_PICKAXE)
            .add(AlphaBlocks.ALPHA_STONE_KEY)
            .add(AlphaBlocks.ALPHA_COBBLESTONE_KEY)
            .add(AlphaBlocks.ALPHA_MOSSY_COBBLESTONE_KEY)
            .add(AlphaBlocks.ALPHA_COAL_ORE_KEY)
            .add(AlphaBlocks.ALPHA_IRON_ORE_KEY)
            .add(AlphaBlocks.ALPHA_GOLD_ORE_KEY)
            .add(AlphaBlocks.ALPHA_REDSTONE_ORE_KEY)
            .add(AlphaBlocks.ALPHA_DIAMOND_ORE_KEY)
            .add(AlphaBlocks.ALPHA_OBSIDIAN_KEY)
            .add(AlphaBlocks.ALPHA_FURNACE_KEY);

        getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_AXE)
            .add(AlphaBlocks.ALPHA_OAK_LOG_KEY)
            .add(AlphaBlocks.ALPHA_OAK_PLANKS_KEY)
            .add(AlphaBlocks.ALPHA_BOOKSHELF_KEY)
            .add(AlphaBlocks.ALPHA_CHEST_KEY)
            .add(AlphaBlocks.ALPHA_CRAFTING_TABLE_KEY);

        getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_SHOVEL)
            .add(AlphaBlocks.ALPHA_DIRT_KEY)
            .add(AlphaBlocks.ALPHA_GRASS_BLOCK_KEY)
            .add(AlphaBlocks.ALPHA_FARMLAND_KEY)
            .add(AlphaBlocks.ALPHA_SAND_KEY)
            .add(AlphaBlocks.ALPHA_GRAVEL_KEY)
            .add(AlphaBlocks.ALPHA_CLAY_KEY)
            .add(AlphaBlocks.ALPHA_SNOW_BLOCK_KEY);

        getOrCreateTagBuilder(BlockTags.NEEDS_STONE_TOOL)
            .add(AlphaBlocks.ALPHA_IRON_ORE_KEY);

        getOrCreateTagBuilder(BlockTags.NEEDS_IRON_TOOL)
            .add(AlphaBlocks.ALPHA_REDSTONE_ORE_KEY)
            .add(AlphaBlocks.ALPHA_DIAMOND_ORE_KEY);

        getOrCreateTagBuilder(BlockTags.NEEDS_DIAMOND_TOOL)
            .add(AlphaBlocks.ALPHA_OBSIDIAN_KEY);

        getOrCreateTagBuilder(BlockTags.CLIMBABLE)
            .add(AlphaBlocks.ALPHA_LADDER_KEY);

        getOrCreateTagBuilder(BlockTags.LOGS)
            .add(AlphaBlocks.ALPHA_OAK_LOG_KEY);

        getOrCreateTagBuilder(BlockTags.PLANKS)
            .add(AlphaBlocks.ALPHA_OAK_PLANKS_KEY);

        getOrCreateTagBuilder(BlockTags.LEAVES)
            .add(AlphaBlocks.ALPHA_LEAVES_KEY);

        getOrCreateTagBuilder(BlockTags.SAND)
            .add(AlphaBlocks.ALPHA_SAND_KEY);

        // BlockTags.SUPPORTS_CROPS does not exist in 1.21.11 - crop placement is
        // decided by the block itself, so the tag entry is dropped here.
    }
}
