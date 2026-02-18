package com.litehed.hytalemodels.blocks.entity;

import com.litehed.hytalemodels.HytaleModelLoader;
import com.litehed.hytalemodels.blockymodel.BlockyModelGeometry;
import com.litehed.hytalemodels.blockymodel.animations.BlockyAnimationDefinition;
import com.litehed.hytalemodels.blockymodel.animations.BlockyAnimationLoader;
import com.litehed.hytalemodels.blockymodel.animations.BlockyAnimationPlayer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public class AnimatedChestRenderer extends HytaleBlockEntityRenderer<AnimatedChestBlockEntity, AnimatedChestRenderState> {

    private static final float ANIMATION_SPEED = 0.01f;
    private static final float MAX_LID_OFFSET = 3.0f;

    private static final float MAX_LID_ANGLE = 45;

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

    private Identifier getAnimationFile(AnimatedChestRenderState renderState) {
        if (renderState.isOpen) {
            return Identifier.fromNamespaceAndPath(HytaleModelLoader.MODID, "animations/chest_small/chest_open.blockyanim");
        }
        return Identifier.fromNamespaceAndPath(HytaleModelLoader.MODID, "animations/chest_small/chest_close.blockyanim");
    }

    @Override
    protected Map<String, NodeTransform> calculateAnimationTransforms(AnimatedChestRenderState renderState,
                                                                      BlockyModelGeometry geometry) {
        // For use of basic animations like base minecraft system
//        Map<String, NodeTransform> transforms = new HashMap<>();
//        float time = renderState.ageInTicks * ANIMATION_SPEED;
//        float angle = (float) Math.sin(time) * MAX_LID_ANGLE;
//        Quaternionf rotation = new Quaternionf().rotateX((float) Math.toRadians(-Math.abs(angle)));
//        transforms.put("Lid", NodeTransform.rotation(rotation));
//
//        BlockyModelGeometry.BlockyNode lidNode = findNodeByName(geometry, "Lid");
//        if (lidNode != null) {
//            applyTransformToDescendantsById(lidNode, rotation, transforms);
//        }
//
//        return transforms;

        // For use of an animation file
        Identifier animationFile = getAnimationFile(renderState);
        try {
            BlockyAnimationDefinition definition = BlockyAnimationLoader.INSTANCE.loadAnimation(animationFile);

            if (definition == null) {
                HytaleModelLoader.LOGGER.warn("Animation definition is null for: {}", animationFile);
                return new HashMap<>();
            }
            HytaleModelLoader.LOGGER.debug("Animation loaded - duration: {}, ageInTicks: {}",
                    definition.getDuration(), renderState.ageInTicks);

            BlockyAnimationPlayer player = new BlockyAnimationPlayer(definition);
            return player.calculateTransforms(renderState.ageInTicks);
        } catch (Exception e) {
            HytaleModelLoader.LOGGER.error("Error playing animation: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}