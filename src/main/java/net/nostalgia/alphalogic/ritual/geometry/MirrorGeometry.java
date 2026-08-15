package net.nostalgia.alphalogic.ritual.geometry;

import net.minecraft.core.BlockPos;

public record MirrorGeometry(int planeY, int pivotZ) implements TransitionGeometry {
    @Override
    public BlockPos forward(BlockPos pos) {
        return new BlockPos(pos.getX(), this.planeY - pos.getY(), 2 * this.pivotZ - pos.getZ());
    }

    @Override
    public BlockPos inverse(BlockPos pos) {
        return this.forward(pos);
    }

    @Override
    public long forwardPacked(BlockPos pos) {
        return BlockPos.asLong(pos.getX(), this.planeY - pos.getY(), 2 * this.pivotZ - pos.getZ());
    }

    @Override
    public long inversePacked(BlockPos pos) {
        return this.forwardPacked(pos);
    }
}
