package net.nostalgia.alphalogic.ritual.geometry;

import net.minecraft.core.BlockPos;

public record OffsetGeometry(int dx, int yOffset, int dz) implements TransitionGeometry {
    @Override
    public BlockPos forward(BlockPos pos) {
        return new BlockPos(pos.getX() + this.dx, pos.getY() - this.yOffset, pos.getZ() + this.dz);
    }

    @Override
    public BlockPos inverse(BlockPos pos) {
        return new BlockPos(pos.getX() - this.dx, pos.getY() + this.yOffset, pos.getZ() - this.dz);
    }

    @Override
    public long forwardPacked(BlockPos pos) {
        return BlockPos.asLong(pos.getX() + this.dx, pos.getY() - this.yOffset, pos.getZ() + this.dz);
    }

    @Override
    public long inversePacked(BlockPos pos) {
        return BlockPos.asLong(pos.getX() - this.dx, pos.getY() + this.yOffset, pos.getZ() - this.dz);
    }
}
