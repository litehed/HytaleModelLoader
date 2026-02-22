package com.litehed.hytalemodels.blocks.entity;

import com.litehed.hytalemodels.HytaleModelLoader;
import com.litehed.hytalemodels.blocks.HytaleBlockBase;
import com.litehed.hytalemodels.blockymodel.BlockyModelGeometry;
import com.litehed.hytalemodels.blockymodel.BlockyModelLoader;
import com.litehed.hytalemodels.blockymodel.QuadBuilder;
import com.litehed.hytalemodels.blockymodel.TransformCalculator;
import com.litehed.hytalemodels.blockymodel.animations.BlockyAnimationDefinition;
import com.litehed.hytalemodels.blockymodel.animations.BlockyAnimationLoader;
import com.litehed.hytalemodels.blockymodel.animations.BlockyAnimationPlayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class HytaleBlockEntityRenderer<T extends HytaleBlockEntity, S extends HytaleRenderState>
        implements BlockEntityRenderer<T, S> {

    private final Map<String, BlockyModelGeometry> geometryCache = new HashMap<>();
    private final Map<Identifier, BlockyAnimationPlayer> playerCache = new HashMap<>();

    public HytaleBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void extractRenderState(T blockEntity, S renderState, float partialTick,
                                   Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);

        renderState.modelName = blockEntity.getModelName();
        renderState.animationTick = blockEntity.getAnimationTick();
        renderState.partialTick = partialTick;
        renderState.ageInTicks = (blockEntity.getAnimationTick() + partialTick) * 4;

        if (blockEntity.getLevel() != null) {
            BlockState blockState = blockEntity.getLevel().getBlockState(blockEntity.getBlockPos());
            if (blockState.hasProperty(HytaleBlockBase.FACING)) {
                renderState.facing = blockState.getValue(HytaleBlockBase.FACING);
            }
        }

        extractAdditionalRenderState(blockEntity, renderState, partialTick);
    }


    /**
     * Allows subclasses to extract additional data from the block entity and put it into the render state
     *
     * @param blockEntity the block entity being rendered
     * @param renderState the render state being built for this block entity
     * @param partialTick the current partial tick
     */
    protected void extractAdditionalRenderState(T blockEntity, S renderState, float partialTick) {
    }

    @Override
    public void submit(S renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState cameraRenderState) {
        if (renderState.modelName == null) {
            return;
        }

        Identifier modelLocation = getModelLocation(renderState.modelName);
        BlockyModelGeometry geometry = getOrLoadGeometry(modelLocation);
        if (geometry == null) {
            HytaleModelLoader.LOGGER.warn("Failed to load geometry for model: {}", modelLocation);
            return;
        }

        Material textureMaterial = getTextureMaterial(renderState.modelName);
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getAtlasManager().get(textureMaterial);

        poseStack.pushPose();

        poseStack.translate(0.5, 0.5, 0.5);

        float yRotDegrees = getFacingYRotation(renderState.facing);
        if (yRotDegrees != 0f) {
            poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(yRotDegrees)));
        }

        Map<String, NodeTransform> nodeTransforms = calculateAnimationTransforms(renderState, geometry);

        List<BlockyModelGeometry.BlockyNode> nodes = geometry.getNodes();
        for (BlockyModelGeometry.BlockyNode node : nodes) {
            if (node.hasShape()) {
                renderNode(poseStack, submitNodeCollector, node, sprite, nodeTransforms, renderState);
            }
        }

        poseStack.popPose();
    }

    /**
     * Gets the rotation degree based on direction blockstate
     *
     * @param facing The Direction the block is being placed in
     * @return Degrees to rotate block along the Y
     */
    protected float getFacingYRotation(Direction facing) {
        return switch (facing) {
            case NORTH -> 180f;
            case EAST -> 90f;
            case WEST -> 270f;
            default -> 0f;
        };
    }


    /**
     * Calculates the transforms for all nodes in the model
     *
     * @param renderState the current render state containing animation tick and other relevant data
     * @param geometry    the geometry of the model being rendered
     * @return a map of node IDs/names to their corresponding transforms for the current frame
     */
    protected abstract Map<String, NodeTransform> calculateAnimationTransforms(S renderState, BlockyModelGeometry geometry);


    /**
     * Gets the model location for a given model name
     *
     * @param modelName the name of the model
     * @return the identifier pointing to the model file for this model name
     */
    protected Identifier getModelLocation(String modelName) {
        return Identifier.fromNamespaceAndPath(HytaleModelLoader.MODID, "models/" + modelName + ".blockymodel");
    }

    /**
     * Gets the texture material for a given model name
     *
     * @param modelName the name of the model
     * @return the material pointing to the texture for this model name
     */
    protected Material getTextureMaterial(String modelName) {
        return new Material(TextureAtlas.LOCATION_BLOCKS,
                Identifier.fromNamespaceAndPath(HytaleModelLoader.MODID, "block/" + modelName + "_texture"));
    }

    /**
     * Renders a single node of the model, including applying any relevant transforms and rendering the quads for its shape
     *
     * @param poseStack      the current pose stack for rendering
     * @param collector      the submit node collector to submit geometry to
     * @param node           the node being rendered
     * @param sprite         the texture sprite to use for rendering this node
     * @param nodeTransforms the map of node transforms calculated for the current frame for animations
     * @param renderState    the current render state
     */
    private void renderNode(PoseStack poseStack, SubmitNodeCollector collector,
                            BlockyModelGeometry.BlockyNode node, TextureAtlasSprite sprite,
                            Map<String, NodeTransform> nodeTransforms, S renderState) {

        NodeTransform transform = nodeTransforms.get(node.getId());
        if (transform == null) transform = nodeTransforms.get(node.getName());
        if (transform != null && !transform.visible()) return;

        poseStack.pushPose();

        applyNodeTransform(poseStack, node, nodeTransforms);

        BlockyModelGeometry.BlockyShape shape = node.getShape();

        Vector3f halfSizes = TransformCalculator.calculateHalfSizes(shape.getSize());
        Vector3f min = new Vector3f(-halfSizes.x, -halfSizes.y, -halfSizes.z);
        Vector3f max = new Vector3f(halfSizes.x, halfSizes.y, halfSizes.z);

        RenderType renderType = getRenderType();

        for (Direction direction : Direction.values()) {
            if (!shape.hasTextureLayout(direction)) {
                continue;
            }

            BlockyModelGeometry.FaceTextureLayout texLayout = shape.getTextureLayout(direction);
            boolean shouldReverse = shape.needsWindingReversal();

            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                    renderQuad(buffer, pose, direction, min, max, sprite, texLayout,
                            shape.getOriginalSize(), renderState, shouldReverse));

            // Render backface if double-sided
            if (shape.isDoubleSided()) {
                collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                        renderQuad(buffer, pose, direction, min, max, sprite, texLayout,
                                shape.getOriginalSize(), renderState, !shouldReverse));
            }
        }

        poseStack.popPose();
    }

    protected RenderType getRenderType() {
        return RenderTypes.cutoutMovingBlock();
    }

    /**
     * Applies the necessary transformations to the pose stack for rendering a given node
     *
     * @param poseStack           the current pose stack for rendering
     * @param node                the node being rendered
     * @param effectiveTransforms the map of effective transforms for all nodes
     */
    private void applyNodeTransform(PoseStack poseStack, BlockyModelGeometry.BlockyNode node,
                                    Map<String, NodeTransform> effectiveTransforms) {
        Vector3f worldPos = TransformCalculator.calculateWorldPosition(node);
        Quaternionf worldRot = TransformCalculator.calculateWorldOrientation(node);

        Vector3f shapeOffset = node.getShape().getOffset();
        Vector3f rotatedOffset = new Vector3f(shapeOffset);
        worldRot.transform(rotatedOffset);

        float centerX = (worldPos.x + rotatedOffset.x) / 32.0f;
        float centerY = ((worldPos.y + rotatedOffset.y) - 16.0f) / 32.0f;
        float centerZ = (worldPos.z + rotatedOffset.z) / 32.0f;

        NodeTransform effectiveTransform = effectiveTransforms.get(node.getId());
        if (effectiveTransform == null) {
            effectiveTransform = effectiveTransforms.get(node.getName());
        }

        NodeTransform parentAnimTransform = getParentAnimationTransform(node, effectiveTransforms);

        boolean hasOwnAnimation = effectiveTransform != null && !effectiveTransform.equals(NodeTransform.identity());
        boolean parentHasAnimation = parentAnimTransform != null && !parentAnimTransform.equals(NodeTransform.identity());

        if (hasOwnAnimation || parentHasAnimation) {
            Vector3f parentPivot = null;
            BlockyModelGeometry.BlockyNode parent = node.getParent();
            if (parent != null && (effectiveTransforms.containsKey(parent.getId()) ||
                    effectiveTransforms.containsKey(parent.getName()))) {
                Vector3f parentWorldPos = TransformCalculator.calculateWorldPosition(parent);
                parentPivot = new Vector3f(
                        parentWorldPos.x / 32.0f,
                        (parentWorldPos.y - 16.0f) / 32.0f,
                        parentWorldPos.z / 32.0f
                );
            }

            float childPivotX = worldPos.x / 32.0f;
            float childPivotY = (worldPos.y - 16.0f) / 32.0f;
            float childPivotZ = worldPos.z / 32.0f;

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

    /**
     * Recursively checks parent nodes for animation transforms to apply to the current node (Child nodes inherit animations)
     *
     * @param node                the node being rendered
     * @param effectiveTransforms the map of effective transforms for all nodes
     * @return the first parent animation transform found, else null
     */
    private NodeTransform getParentAnimationTransform(BlockyModelGeometry.BlockyNode node,
                                                      Map<String, NodeTransform> effectiveTransforms) {
        BlockyModelGeometry.BlockyNode current = node.getParent();

        while (current != null) {
            NodeTransform transform = effectiveTransforms.get(current.getId());
            if (transform == null) {
                transform = effectiveTransforms.get(current.getName());
            }
            if (transform != null && !transform.equals(NodeTransform.identity())) {
                return transform;
            }
            current = current.getParent();
        }

        return null;
    }

    /**
     * Renders a single quad for a given face of the shape, applying the appropriate texture coordinates and normal based on the face direction
     *
     * @param buffer       the vertex consumer buffer to submit vertices to
     * @param pose         the current pose for rendering, containing the transformation matrix and normal matrix
     * @param direction    the face direction this quad is being rendered for
     * @param min          the minimum corner of the quad in model space
     * @param max          the maximum corner of the quad in model space
     * @param sprite       the texture sprite to use for calculating UV coordinates
     * @param texLayout    the texture layout information for this face, containing UV offsets and sizes
     * @param originalSize the original size of the shape, used for calculating UV coordinates when set to stretch
     * @param renderState  the current render state
     * @param reversed     whether to reverse the vertex order for this quad
     */
    private void renderQuad(VertexConsumer buffer, PoseStack.Pose pose, Direction direction,
                            Vector3f min, Vector3f max, TextureAtlasSprite sprite,
                            BlockyModelGeometry.FaceTextureLayout texLayout, Vector3f originalSize,
                            S renderState, boolean reversed) {

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
                addVertex(buffer, poseMatrix, normal, vertices[i], uvCoords[i], renderState.lightCoords);
            }
        } else {
            for (int i = 0; i < 4; i++) {
                addVertex(buffer, poseMatrix, normal, vertices[i], uvCoords[i], renderState.lightCoords);
            }
        }
    }

    /**
     * Adds a single vertex to the buffer with the given position, normal, UV coordinates, and light information
     *
     * @param buffer      the vertex consumer buffer to submit the vertex to
     * @param pose        the current pose transformation matrix to apply to the vertex position
     * @param normal      the normal vector for this vertex, used for lighting calculations
     * @param vertex      the position of the vertex in model space
     * @param uv          the UV coordinates for this vertex, calculated based on the texture layout and sprite
     * @param lightCoords the combined block and sky light coordinates for this vertex, used for lighting calculations
     */
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
     * Finds a node in the model geometry by its name, searching recursively through all nodes and their children
     *
     * @param geometry the model geometry to search through
     * @param name     the name of the node to find
     * @return the node with the given name, else null
     */
    protected BlockyModelGeometry.BlockyNode findNodeByName(BlockyModelGeometry geometry, String name) {
        return findNodeByNameRecursive(geometry.getNodes(), name);
    }

    /**
     * Helper method to recursively search for a node by name through a list of nodes and their children
     *
     * @param nodes the list of nodes to search through
     * @param name  the name of the node to find
     * @return the node with the given name, else null
     */
    private BlockyModelGeometry.BlockyNode findNodeByNameRecursive(
            List<BlockyModelGeometry.BlockyNode> nodes, String name) {
        for (BlockyModelGeometry.BlockyNode node : nodes) {
            if (node.getName().equals(name)) return node;
            BlockyModelGeometry.BlockyNode found = findNodeByNameRecursive(node.getChildren(), name);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * Recursively applies a given rotation transform to a node and all of its descendants, storing the results in the provided transforms map
     *
     * @param node       the node to apply the transform to, along with all of its children
     * @param rotation   the rotation to apply to this node and its descendants
     * @param transforms the map to store the resulting transforms for each node, keyed by node ID or name
     */
    protected void applyTransformToDescendants(BlockyModelGeometry.BlockyNode node,
                                               Quaternionf rotation,
                                               Map<String, NodeTransform> transforms) {
        for (BlockyModelGeometry.BlockyNode child : node.getChildren()) {
            transforms.put(child.getId(), NodeTransform.rotation(rotation));
            applyTransformToDescendants(child, rotation, transforms);
        }
    }

    /**
     * Gets the geometry for a given model location, either from the cache or by loading it from the model file if not already cached
     *
     * @param modelLocation the identifier pointing to the model file for this model
     * @return the geometry for this model, else null
     */
    private BlockyModelGeometry getOrLoadGeometry(Identifier modelLocation) {
        String key = modelLocation.toString();

        if (geometryCache.containsKey(key)) {
            return geometryCache.get(key);
        }

        try {
            BlockyModelGeometry geometry = BlockyModelLoader.INSTANCE.loadGeometry(
                    new BlockyModelGeometry.Settings(modelLocation)
            );
            geometryCache.put(key, geometry);
            return geometry;
        } catch (Exception e) {
            HytaleModelLoader.LOGGER.error("Failed to load geometry for {}: {}", modelLocation, e.getMessage());
            return null;
        }
    }

    /**
     * Returns a cached BlockyAnimationPlayer for the given animation ID, creating and caching one
     * on first access. Returns null if the animation definition cannot be loaded.
     *
     * @param animId the identifier pointing to the animation file
     * @return the cached player for this animation, else null
     */
    protected BlockyAnimationPlayer getOrCreatePlayer(Identifier animId) {
        return playerCache.computeIfAbsent(animId, id -> {
            BlockyAnimationDefinition definition = BlockyAnimationLoader.INSTANCE.loadAnimation(id);
            return definition != null ? new BlockyAnimationPlayer(definition) : null;
        });
    }

}