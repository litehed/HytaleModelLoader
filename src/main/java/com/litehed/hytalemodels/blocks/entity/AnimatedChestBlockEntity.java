package com.litehed.hytalemodels.blocks.entity;

import com.litehed.hytalemodels.HytaleModelLoader;
import com.litehed.hytalemodels.init.BlockEntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class AnimatedChestBlockEntity extends HytaleBlockEntity {

    private static final String NBT_KEY = "ChestData";
    private static final String NBT_IS_OPEN = "IsOpen";
    private static final String NBT_ANIM_TICK = "AnimationTick";

    private boolean isOpen = false;
    private final AnimationState openAnimationState = new AnimationState();
    private int animationTick = 0;

    public AnimatedChestBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityInit.CHEST_TEST_ENT.get(), pos, state, "chest_small");
    }

    /**
     * Opens the chest and starts the opening animation.
     */
    public void openChest() {
        if (!isOpen) {
            isOpen = true;

            if (level != null && level.isClientSide()) {
                long gameTime = level.getGameTime();
                openAnimationState.start((int) gameTime);
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

            if (level != null && level.isClientSide()) {
                openAnimationState.stop();
                HytaleModelLoader.LOGGER.debug("Client: Stopping chest open animation");
            }

            setChanged();
            syncToClients();
        }
    }

    /**
     * Toggles the chest open/closed state.
     */
    public void toggleChest() {
        if (isOpen) {
            closeChest();
        } else {
            openChest();
        }
    }

    public boolean isOpen() {
        return isOpen;
    }

    public AnimationState getOpenAnimationState() {
        return openAnimationState;
    }

    @Override
    public int getAnimationTick() {
        return animationTick;
    }

    /**
     * Server/client tick for animation updates.
     */
    public static void tick(Level level, BlockPos pos, BlockState state, AnimatedChestBlockEntity blockEntity) {
        if (level.isClientSide()) {
            if (blockEntity.isOpen) {
                blockEntity.animationTick++;
            } else {
                if (blockEntity.animationTick > 0) {
                    blockEntity.animationTick--;
                }
            }
        }
    }

    @Override
    protected void loadAnimationData(ValueInput input) {
        input.read(NBT_KEY, CompoundTag.CODEC).ifPresent(chestTag -> {
            if (chestTag.contains(NBT_IS_OPEN)) {
                boolean wasOpen = isOpen;
                isOpen = chestTag.getBoolean(NBT_IS_OPEN).get();

                // Update animation state on client
                if (level != null && level.isClientSide() && wasOpen != isOpen) {
                    if (isOpen) {
                        openAnimationState.start((int) level.getGameTime());
                    } else {
                        openAnimationState.stop();
                    }
                }
            }

            if (chestTag.contains(NBT_ANIM_TICK)) {
                animationTick = chestTag.getInt(NBT_ANIM_TICK).get();
            }
        });
    }

    @Override
    protected void saveAnimationData(ValueOutput output) {
        CompoundTag chestTag = new CompoundTag();
        chestTag.putBoolean(NBT_IS_OPEN, isOpen);
        chestTag.putInt(NBT_ANIM_TICK, animationTick);
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
                ", isOpen=" + isOpen +
                ", animationTick=" + animationTick +
                "}";
    }
}