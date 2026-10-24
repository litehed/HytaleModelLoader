package com.litehed.hytalemodels.api.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

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
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadAnimationData(tag, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveAnimationData(tag, registries);
    }

    /**
     * Override to load your own animation state, called from loadAdditional
     *
     * @param tag        the data to read from
     * @param registries the registries, for data that needs them
     */
    protected void loadAnimationData(CompoundTag tag, HolderLookup.Provider registries) {
    }

    /**
     * Override to save your own animation state, called from saveAdditional
     *
     * @param tag        the data to write to
     * @param registries the registries, for data that needs them
     */
    protected void saveAnimationData(CompoundTag tag, HolderLookup.Provider registries) {
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "modelName='" + modelName + '\'' +
                ", pos=" + worldPosition +
                "}";
    }
}