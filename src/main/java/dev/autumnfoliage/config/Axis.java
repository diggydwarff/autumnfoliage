package dev.autumnfoliage.config;

import net.minecraft.core.BlockPos;

public enum Axis {
    X,
    Z;

    public int coordinate(BlockPos pos) {
        return this == X ? pos.getX() : pos.getZ();
    }
}
