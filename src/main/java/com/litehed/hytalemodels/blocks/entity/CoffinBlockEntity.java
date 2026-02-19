package com.litehed.hytalemodels.blocks.entity;

import com.litehed.hytalemodels.init.BlockEntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class CoffinBlockEntity extends HytaleBlockEntity {
    private boolean isOpen = false;
    private int animationTick = 0;

    public CoffinBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityInit.COFFIN_ENT.get(), pos, state, "coffin");
    }

    public void openCoffin() {
        if (!isOpen) {
            animationTick = 0;
            isOpen = true;
            setChanged();
            syncToClients();
        }
    }

    public void closeCoffin() {
        if (isOpen) {
            animationTick = 0;
            isOpen = false;
            setChanged();
            syncToClients();
        }
    }

    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public int getAnimationTick() {
        return animationTick;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state,
                                  CoffinBlockEntity be) {
        be.animationTick++;
    }

    private void syncToClients() {
        if (level != null && !level.isClientSide()) {
            level.blockEntityChanged(getBlockPos());
        }
    }

}
