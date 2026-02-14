package com.litehed.hytalemodels.blocks.entity;

import com.litehed.hytalemodels.HytaleModelLoader;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.world.phys.Vec3;

public class AnimatedBlockEntityRenderer implements BlockEntityRenderer<AnimatedChestBlockEntity, AnimatedChestRenderState> {

    private final MaterialSet materials;

    public AnimatedBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.materials = context.materials();
    }

    @Override
    public AnimatedChestRenderState createRenderState() {
        return new AnimatedChestRenderState();
    }

    @Override
    public void extractRenderState(AnimatedChestBlockEntity blockEntity, AnimatedChestRenderState renderState, float partialTick, Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);

        renderState.modelName = blockEntity.getModelName();
        renderState.isOpen = blockEntity.isOpen();
        renderState.animationTick = blockEntity.getAnimationTick();
        renderState.partialTick = partialTick;

        if (blockEntity.getLevel() != null) {
            renderState.ageInTicks = blockEntity.getLevel().getGameTime() + partialTick;
        }
    }


    @Override
    public void submit(AnimatedChestRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        HytaleModelLoader.LOGGER.debug("MEOW");
    }
}
