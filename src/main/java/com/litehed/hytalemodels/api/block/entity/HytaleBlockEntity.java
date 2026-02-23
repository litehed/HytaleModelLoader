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
     * Returns the name of the Blocky model used to render this block entity.
     * This is used to locate the {@code .blockymodel} file and the associated texture.
     *
     * @return the model name; never null or blank
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * Returns the current animation tick for this block entity.
     *
     * <p>This value is used alongside the partial tick to compute smooth animation playback.
     * Internally, it is multiplied by 4 before being passed to the animation system to match
     * Blockyanims expected animation speed. Increment this each game tick in your static
     * {@code tick()} method for continuous animations.
     *
     * @return the current animation tick; must be a non-negative integer
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
     * Override this method to load custom animation or state data from NBT/storage on world load.
     * Called automatically during {@link #loadAdditional}.
     *
     * <p>Default implementation does nothing.
     *
     * @param input the value input to read from
     */
    protected void loadAnimationData(ValueInput input) {
        // Default implementation does nothing
    }

    /**
     * Override this method to save custom animation or state data to NBT/storage for persistence.
     * Called automatically during {@link #saveAdditional}.
     *
     * <p>Default implementation does nothing.
     *
     * @param output the value output to write to
     */
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