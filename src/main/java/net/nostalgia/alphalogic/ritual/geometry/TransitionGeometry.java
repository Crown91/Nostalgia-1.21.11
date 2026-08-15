package net.nostalgia.alphalogic.ritual.geometry;

import net.minecraft.core.BlockPos;

public interface TransitionGeometry {
    BlockPos forward(BlockPos pos);
    BlockPos inverse(BlockPos pos);
    long forwardPacked(BlockPos pos);
    long inversePacked(BlockPos pos);
}
