package com.litehed.hytalemodels.blocks.entity;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class HytaleRenderState extends BlockEntityRenderState {
    public String modelName;
    public int animationTick;
    public float ageInTicks; // Smoothed animation time in ticks sped 4x to work with blockyanim
    public float partialTick;
}
