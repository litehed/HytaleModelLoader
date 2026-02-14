package com.litehed.hytalemodels.blocks.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class HytaleBlockEntity extends BlockEntity {

    private final String modelName;


    public HytaleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, String modelName) {
        super(type, pos, state);
        this.modelName = modelName;
    }


    public String getModelName() {
        return modelName;
    }

    public int getAnimationTick() {
        return 0;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        loadAnimationData(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        saveAnimationData(output);
    }


    protected void loadAnimationData(ValueInput input) {
        // Default implementation does nothing
    }

    protected void saveAnimationData(ValueOutput output) {
        // Default implementation does nothing
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "modelName='" + modelName + '\'' +
                ", pos=" + worldPosition +
                "}";
    }
}