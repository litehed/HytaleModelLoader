package com.litehed.hytalemodels.blockymodel;

import net.minecraft.client.resources.model.UnbakedGeometry;
import net.neoforged.neoforge.client.model.AbstractUnbakedModel;
import net.neoforged.neoforge.client.model.StandardModelParameters;
import org.jspecify.annotations.Nullable;

public class BlockyModel extends AbstractUnbakedModel {

    private final BlockyModelGeometry geometry;

    /**
     * Constructor for BlockyModel
     *
     * @param parameters the standard model parameters
     * @param geometry   the blocky model geometry
     */
    protected BlockyModel(StandardModelParameters parameters, BlockyModelGeometry geometry) {
        super(parameters);
        this.geometry = geometry;
    }

    @Override
    public @Nullable UnbakedGeometry geometry() {
        return geometry;
    }
}
