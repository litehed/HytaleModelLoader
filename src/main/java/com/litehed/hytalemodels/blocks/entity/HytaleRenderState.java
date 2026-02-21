package com.litehed.hytalemodels.blocks.entity;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public class HytaleRenderState extends BlockEntityRenderState {
    public String modelName;
    public int animationTick; // Animation tick
    public float partialTick; // Partial tick for smooth animation
    public float ageInTicks; // Smoothed animation time in ticks sped 4x to work with blockyanim
    public Direction facing; // Block facing direction for rotation
}
