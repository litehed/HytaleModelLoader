package com.litehed.hytalemodels.entities;

import com.litehed.hytalemodels.api.entity.HytaleEntityRenderState;
import com.litehed.hytalemodels.api.entity.HytaleEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

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
}