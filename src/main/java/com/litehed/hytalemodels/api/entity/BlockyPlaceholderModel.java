package com.litehed.hytalemodels.api.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

import java.util.List;
import java.util.Map;

/**
 * An empty model. Vanilla needs one, but Blocky models are drawn by {@link HytaleEntityRenderer}
 *
 * @param <S> the render state type
 */
public final class BlockyPlaceholderModel<S extends LivingEntityRenderState> extends EntityModel<S> {

    public BlockyPlaceholderModel() {
        super(new ModelPart(List.of(), Map.of()));
    }
}