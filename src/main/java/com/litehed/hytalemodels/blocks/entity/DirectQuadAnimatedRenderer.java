package com.litehed.hytalemodels.blocks.entity;

import com.litehed.hytalemodels.HytaleModelLoader;
import com.litehed.hytalemodels.blockymodel.BlockyModelGeometry;
import com.litehed.hytalemodels.blockymodel.BlockyModelLoader;
import com.litehed.hytalemodels.blockymodel.TransformCalculator;
import com.litehed.hytalemodels.blockymodel.animations.AnimatedUVCalculator;
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

public class DirectQuadAnimatedRenderer implements BlockEntityRenderer<AnimatedChestBlockEntity, AnimatedChestRenderState> {

    private final Map<String, BlockyModelGeometry> geometryCache = new HashMap<>();

    private static final Material CHEST_TEXTURE_MATERIAL =
            new Material(TextureAtlas.LOCATION_BLOCKS,
                    Identifier.fromNamespaceAndPath(HytaleModelLoader.MODID, "block/chest_small_texture"));

    // Model location
    private static final Identifier CHEST_MODEL =
            Identifier.fromNamespaceAndPath(HytaleModelLoader.MODID, "models/chest_small.blockymodel");

    public DirectQuadAnimatedRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public AnimatedChestRenderState createRenderState() {
        return new AnimatedChestRenderState();
    }

    @Override
    public void extractRenderState(AnimatedChestBlockEntity blockEntity, AnimatedChestRenderState renderState,
                                   float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);
        renderState.modelName = blockEntity.getModelName();
        renderState.isOpen = blockEntity.isOpen();
        renderState.animationTick = blockEntity.getAnimationTick();
        renderState.partialTick = partialTick;

        if (blockEntity.getLevel() != null) {
            renderState.ageInTicks = blockEntity.getLevel().getGameTime() + partialTick;
        }
    }

    @Override
    public void submit(AnimatedChestRenderState renderState, PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {


        if (renderState.modelName == null) {
            return;
        }

        BlockyModelGeometry geometry = getOrLoadGeometry(CHEST_MODEL);
        if (geometry == null) {
            return;
        }

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getAtlasManager().get(CHEST_TEXTURE_MATERIAL);

        poseStack.pushPose();

        poseStack.translate(0.5, 0.5, 0.5);

        Map<String, NodeTransform> nodeTransforms = calculateNodeTransforms(renderState, geometry);
        List<BlockyModelGeometry.BlockyNode> nodes = geometry.getNodes();
        for (BlockyModelGeometry.BlockyNode node : nodes) {
            if (node.hasShape()) {
                renderNode(poseStack, submitNodeCollector, node, sprite, nodeTransforms, renderState);

            }
        }

        poseStack.popPose();
    }

    // Test anims
    private Map<String, NodeTransform> calculateNodeTransforms(AnimatedChestRenderState renderState,
                                                               BlockyModelGeometry geometry) {
        Map<String, NodeTransform> transforms = new HashMap<>();

        float time = renderState.ageInTicks * 0.1f;
        float yOffset = (float) Math.sin(time) * 3.0f;
        NodeTransform lidTransform = new NodeTransform(
                new Vector3f(0, yOffset, 0),
                new Quaternionf(),
                new Vector3f(1, 1, 1)
        );

        transforms.put("Lid", lidTransform);

        return transforms;
    }

    private void renderNode(PoseStack poseStack, SubmitNodeCollector collector,
                            BlockyModelGeometry.BlockyNode node, TextureAtlasSprite sprite,
                            Map<String, NodeTransform> nodeTransforms, AnimatedChestRenderState renderState) {

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

            RenderType renderType = RenderTypes.cutoutMovingBlock();

            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                    renderQuad(buffer, pose, direction, min, max, sprite, texLayout, shape.getOriginalSize(), renderState));

            // Render backface if double-sided
            if (shape.isDoubleSided()) {
                collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                        renderQuadReversed(buffer, pose, direction, min, max, sprite, texLayout, shape.getOriginalSize(), renderState));
            }
        }

        poseStack.popPose();
    }


    private void applyNodeTransform(PoseStack poseStack, BlockyModelGeometry.BlockyNode node,
                                    Map<String, NodeTransform> animTransforms) {
        Vector3f worldPos = TransformCalculator.calculateWorldPosition(node);
        Quaternionf worldRot = TransformCalculator.calculateWorldOrientation(node);

        Vector3f shapeOffset = node.getShape().getOffset();
        Vector3f rotatedOffset = new Vector3f(shapeOffset);
        worldRot.transform(rotatedOffset);

        float centerX = (worldPos.x + rotatedOffset.x) / 32.0f;
        float centerY = ((worldPos.y + rotatedOffset.y) - 16.0f) / 32.0f;
        float centerZ = (worldPos.z + rotatedOffset.z) / 32.0f;

        poseStack.translate(centerX, centerY, centerZ);

        poseStack.mulPose(worldRot);

        NodeTransform animTransform = animTransforms.get(node.getName());
        if (animTransform != null) {
            poseStack.translate(
                    animTransform.position.x / 16.0f,
                    animTransform.position.y / 16.0f,
                    animTransform.position.z / 16.0f
            );
            poseStack.mulPose(animTransform.rotation);
            poseStack.scale(animTransform.scale.x, animTransform.scale.y, animTransform.scale.z);
        }
    }

    private void renderQuad(VertexConsumer buffer, PoseStack.Pose pose, Direction direction,
                            Vector3f min, Vector3f max, TextureAtlasSprite sprite,
                            BlockyModelGeometry.FaceTextureLayout texLayout, Vector3f originalSize,
                            AnimatedChestRenderState renderState) {

        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        Vector3f n = new Vector3f(direction.getStepX(), direction.getStepY(), direction.getStepZ());
        normalMatrix.transform(n);

        float[][] uvCoords = AnimatedUVCalculator.calculateUVs(direction, texLayout, originalSize, sprite);

        Vector3f[] vertices = getQuadBuilderVertices(direction, min, max);

        for (int i = 0; i < 4; i++) {
            vertex(buffer, poseMatrix, n, vertices[i].x, vertices[i].y, vertices[i].z,
                    uvCoords[i][0], uvCoords[i][1], renderState.lightCoords);
        }
    }

    private void renderQuadReversed(VertexConsumer buffer, PoseStack.Pose pose, Direction direction,
                                    Vector3f min, Vector3f max, TextureAtlasSprite sprite,
                                    BlockyModelGeometry.FaceTextureLayout texLayout, Vector3f originalSize,
                                    AnimatedChestRenderState renderState) {

        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        Vector3f n = new Vector3f(-direction.getStepX(), -direction.getStepY(), -direction.getStepZ());
        normalMatrix.transform(n);

        float[][] uvCoords = AnimatedUVCalculator.calculateUVs(direction, texLayout, originalSize, sprite);

        Vector3f[] vertices = getQuadBuilderVertices(direction, min, max);

        for (int i = 3; i >= 0; i--) {
            vertex(buffer, poseMatrix, n, vertices[i].x, vertices[i].y, vertices[i].z,
                    uvCoords[i][0], uvCoords[i][1], renderState.lightCoords);
        }
    }

    private Vector3f[] getQuadBuilderVertices(Direction face, Vector3f min, Vector3f max) {
        float x0 = min.x, y0 = min.y, z0 = min.z;
        float x1 = max.x, y1 = max.y, z1 = max.z;

        return switch (face) {
            case DOWN -> new Vector3f[]{
                    new Vector3f(x0, y0, z0), new Vector3f(x1, y0, z0),
                    new Vector3f(x1, y0, z1), new Vector3f(x0, y0, z1)
            };
            case UP -> new Vector3f[]{
                    new Vector3f(x0, y1, z1), new Vector3f(x1, y1, z1),
                    new Vector3f(x1, y1, z0), new Vector3f(x0, y1, z0)
            };
            case NORTH -> new Vector3f[]{
                    new Vector3f(x1, y0, z0), new Vector3f(x0, y0, z0),
                    new Vector3f(x0, y1, z0), new Vector3f(x1, y1, z0)
            };
            case SOUTH -> new Vector3f[]{
                    new Vector3f(x0, y0, z1), new Vector3f(x1, y0, z1),
                    new Vector3f(x1, y1, z1), new Vector3f(x0, y1, z1)
            };
            case WEST -> new Vector3f[]{
                    new Vector3f(x0, y0, z0), new Vector3f(x0, y0, z1),
                    new Vector3f(x0, y1, z1), new Vector3f(x0, y1, z0)
            };
            case EAST -> new Vector3f[]{
                    new Vector3f(x1, y0, z1), new Vector3f(x1, y0, z0),
                    new Vector3f(x1, y1, z0), new Vector3f(x1, y1, z1)
            };
        };
    }


    private void vertex(VertexConsumer buffer, Matrix4f pose, Vector3f normal,
                        float x, float y, float z, float u, float v, int lightCoords) {
        int blockLight = lightCoords & 0xFFFF;
        int skyLight = (lightCoords >> 16) & 0xFFFF;

        buffer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setUv2(blockLight, skyLight)
                .setNormal(normal.x, normal.y, normal.z);
    }


    private BlockyModelGeometry getOrLoadGeometry(Identifier modelLocation) {
        if (geometryCache.containsKey(modelLocation.toString())) {
            return geometryCache.get(modelLocation.toString());
        }

        try {
            BlockyModelGeometry geometry = BlockyModelLoader.INSTANCE.loadGeometry(
                    new BlockyModelGeometry.Settings(modelLocation)
            );
            geometryCache.put(modelLocation.toString(), geometry);
            return geometry;
        } catch (Exception e) {
            return null;
        }
    }


    private static class NodeTransform {
        final Vector3f position;
        final Quaternionf rotation;
        final Vector3f scale;

        NodeTransform(Vector3f position, Quaternionf rotation, Vector3f scale) {
            this.position = position;
            this.rotation = rotation;
            this.scale = scale;
        }
    }
}















