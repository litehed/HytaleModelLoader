package com.litehed.hytalemodels.api.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Render state for entities drawn from a .blockymodel
 * Everything vanilla tracks for living entities (body rotation, hurt/death state, walk animation,
 * scale, pose, invisibility) is inherited from {@link LivingEntityRenderState}
 */
public class HytaleEntityRenderState extends LivingEntityRenderState {
    public String modelNamespace; // Namespace model/animations exists in
    public String modelName; // Name of model to render
}