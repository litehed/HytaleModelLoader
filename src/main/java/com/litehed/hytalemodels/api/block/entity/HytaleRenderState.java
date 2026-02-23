package com.litehed.hytalemodels.api.block.entity;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public class HytaleRenderState extends BlockEntityRenderState {
    public String modelName;
    public int animationTick; // Animation tick
    public float partialTick; // Partial tick for smooth animation
    /**
     * The smoothed animation time in ticks, pre-multiplied by 4 to match blockyanim internal
     * animation speed expectations. Computed as {@code (animationTick + partialTick) * 4}.
     * Pass this value directly to {@code BlockyAnimationPlayer} when sampling keyframes.
     */
    public float ageInTicks;
    public Direction facing; // Block facing direction for rotation
}
