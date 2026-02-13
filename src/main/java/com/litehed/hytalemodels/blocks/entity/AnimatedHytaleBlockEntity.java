package com.litehed.hytalemodels.blocks.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class AnimatedHytaleBlockEntity extends BlockEntity {
    private final String modelName;

    public AnimatedHytaleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, String modelName) {
        super(type, pos, state);
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }

    @Override
    public String toString() {
        return "AnimatedHytaleBlockEntity{" +
                "modelName='" + modelName + '\'' +
                ", pos=" + this.worldPosition +
                "}";
    }
}
