package com.litehed.hytalemodels.blocks.entity;

import com.litehed.hytalemodels.HytaleModelLoader;
import com.litehed.hytalemodels.blockymodel.BlockyModelGeometry;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public class AnimatedChestRenderer extends HytaleBlockEntityRenderer<AnimatedChestBlockEntity, AnimatedChestRenderState> {

    private static final float ANIMATION_SPEED = 0.01f;
    private static final float MAX_LID_OFFSET = 3.0f;

    private static final float MAX_LID_ANGLE = 45;

    private static final Identifier ANIM_OPEN =
            Identifier.fromNamespaceAndPath(HytaleModelLoader.MODID,
                    "animations/chest_small/chest_open.blockyanim");

    private static final Identifier ANIM_CLOSE =
            Identifier.fromNamespaceAndPath(HytaleModelLoader.MODID,
                    "animations/chest_small/chest_close.blockyanim");

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

        Identifier animId = renderState.isOpen ? ANIM_OPEN : ANIM_CLOSE;
        getOrCreatePlayer(animId).calculateTransforms(renderState.ageInTicks).putAll(transforms);


        // Procedural System
//        float angle = (float) Math.sin(renderState.ageInTicks * ANIMATION_SPEED) * MAX_LID_ANGLE;
//        Quaternionf rotation = new Quaternionf().rotateX((float) Math.toRadians(-Math.abs(angle)));
//        transforms.put("Lid", NodeTransform.rotation(rotation));

        return transforms;
    }
}