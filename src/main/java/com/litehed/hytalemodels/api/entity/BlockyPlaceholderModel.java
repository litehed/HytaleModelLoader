package com.litehed.hytalemodels.api.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

import java.util.List;
import java.util.Map;

/**
 * An EntityModel with no parts. LivingEntityRenderer requires a model, but Blocky geometry
 * cannot be expressed as ModelPart cubes, so it is drawn by {@link HytaleEntityRenderer} instead
 * and this model is never rendered.
 *
 * @param <S> the render state type
 */
public final class BlockyPlaceholderModel<S extends LivingEntityRenderState> extends EntityModel<S> {

    public BlockyPlaceholderModel() {
        super(new ModelPart(List.of(), Map.of()));
    }
}