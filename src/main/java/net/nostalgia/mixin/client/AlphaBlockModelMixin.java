package net.nostalgia.mixin.client;

import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.nostalgia.block.AlphaBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Swaps a handful of vanilla plant models for their alpha-era counterparts while the
 * player is inside the alpha dimension.
 *
 * 26.1's BlockStateModelSet.get(BlockState) does not exist in 1.21.11. The model lookup
 * lives on BlockRenderDispatcher.getBlockModel(BlockState) here, so that is the hook.
 *
 * The nested getBlockModel call re-enters this injector exactly once: the alpha blocks
 * are not vanilla flowers, so the second pass falls straight through to vanilla and the
 * recursion cannot go deeper.
 */
@Mixin(BlockRenderDispatcher.class)
public class AlphaBlockModelMixin {

    @Inject(method = "getBlockModel", at = @At("HEAD"), cancellable = true)
    private void nostalgia$swapAlphaModels(BlockState state, CallbackInfoReturnable<BlockStateModel> cir) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null || mc.level == null) {
            return;
        }
        if (mc.level.dimension() != net.nostalgia.world.dimension.ModDimensions.ALPHA_112_01_LEVEL_KEY) {
            return;
        }

        BlockRenderDispatcher self = (BlockRenderDispatcher)(Object)this;
        if (state.is(Blocks.DANDELION)) {
            cir.setReturnValue(self.getBlockModel(AlphaBlocks.ALPHA_YELLOW_FLOWER.defaultBlockState()));
        } else if (state.is(Blocks.POPPY)) {
            cir.setReturnValue(self.getBlockModel(AlphaBlocks.ALPHA_RED_FLOWER.defaultBlockState()));
        } else if (state.is(Blocks.SUGAR_CANE)) {
            cir.setReturnValue(self.getBlockModel(AlphaBlocks.ALPHA_SUGAR_CANE.defaultBlockState()));
        } else if (state.is(Blocks.COBWEB)) {
            cir.setReturnValue(self.getBlockModel(AlphaBlocks.ALPHA_COBWEB.defaultBlockState()));
        } else if (state.is(Blocks.OAK_SAPLING)) {
            cir.setReturnValue(self.getBlockModel(AlphaBlocks.ALPHA_SAPLING.defaultBlockState()));
        }
    }
}
