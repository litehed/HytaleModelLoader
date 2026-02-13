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

public class AnimatedChestBlockEntity extends AnimatedHytaleBlockEntity {

    // Track whether chest is open (for server-side state)
    private boolean isOpen = false;
    private final AnimationState openAnimationState = new AnimationState();
    private int animationTick = 0;

    // Animation name for opening
    private static final String OPEN_ANIMATION = "chest_open";

    // Animation name for closing (if we have one)
    private static final String CLOSE_ANIMATION = "chest_close";

    public AnimatedChestBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityInit.CHEST_TEST_ENT.get(), pos, state, "chest_small");
    }

    public void openChest() {
        if (!this.isOpen) {
            this.isOpen = true;

            // Start the animation on client side
            if (this.level != null && this.level.isClientSide()) {
                long gameTime = this.level.getGameTime();
                this.openAnimationState.start((int) gameTime);
                HytaleModelLoader.LOGGER.info("Client: Starting open animation at game time: {}", gameTime);
            }

            HytaleModelLoader.LOGGER.info("Chest opened at position: {}", this.worldPosition);
            this.setChanged();

            // Sync to clients
            if (this.level != null && !this.level.isClientSide()) {
                this.level.blockEntityChanged(this.getBlockPos());
                HytaleModelLoader.LOGGER.info("Server: Syncing open animation to clients");
            }
        }
    }

    public void closeChest() {
        if (this.isOpen) {
            this.isOpen = false;

            // Stop the animation on client side
            if (this.level != null && this.level.isClientSide()) {
                this.openAnimationState.stop();
                HytaleModelLoader.LOGGER.info("Client: Stopping open animation");
            }

            HytaleModelLoader.LOGGER.info("Chest closed at position: {}", this.worldPosition);
            this.setChanged();

            // Sync to clients
            if (this.level != null && !this.level.isClientSide()) {
                this.level.blockEntityChanged(this.getBlockPos());
                HytaleModelLoader.LOGGER.info("Server: Syncing close animation to clients");
            }
        }
    }


    public boolean isOpen() {
        return isOpen;
    }

    public AnimationState getOpenAnimationState() {
        return openAnimationState;
    }

    public int getAnimationTick() {
        return animationTick;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AnimatedChestBlockEntity blockEntity) {
        if (level.isClientSide()) {
            // Update animation tick counter
            if (blockEntity.isOpen) {
                blockEntity.animationTick++;
            } else {
                // Optionally decay animation tick when closed
                if (blockEntity.animationTick > 0) {
                    blockEntity.animationTick--;
                }
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("ChestData", CompoundTag.CODEC).ifPresent(chestTag -> {
            if (chestTag.contains("IsOpen")) {
                boolean wasOpen = this.isOpen;
                this.isOpen = chestTag.getBoolean("IsOpen").get();

                // If state changed, update animation on client
                if (this.level != null && this.level.isClientSide() && wasOpen != this.isOpen) {
                    if (this.isOpen) {
                        this.openAnimationState.start((int) this.level.getGameTime());
                    } else {
                        this.openAnimationState.stop();
                    }
                }
            }
            if (chestTag.contains("AnimationTick")) {
                this.animationTick = chestTag.getInt("AnimationTick").get();
            }
        });
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag chestTag = new CompoundTag();
        chestTag.putBoolean("isOpen", isOpen);
        chestTag.putInt("AnimationTick", animationTick);
        output.store("ChestData", CompoundTag.CODEC, chestTag);
    }

    @Override
    public String toString() {
        return "AnimatedChestBlockEntity{" +
                "pos=" + this.worldPosition +
                ", isOpen=" + isOpen +
                "}";
    }
}