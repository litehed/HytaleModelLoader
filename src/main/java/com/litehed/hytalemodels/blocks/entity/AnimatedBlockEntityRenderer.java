package com.litehed.hytalemodels.blocks.entity;

import com.litehed.hytalemodels.HytaleModelLoader;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public class AnimatedBlockEntityRenderer implements BlockEntityRenderer<AnimatedChestBlockEntity, AnimatedChestRenderState> {

    private final MaterialSet materials;
    private final EntityModelSet entityModelSet;

    private static final Identifier CHEST_OPEN_ANIM =
            Identifier.fromNamespaceAndPath(HytaleModelLoader.MODID, "animations/chest_small/chest_open.blockyanim");

    public AnimatedBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.materials = context.materials();
        this.entityModelSet = context.entityModelSet();
    }

    @Override
    public AnimatedChestRenderState createRenderState() {
        return new AnimatedChestRenderState();
    }

    @Override
    public void extractRenderState(AnimatedChestBlockEntity blockEntity, AnimatedChestRenderState renderState, float partialTick, Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
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
        if (renderState.modelName == null) {
            return;
        }
    }
}
