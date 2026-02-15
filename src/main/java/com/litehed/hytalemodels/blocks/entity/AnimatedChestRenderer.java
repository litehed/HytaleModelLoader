package com.litehed.hytalemodels.blocks.entity;

import com.litehed.hytalemodels.blockymodel.BlockyModelGeometry;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimatedChestRenderer extends HytaleBlockEntityRenderer<AnimatedChestBlockEntity, AnimatedChestRenderState> {

    private static final float ANIMATION_SPEED = 0.05f;
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
        float time = renderState.ageInTicks * ANIMATION_SPEED;
        float angle = (float) Math.sin(time) * MAX_LID_ANGLE;
        Quaternionf rotation = new Quaternionf().rotateX((float) Math.toRadians(-Math.abs(angle)));
        transforms.put("Lid", NodeTransform.rotation(rotation));

        BlockyModelGeometry.BlockyNode lidNode = findNodeByName(geometry, "Lid");
        if (lidNode != null) {
            applyTransformToDescendantsById(lidNode, rotation, transforms);
        }

        return transforms;
    }

    private void applyTransformToDescendantsById(BlockyModelGeometry.BlockyNode node,
                                                 Quaternionf rotation,
                                                 Map<String, NodeTransform> transforms) {
        for (BlockyModelGeometry.BlockyNode child : node.getChildren()) {
            transforms.put(child.getId(), NodeTransform.rotation(rotation));
            applyTransformToDescendantsById(child, rotation, transforms);
        }
    }

    private BlockyModelGeometry.BlockyNode findNodeByName(BlockyModelGeometry geometry, String name) {
        return findNodeByNameRecursive(geometry.getNodes(), name);
    }

    private BlockyModelGeometry.BlockyNode findNodeByNameRecursive(List<BlockyModelGeometry.BlockyNode> nodes, String name) {
        for (BlockyModelGeometry.BlockyNode node : nodes) {
            if (node.getName().equals(name)) {
                return node;
            }
            BlockyModelGeometry.BlockyNode found = findNodeByNameRecursive(node.getChildren(), name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}