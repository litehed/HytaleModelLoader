package com.litehed.hytalemodels.blocks.entity;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class AnimatedChestRenderState extends BlockEntityRenderState {

    // Animation data
    public String modelName;
    public boolean isOpen;
    public int animationTick;

    // Timing for animation
    public float ageInTicks;
    public float partialTick;

    public AnimatedChestRenderState() {
        this.modelName = null;
        this.isOpen = false;
        this.animationTick = 0;
        this.ageInTicks = 0;
        this.partialTick = 0;
    }
}