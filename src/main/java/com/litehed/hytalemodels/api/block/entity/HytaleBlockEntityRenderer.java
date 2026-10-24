package com.litehed.hytalemodels.api.block.entity;

import com.litehed.hytalemodels.HytaleModelLoader;
import com.litehed.hytalemodels.api.NodeTransform;
import com.litehed.hytalemodels.api.block.HytaleBlockBase;
import com.litehed.hytalemodels.blockymodel.BlockyModelGeometry;
import com.litehed.hytalemodels.blockymodel.BlockyModelLoader;
import com.litehed.hytalemodels.blockymodel.QuadBuilder;
import com.litehed.hytalemodels.blockymodel.TransformCalculator;
import com.litehed.hytalemodels.blockymodel.animations.BlockyAnimationLoader;
import com.litehed.hytalemodels.blockymodel.animations.BlockyAnimationPlayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class HytaleBlockEntityRenderer<T extends HytaleBlockEntity> implements BlockEntityRenderer<T> {

    // Blockyanim time units that pass per animation tick
    private static final float ANIMATION_UNITS_PER_TICK = 4.0f;

    private final Map<ResourceLocation, Optional<BlockyModelGeometry>> geometryCache = new HashMap<>();
    private final Map<ResourceLocation, Optional<BlockyAnimationPlayer>> playerCache = new HashMap<>();

    public HytaleBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        String modelName = blockEntity.getModelName();
        if (modelName == null) {
            return;
        }

        String namespace = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()).getNamespace();

        ResourceLocation modelLocation = getModelLocation(namespace, modelName);
        BlockyModelGeometry geometry = getOrLoadGeometry(modelLocation);
        if (geometry == null) {
            return;
        }

        TextureAtlasSprite sprite = getTextureMaterial(namespace, modelName).sprite();

        BlockState blockState = blockEntity.getBlockState();
        Direction facing = blockState.hasProperty(HytaleBlockBase.FACING) ? blockState.getValue(HytaleBlockBase.FACING) : null;

        float ageInTicks = (blockEntity.getAnimationTick() + partialTick) * ANIMATION_UNITS_PER_TICK;
        Map<String, NodeTransform> nodeTransforms = calculateAnimationTransforms(blockEntity, ageInTicks, geometry);

        VertexConsumer buffer = bufferSource.getBuffer(getRenderType());

        poseStack.pushPose();

        poseStack.translate(0.5, 0.5, 0.5);

        float yRotDegrees = getFacingYRotation(facing);
        if (yRotDegrees != 0f) {
            poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(yRotDegrees)));
        }

        List<BlockyModelGeometry.BlockyNode> nodes = geometry.getNodes();
        for (BlockyModelGeometry.BlockyNode node : nodes) {
            if (node.hasShape()) {
                renderNode(poseStack, buffer, node, sprite, nodeTransforms, packedLight);
            }
        }

        poseStack.popPose();
    }

    /**
     * Y rotation in degrees for a facing. North is 180, east 90, west 270 and south 0
     *
     * @param facing the facing of the block, null if it has none
     * @return the rotation in degrees
     */
    protected float getFacingYRotation(Direction facing) {
        if (facing == null) {
            return 0f;
        }
        return switch (facing) {
            case NORTH -> 180f;
            case EAST -> 90f;
            case WEST -> 270f;
            default -> 0f;
        };
    }

    /**
     * Works out the transform of each node for this frame
     *
     * @param blockEntity the block entity being rendered
     * @param ageInTicks  the animation time in blockyanim units
     * @param geometry    the model being rendered
     * @return transforms keyed by node id or name
     */
    protected abstract Map<String, NodeTransform> calculateAnimationTransforms(T blockEntity, float ageInTicks,
                                                                               BlockyModelGeometry geometry);

    /**
     * Location of the model file
     *
     * @param namespace the namespace of the block entity's mod
     * @param modelName the name of the model
     * @return the location of the .blockymodel file
     */
    protected ResourceLocation getModelLocation(String namespace, String modelName) {
        return ResourceLocation.fromNamespaceAndPath(namespace, "models/" + modelName + ".blockymodel");
    }

    /**
     * Texture used by the model
     *
     * @param namespace the namespace of the block entity's mod
     * @param modelName the name of the model
     * @return the texture material
     */
    protected Material getTextureMaterial(String namespace, String modelName) {
        return new Material(TextureAtlas.LOCATION_BLOCKS,
                ResourceLocation.fromNamespaceAndPath(namespace, "block/" + modelName + "_texture"));
    }

    private void renderNode(PoseStack poseStack, VertexConsumer buffer,
                            BlockyModelGeometry.BlockyNode node, TextureAtlasSprite sprite,
                            Map<String, NodeTransform> nodeTransforms, int lightCoords) {
        NodeTransform transform = findTransform(nodeTransforms, node);
        if (transform != null && !transform.visible()) return;

        poseStack.pushPose();

        applyNodeTransform(poseStack, node, nodeTransforms);

        BlockyModelGeometry.BlockyShape shape = node.getShape();

        Vector3f halfSizes = TransformCalculator.calculateHalfSizes(shape.getSize());
        Vector3f min = new Vector3f(-halfSizes.x, -halfSizes.y, -halfSizes.z);
        Vector3f max = new Vector3f(halfSizes.x, halfSizes.y, halfSizes.z);

        for (Direction direction : Direction.values()) {
            if (!shape.hasTextureLayout(direction)) {
                continue;
            }

            BlockyModelGeometry.FaceTextureLayout texLayout = shape.getTextureLayout(direction);
            boolean shouldReverse = shape.needsWindingReversal();

            renderQuad(buffer, poseStack.last(), direction, min, max, sprite, texLayout,
                    shape.getOriginalSize(), lightCoords, shouldReverse);

            // Render backface if double-sided
            if (shape.isDoubleSided()) {
                renderQuad(buffer, poseStack.last(), direction, min, max, sprite, texLayout,
                        shape.getOriginalSize(), lightCoords, !shouldReverse);
            }
        }

        poseStack.popPose();
    }

    /**
     * Render type used to draw the model
     *
     * @return the render type
     */
    protected RenderType getRenderType() {
        return RenderType.cutout();
    }

    private void applyNodeTransform(PoseStack poseStack, BlockyModelGeometry.BlockyNode node,
                                    Map<String, NodeTransform> effectiveTransforms) {
        Vector3f worldPos = TransformCalculator.calculateWorldPosition(node);
        Quaternionf worldRot = TransformCalculator.calculateWorldOrientation(node);

        Vector3f shapeOffset = node.getShape().getOffset();
        Vector3f rotatedOffset = new Vector3f(shapeOffset);
        worldRot.transform(rotatedOffset);

        float centerX = (worldPos.x + rotatedOffset.x) / TransformCalculator.UNITS_PER_BLOCK;
        float centerY = ((worldPos.y + rotatedOffset.y) - TransformCalculator.ORIGIN_Y_OFFSET) / TransformCalculator.UNITS_PER_BLOCK;
        float centerZ = (worldPos.z + rotatedOffset.z) / TransformCalculator.UNITS_PER_BLOCK;

        NodeTransform effectiveTransform = findTransform(effectiveTransforms, node);

        NodeTransform parentAnimTransform = getParentAnimationTransform(node, effectiveTransforms);

        boolean hasOwnAnimation = effectiveTransform != null && !effectiveTransform.isIdentity();
        boolean parentHasAnimation = parentAnimTransform != null && !parentAnimTransform.isIdentity();

        if (hasOwnAnimation || parentHasAnimation) {
            Vector3f parentPivot = null;
            BlockyModelGeometry.BlockyNode parent = node.getParent();
            if (parent != null && findTransform(effectiveTransforms, parent) != null) {
                Vector3f parentWorldPos = TransformCalculator.calculateWorldPosition(parent);
                parentPivot = new Vector3f(
                        parentWorldPos.x / TransformCalculator.UNITS_PER_BLOCK,
                        (parentWorldPos.y - TransformCalculator.ORIGIN_Y_OFFSET) / TransformCalculator.UNITS_PER_BLOCK,
                        parentWorldPos.z / TransformCalculator.UNITS_PER_BLOCK
                );
            }

            float childPivotX = worldPos.x / TransformCalculator.UNITS_PER_BLOCK;
            float childPivotY = (worldPos.y - TransformCalculator.ORIGIN_Y_OFFSET) / TransformCalculator.UNITS_PER_BLOCK;
            float childPivotZ = worldPos.z / TransformCalculator.UNITS_PER_BLOCK;

            if (parentHasAnimation && parentPivot != null) {
                poseStack.translate(parentPivot.x, parentPivot.y, parentPivot.z);

                poseStack.mulPose(parentAnimTransform.rotation());
                Vector3f parentPos = parentAnimTransform.position();
                poseStack.translate(parentPos.x, parentPos.y, parentPos.z);

                poseStack.translate(-parentPivot.x, -parentPivot.y, -parentPivot.z);
            }

            if (hasOwnAnimation) {
                poseStack.translate(childPivotX, childPivotY, childPivotZ);

                poseStack.mulPose(effectiveTransform.rotation());
                Vector3f childAnimPos = effectiveTransform.position();
                poseStack.translate(childAnimPos.x, childAnimPos.y, childAnimPos.z);

                poseStack.translate(-childPivotX, -childPivotY, -childPivotZ);
            }

            poseStack.translate(centerX, centerY, centerZ);
            poseStack.mulPose(worldRot);

            Vector3f animScale = effectiveTransform != null ? effectiveTransform.scale() : new Vector3f(1, 1, 1);
            poseStack.scale(animScale.x, animScale.y, animScale.z);
        } else {
            // No animation
            poseStack.translate(centerX, centerY, centerZ);
            poseStack.mulPose(worldRot);
        }
    }

    private NodeTransform getParentAnimationTransform(BlockyModelGeometry.BlockyNode node,
                                                      Map<String, NodeTransform> effectiveTransforms) {
        BlockyModelGeometry.BlockyNode current = node.getParent();

        while (current != null) {
            NodeTransform transform = findTransform(effectiveTransforms, current);
            if (transform != null && !transform.isIdentity()) {
                return transform;
            }
            current = current.getParent();
        }

        return null;
    }

    private static NodeTransform findTransform(Map<String, NodeTransform> transforms,
                                               BlockyModelGeometry.BlockyNode node) {
        NodeTransform transform = transforms.get(node.getId());
        return transform != null ? transform : transforms.get(node.getName());
    }

    private void renderQuad(VertexConsumer buffer, PoseStack.Pose pose, Direction direction,
                            Vector3f min, Vector3f max, TextureAtlasSprite sprite,
                            BlockyModelGeometry.FaceTextureLayout texLayout, Vector3f originalSize,
                            int lightCoords, boolean reversed) {

        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        int normalMult = reversed ? -1 : 1;
        Vector3f normal = new Vector3f(
                direction.getStepX() * normalMult,
                direction.getStepY() * normalMult,
                direction.getStepZ() * normalMult
        );
        normalMatrix.transform(normal);

        float[][] uvCoords = QuadBuilder.calculateUVCoordinates(direction, texLayout, originalSize, sprite);

        Vector3f[] vertices = QuadBuilder.getFaceVertices(direction, min, max);

        if (reversed) {
            for (int i = 3; i >= 0; i--) {
                addVertex(buffer, poseMatrix, normal, vertices[i], uvCoords[i], lightCoords);
            }
        } else {
            for (int i = 0; i < 4; i++) {
                addVertex(buffer, poseMatrix, normal, vertices[i], uvCoords[i], lightCoords);
            }
        }
    }

    private void addVertex(VertexConsumer buffer, Matrix4f pose, Vector3f normal,
                           Vector3f vertex, float[] uv, int lightCoords) {
        int blockLight = lightCoords & 0xFFFF;
        int skyLight = (lightCoords >> 16) & 0xFFFF;

        buffer.addVertex(pose, vertex.x, vertex.y, vertex.z)
                .setColor(255, 255, 255, 255)
                .setUv(uv[0], uv[1])
                .setUv2(blockLight, skyLight)
                .setNormal(normal.x, normal.y, normal.z);
    }

    /**
     * Finds a node by its name
     *
     * @param geometry the model to search
     * @param name     the name of the node
     * @return the node, or null if there is none
     */
    protected BlockyModelGeometry.BlockyNode findNodeByName(BlockyModelGeometry geometry, String name) {
        for (BlockyModelGeometry.BlockyNode node : geometry.getNodes()) {
            if (node.getName().equals(name)) return node;
        }
        return null;
    }

    /**
     * Sets a rotation on every descendant of a node
     *
     * @param node       the node whose descendants are rotated
     * @param rotation   the rotation to set
     * @param transforms the map the rotations are put in, keyed by node id
     */
    protected void applyTransformToDescendants(BlockyModelGeometry.BlockyNode node,
                                               Quaternionf rotation,
                                               Map<String, NodeTransform> transforms) {
        for (BlockyModelGeometry.BlockyNode child : node.getChildren()) {
            transforms.put(child.getId(), NodeTransform.rotation(rotation));
            applyTransformToDescendants(child, rotation, transforms);
        }
    }

    private BlockyModelGeometry getOrLoadGeometry(ResourceLocation modelLocation) {
        return geometryCache.computeIfAbsent(modelLocation, location -> {
            try {
                return Optional.of(BlockyModelLoader.INSTANCE.loadGeometry(new BlockyModelGeometry.Settings(location)));
            } catch (Exception e) {
                // Cached as empty so it only logs once
                HytaleModelLoader.LOGGER.error("Failed to load geometry for {}: {}", location, e.getMessage());
                return Optional.empty();
            }
        }).orElse(null);
    }

    /**
     * Gets an animation player, cached per renderer
     *
     * @param animId the location of the animation file
     * @return the player, or null if the animation can't be loaded
     */
    protected BlockyAnimationPlayer getOrCreatePlayer(ResourceLocation animId) {
        return playerCache.computeIfAbsent(animId, id ->
                Optional.ofNullable(BlockyAnimationLoader.INSTANCE.loadAnimation(id)).map(BlockyAnimationPlayer::new)
        ).orElse(null);
    }

}