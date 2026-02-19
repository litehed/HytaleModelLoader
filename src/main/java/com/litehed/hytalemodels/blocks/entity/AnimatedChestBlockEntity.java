package com.litehed.hytalemodels.blocks.entity;

import com.litehed.hytalemodels.init.BlockEntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class AnimatedChestBlockEntity extends HytaleBlockEntity {

    private static final String NBT_KEY = "ChestData";
    private static final String NBT_IS_OPEN = "IsOpen";

    private boolean isOpen = false;
    private int animationTick = 0;
    private boolean lastKnownOpen = false;

    public AnimatedChestBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityInit.CHEST_TEST_ENT.get(), pos, state, "chest_small");
    }

    /**
     * Opens the chest and starts the opening animation.
     */
    public void openChest() {
        if (!isOpen) {
            animationTick = 0;
            isOpen = true;
            setChanged();
            syncToClients();
        }
    }

    /**
     * Closes the chest and stops the opening animation.
     */
    public void closeChest() {
        if (isOpen) {
            animationTick = 0;
            isOpen = false;
            setChanged();
            syncToClients();
        }
    }

    /**
     * Toggles the chest open/closed state.
     */

    public boolean isOpen() {
        return isOpen;
    }


    @Override
    public int getAnimationTick() {
        return animationTick;
    }

    /**
     * Server/client tick for animation updates.
     */
    public static void clientTick(Level level, BlockPos pos, BlockState state,
                                  AnimatedChestBlockEntity be) {
        be.animationTick++;
    }

    @Override
    protected void loadAnimationData(ValueInput input) {
        input.read(NBT_KEY, CompoundTag.CODEC).ifPresent(tag -> {
            boolean newOpen = tag.getBoolean(NBT_IS_OPEN).orElse(false);
            // If the state changed (server pushed an update), reset the animation tick
            if (newOpen != lastKnownOpen) {
                animationTick = 0;
                lastKnownOpen = newOpen;
            }
            isOpen = newOpen;
        });
    }

    @Override
    protected void saveAnimationData(ValueOutput output) {
        CompoundTag chestTag = new CompoundTag();
        chestTag.putBoolean(NBT_IS_OPEN, isOpen);
        output.store(NBT_KEY, CompoundTag.CODEC, chestTag);
    }

    /**
     * Sync the block entity state to clients.
     */
    private void syncToClients() {
        if (level != null && !level.isClientSide()) {
            level.blockEntityChanged(getBlockPos());
        }
    }

    @Override
    public String toString() {
        return "AnimatedChestBlockEntity{" +
                "pos=" + worldPosition +
                ", isOpen=" + isOpen + "}";
    }
}