package com.litehed.hytalemodels.blocks.entity;

import com.litehed.hytalemodels.HytaleModelLoader;
import com.litehed.hytalemodels.blockymodel.BlockyModelGeometry;
import com.litehed.hytalemodels.blockymodel.BlockyModelLoader;
import com.litehed.hytalemodels.blockymodel.QuadBuilder;
import com.litehed.hytalemodels.blockymodel.TransformCalculator;
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

    public HytaleBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void extractRenderState(T blockEntity, S renderState, float partialTick,
                                   Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);

        renderState.modelName = blockEntity.getModelName();
        renderState.animationTick = blockEntity.getAnimationTick();
        renderState.partialTick = partialTick;

        if (blockEntity.getLevel() != null) {
            renderState.ageInTicks = blockEntity.getLevel().getGameTime() + partialTick;
        }

        extractAdditionalRenderState(blockEntity, renderState, partialTick);
    }


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

        Map<String, NodeTransform> nodeTransforms = calculateAnimationTransforms(renderState, geometry);

        List<BlockyModelGeometry.BlockyNode> nodes = geometry.getNodes();
        for (BlockyModelGeometry.BlockyNode node : nodes) {
            if (node.hasShape()) {
                renderNode(poseStack, submitNodeCollector, node, sprite, nodeTransforms, renderState);
            }
        }

        poseStack.popPose();
    }


    protected abstract Map<String, NodeTransform> calculateAnimationTransforms(S renderState, BlockyModelGeometry geometry);


    protected Identifier getModelLocation(String modelName) {
        return Identifier.fromNamespaceAndPath(HytaleModelLoader.MODID, "models/" + modelName + ".blockymodel");
    }

    protected Material getTextureMaterial(String modelName) {
        return new Material(TextureAtlas.LOCATION_BLOCKS,
                Identifier.fromNamespaceAndPath(HytaleModelLoader.MODID, "block/" + modelName + "_texture"));
    }

    private void renderNode(PoseStack poseStack, SubmitNodeCollector collector,
                            BlockyModelGeometry.BlockyNode node, TextureAtlasSprite sprite,
                            Map<String, NodeTransform> nodeTransforms, S renderState) {
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

        if (effectiveTransform != null && !effectiveTransform.equals(NodeTransform.identity())) {
            Vector3f pivotBlock;
            BlockyModelGeometry.BlockyNode parent = node.getParent();

            if (parent != null && (effectiveTransforms.containsKey(parent.getId()) || effectiveTransforms.containsKey(parent.getName()))) {
                // Child node
                Vector3f parentWorldPos = TransformCalculator.calculateWorldPosition(parent);
                float pivotX = parentWorldPos.x / 32.0f;
                float pivotY = (parentWorldPos.y - 16.0f) / 32.0f;
                float pivotZ = parentWorldPos.z / 32.0f;
                pivotBlock = new Vector3f(pivotX, pivotY, pivotZ);
            } else {
                // Parent node or standalone node
                float pivotX = worldPos.x / 32.0f;
                float pivotY = (worldPos.y - 16.0f) / 32.0f;
                float pivotZ = worldPos.z / 32.0f;
                pivotBlock = new Vector3f(pivotX, pivotY, pivotZ);
            }

            poseStack.translate(pivotBlock.x, pivotBlock.y, pivotBlock.z);

            poseStack.mulPose(effectiveTransform.rotation());

            Vector3f animPos = effectiveTransform.position();
            poseStack.translate(animPos.x, animPos.y, animPos.z);

            poseStack.translate(
                    centerX - pivotBlock.x,
                    centerY - pivotBlock.y,
                    centerZ - pivotBlock.z
            );

            poseStack.mulPose(worldRot);

            Vector3f animScale = effectiveTransform.scale();
            poseStack.scale(animScale.x, animScale.y, animScale.z);
        } else {
            // No animation
            poseStack.translate(centerX, centerY, centerZ);
            poseStack.mulPose(worldRot);
        }
    }

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
}