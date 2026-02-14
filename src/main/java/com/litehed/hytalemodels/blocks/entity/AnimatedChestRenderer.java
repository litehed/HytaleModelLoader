package com.litehed.hytalemodels.blocks.entity;

import com.litehed.hytalemodels.blockymodel.BlockyModelGeometry;
import com.litehed.hytalemodels.blockymodel.animations.AnimationTransformCalculator;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class AnimatedChestRenderer extends HytaleBlockEntityRenderer<AnimatedChestBlockEntity, AnimatedChestRenderState> {

    private static final float ANIMATION_SPEED = 0.1f;
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

    @Override
    protected Map<String, NodeTransform> calculateAnimationTransforms(AnimatedChestRenderState renderState,
                                                                      BlockyModelGeometry geometry) {
        Map<String, NodeTransform> transforms = new HashMap<>();
//
//        BlockyModelGeometry.BlockyNode lidNode = findNodeByName(geometry, "Lid");
//        if (lidNode == null) {
//            return transforms;
//        }
//
//        Vector3f pivot = AnimationTransformCalculator.getPivotInBlockCoords(lidNode);
//
//        float time = renderState.ageInTicks * ANIMATION_SPEED;
//        float angle = (float) Math.sin(time) * MAX_LID_ANGLE;
//
//        Quaternionf rotation = new Quaternionf().rotateX((float) Math.toRadians(-angle));
//
//        transforms.put("Lid", NodeTransform.rotation(rotation));

        return transforms;
    }

    private BlockyModelGeometry.BlockyNode findNodeByName(BlockyModelGeometry geometry, String name) {
        for (BlockyModelGeometry.BlockyNode node : geometry.getNodes()) {
            BlockyModelGeometry.BlockyNode found = findNodeByNameRecursive(node, name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private BlockyModelGeometry.BlockyNode findNodeByNameRecursive(BlockyModelGeometry.BlockyNode node, String name) {
        if (node.getName().equals(name)) {
            return node;
        }
        return null;
    }
}