package com.litehed.hytalemodels.blocks.entity;

import com.litehed.hytalemodels.HytaleModelLoader;
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
    private int openTick = 0;
    private int closeTick = 0;

    public AnimatedChestBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityInit.CHEST_TEST_ENT.get(), pos, state, "chest_small");
    }

    /**
     * Opens the chest and starts the opening animation.
     */
    public void openChest() {
        if (!isOpen) {
            isOpen = true;
            openTick = 0;

            if (level != null && level.isClientSide()) {
                long gameTime = level.getGameTime();
                HytaleModelLoader.LOGGER.debug("Client: Starting chest open animation at {}", gameTime);
            }

            setChanged();
            syncToClients();
        }
    }

    /**
     * Closes the chest and stops the opening animation.
     */
    public void closeChest() {
        if (isOpen) {
            isOpen = false;
            closeTick = 0;
            if (level != null && level.isClientSide()) {
                HytaleModelLoader.LOGGER.debug("Client: Stopping chest open animation");
            }

            setChanged();
            syncToClients();
        }
    }

    public void toggleChest() {
        if (isOpen) {
            closeChest();
        } else {
            openChest();
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
        return isOpen ? openTick : closeTick;
    }

    /**
     * Server/client tick for animation updates.
     */
    public static void tick(Level level, BlockPos pos, BlockState state, AnimatedChestBlockEntity blockEntity) {
        if (level.isClientSide()) {
            if (blockEntity.isOpen) {
                blockEntity.openTick++;
            } else {
                blockEntity.closeTick++;
            }
        }
    }

    @Override
    protected void loadAnimationData(ValueInput input) {
        input.read(NBT_KEY, CompoundTag.CODEC).ifPresent(chestTag -> {
            if (chestTag.contains(NBT_IS_OPEN)) {
                boolean wasOpen = isOpen;
                isOpen = chestTag.getBoolean(NBT_IS_OPEN).get();

                if (level != null && level.isClientSide()) {
                    if (wasOpen != isOpen && isOpen) {
                        openTick = 0;
                        HytaleModelLoader.LOGGER.debug("CLIENT: Chest opened, reset animationTick to 0");
                    }
                }
            }
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