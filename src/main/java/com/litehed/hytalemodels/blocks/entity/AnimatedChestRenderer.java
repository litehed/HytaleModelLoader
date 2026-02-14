package com.litehed.hytalemodels.blocks.entity;

import com.litehed.hytalemodels.blockymodel.BlockyModelGeometry;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class AnimatedChestRenderer extends HytaleBlockEntityRenderer<AnimatedChestBlockEntity, AnimatedChestRenderState> {

    private static final float ANIMATION_SPEED = 0.1f;
    private static final float MAX_LID_OFFSET = 3.0f;

    public AnimatedChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public AnimatedChestRenderState createRenderState() {
        return new AnimatedChestRenderState();
    }

    @Override
    protected void extractAdditionalRenderState(AnimatedChestBlockEntity blockEntity,
                                                AnimatedChestRenderState renderState,
                                                float partialTick) {
        renderState.isOpen = blockEntity.isOpen();
    }

    @Override
    protected Map<String, NodeTransform> calculateAnimationTransforms(AnimatedChestRenderState renderState,
                                                                      BlockyModelGeometry geometry) {
        Map<String, NodeTransform> transforms = new HashMap<>();

        float time = renderState.ageInTicks * ANIMATION_SPEED;
        float yOffset = (float) Math.sin(time) * MAX_LID_OFFSET;

        transforms.put("Lid", NodeTransform.translation(new Vector3f(0, yOffset, 0)));


        return transforms;
    }
}