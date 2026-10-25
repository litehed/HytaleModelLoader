package com.litehed.hytalemodels.blocks.entity;

import com.litehed.hytalemodels.TestContent;
import com.litehed.hytalemodels.api.block.entity.HytaleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Keeps the open state of the chest and the time since it last changed
 */
public class ChestSmallBlockEntity extends HytaleBlockEntity {

    // Past the end of any animation, so a chest that was already open or closed shows its final pose
    private static final int SETTLED_TICK = 1000;

    private boolean open;
    private boolean loaded;
    private int animationTick = SETTLED_TICK;

    public ChestSmallBlockEntity(BlockPos pos, BlockState state) {
        super(TestContent.CHEST_SMALL_BE.get(), pos, state, "chest_small");
    }

    /**
     * Opens the chest if it is closed and closes it if it is open. Does nothing on the client
     */
    public void toggle() {
        if (level == null || level.isClientSide) {
            return;
        }
        open = !open;
        level.playSound(null, worldPosition, open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE,
                SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    /**
     * @return true if the chest is open
     */
    public boolean isOpen() {
        return open;
    }

    /**
     * Moves the animation on by one tick, called every client tick
     */
    public void clientTick() {
        if (animationTick < SETTLED_TICK) {
            animationTick++;
        }
    }

    @Override
    public int getAnimationTick() {
        return animationTick;
    }

    @Override
    protected void loadAnimationData(CompoundTag tag, HolderLookup.Provider registries) {
        boolean wasOpen = open;
        open = tag.getBoolean("Open");
        if (loaded && open != wasOpen) {
            animationTick = 0;
        }
        loaded = true;
    }

    @Override
    protected void saveAnimationData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("Open", open);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}