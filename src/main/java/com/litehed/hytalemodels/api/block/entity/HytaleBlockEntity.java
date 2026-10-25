package com.litehed.hytalemodels.api.block.entity;

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

    /**
     * Name of the model, used to find the .blockymodel file and its texture
     *
     * @return the model name
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * Animation tick of this block entity
     * Increase it every game tick, for example from your ticker
     * The renderer multiplies it by 4 to match blockyanim speed
     *
     * @return the current animation tick
     */
    public abstract int getAnimationTick();

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

    /**
     * Override to load your own animation state, called from loadAdditional
     *
     * @param input the value input to read from
     */
    protected void loadAnimationData(ValueInput input) {
    }

    /**
     * Override to save your own animation state, called from saveAdditional
     *
     * @param output the value output to write to
     */
    protected void saveAnimationData(ValueOutput output) {
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "modelName='" + modelName + '\'' +
                ", pos=" + worldPosition +
                "}";
    }
}