package com.litehed.hytalemodels.entities;

import com.litehed.hytalemodels.api.BlockyPose;
import com.litehed.hytalemodels.api.entity.HytaleEntityRenderState;
import com.litehed.hytalemodels.api.entity.HytaleEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

public class GoblinRenderer extends HytaleEntityRenderer<GoblinEntity, HytaleEntityRenderState> {

    // The model is about 2.6 blocks tall at full scale; 0.6 gives roughly 1.6 blocks
    private static final float SCALE = 0.6f;
    private static final float SHADOW_RADIUS = 0.4f;

    public GoblinRenderer(EntityRendererProvider.Context context) {
        super(context, SHADOW_RADIUS);
    }

    @Override
    public HytaleEntityRenderState createRenderState() {
        return new HytaleEntityRenderState();
    }

    @Override
    protected String getModelName(GoblinEntity entity) {
        return "goblin";
    }

    @Override
    protected float getModelScale() {
        return SCALE;
    }

    @Override
    protected void setupAnim(HytaleEntityRenderState state, BlockyPose pose) {
        // Keyframed idle from animations/goblin/idle.blockyanim, looping
        pose.play(getAnimation(state, "idle"), state.ageInTicks * ANIMATION_UNITS_PER_TICK);

        // Minecraft-style procedural motion stacked on top: head look and a sin-based leg swing
        pose.rotateY("Head", state.yRot * Mth.DEG_TO_RAD);
        pose.rotateX("Head", state.xRot * Mth.DEG_TO_RAD);

        float swing = state.walkAnimationPos * 0.6662f;
        float amount = Math.min(state.walkAnimationSpeed, 1.0f);
        pose.rotateX("R-Thigh", Mth.cos(swing) * 1.4f * amount);
        pose.rotateX("L-Thigh", Mth.cos(swing + Mth.PI) * 1.4f * amount);
    }
}