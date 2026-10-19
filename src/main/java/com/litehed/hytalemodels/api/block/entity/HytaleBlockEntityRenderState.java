package com.litehed.hytalemodels.api.block.entity;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public class HytaleBlockEntityRenderState extends BlockEntityRenderState {
    public String modelName;
    public int animationTick;
    public float partialTick;
    /**
     * Animation time in blockyanim units, (animationTick + partialTick) * 4
     * Pass it to BlockyAnimationPlayer when sampling keyframes
     */
    public float ageInTicks;
    // Null if the block has no facing
    public Direction facing;
}