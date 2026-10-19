package com.litehed.hytalemodels.blockymodel;

import com.litehed.hytalemodels.api.BlockyPose;
import com.litehed.hytalemodels.api.NodeTransform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * A static, precomputed model snapshot with node transforms and baked face geometry.
 * Rebuilt whenever the texture region changes, such as after resource reloads.
 */
public final class PreparedBlockyModel {

    private final Bone[] bones;

    private PreparedBlockyModel(Bone[] bones) {
        this.bones = bones;
    }

    /**
     * Prepares a model for rendering with an atlas sprite
     *
     * @param geometry the parsed model
     * @param sprite   the sprite the UVs are mapped into
     * @return the prepared model
     */
    public static PreparedBlockyModel create(BlockyModelGeometry geometry, TextureAtlasSprite sprite) {
        return create(geometry, UVRegion.of(sprite));
    }

    /**
     * Prepares a model for rendering with any texture region
     *
     * @param geometry the parsed model
     * @param region   the region the UVs are mapped into
     * @return the prepared model
     */
    public static PreparedBlockyModel create(BlockyModelGeometry geometry, UVRegion region) {
        List<BlockyModelGeometry.BlockyNode> nodes = geometry.getNodes();

        Map<BlockyModelGeometry.BlockyNode, Integer> indices = new IdentityHashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            indices.put(nodes.get(i), i);
        }

        Bone[] bones = new Bone[nodes.size()];
        for (int i = 0; i < bones.length; i++) {
            bones[i] = buildBone(nodes.get(i), i, indices, region);
        }
        return new PreparedBlockyModel(bones);
    }

    private static Bone buildBone(BlockyModelGeometry.BlockyNode node, int index,
                                  Map<BlockyModelGeometry.BlockyNode, Integer> indices, UVRegion region) {
        BlockyModelGeometry.BlockyNode parent = node.getParent();
        int parentIndex = parent == null ? -1 : indices.get(parent);
        if (parentIndex >= index) {
            throw new IllegalStateException("Node " + node.getName() + " is listed before its parent");
        }

        // Children attach at the shape offset of their parent
        Vector3f shapeOffset = new Vector3f();
        Quad[] quads = new Quad[0];
        if (node.hasShape()) {
            shapeOffset = node.getShape().getOffset();
            quads = buildQuads(node.getShape(), region);
        }
        Vector3f attach = parent != null && parent.hasShape() ? parent.getShape().getOffset() : new Vector3f();

        return new Bone(node.getId(), node.getName(), parentIndex, node.getPosition(), node.getOrientation(),
                attach, shapeOffset, quads);
    }

    private static Quad[] buildQuads(BlockyModelGeometry.BlockyShape shape, UVRegion region) {
        Vector3f halfSizes = TransformCalculator.calculateHalfSizes(shape.getSize());
        Vector3f min = new Vector3f(-halfSizes.x, -halfSizes.y, -halfSizes.z);
        Vector3f max = new Vector3f(halfSizes.x, halfSizes.y, halfSizes.z);

        boolean reversed = shape.needsWindingReversal();
        List<Quad> quads = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            if (!shape.hasTextureLayout(direction)) {
                continue;
            }
            BlockyModelGeometry.FaceTextureLayout layout = shape.getTextureLayout(direction);

            quads.add(buildQuad(direction, min, max, region, layout, shape.getOriginalSize(), reversed));
            if (shape.isDoubleSided()) {
                quads.add(buildQuad(direction, min, max, region, layout, shape.getOriginalSize(), !reversed));
            }
        }
        return quads.toArray(new Quad[0]);
    }

    private static Quad buildQuad(Direction direction, Vector3f min, Vector3f max, UVRegion region,
                                  BlockyModelGeometry.FaceTextureLayout layout, Vector3f originalSize,
                                  boolean reversed) {
        Vector3f[] vertices = QuadBuilder.getFaceVertices(direction, min, max);
        float[][] uvs = QuadBuilder.calculateUVCoordinates(direction, layout, originalSize, region);

        float[] positions = new float[12];
        float[] texCoords = new float[8];
        for (int i = 0; i < 4; i++) {
            int src = reversed ? 3 - i : i;
            positions[i * 3] = vertices[src].x;
            positions[i * 3 + 1] = vertices[src].y;
            positions[i * 3 + 2] = vertices[src].z;
            texCoords[i * 2] = uvs[src][0];
            texCoords[i * 2 + 1] = uvs[src][1];
        }

        int mult = reversed ? -1 : 1;
        return new Quad(positions, texCoords,
                direction.getStepX() * mult, direction.getStepY() * mult, direction.getStepZ() * mult);
    }

    /**
     * Submits the model in its rest pose
     *
     * @see #submit(PoseStack, SubmitNodeCollector, RenderType, int, int, int, BlockyPose)
     */
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType,
                       int lightCoords, int overlayCoords, int color) {
        submit(poseStack, collector, renderType, lightCoords, overlayCoords, color, new BlockyPose());
    }

    /**
     * Submits the model with a pose. The pose stack's current transform is the model origin
     *
     * @param poseStack     the pose stack, already moved to where the model goes
     * @param collector     the collector to submit geometry to
     * @param renderType    the render type to draw with
     * @param lightCoords   packed light for the whole model
     * @param overlayCoords packed overlay for the hurt flash, or OverlayTexture.NO_OVERLAY
     * @param color         packed ARGB tint, -1 for none
     * @param blockyPose    transforms layered on the rest pose
     */
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType,
                       int lightCoords, int overlayCoords, int color, BlockyPose blockyPose) {
        int blockLight = lightCoords & 0xFFFF;
        int skyLight = (lightCoords >> 16) & 0xFFFF;

        // Origin and rotation of each node in model space, in model units
        Vector3f[] origins = new Vector3f[bones.length];
        Quaternionf[] rotations = new Quaternionf[bones.length];
        Vector3f local = new Vector3f();

        for (int i = 0; i < bones.length; i++) {
            Bone bone = bones[i];
            NodeTransform animated = blockyPose.resolve(bone.id, bone.name);

            local.set(bone.restPosition);
            if (animated != null) {
                local.add(animated.position().mul(TransformCalculator.UNITS_PER_BLOCK));
            }

            if (bone.parent >= 0) {
                local.add(bone.attach);
                rotations[bone.parent].transform(local);
                origins[i] = new Vector3f(origins[bone.parent]).add(local);
                rotations[i] = new Quaternionf(rotations[bone.parent]).mul(bone.restRotation);
            } else {
                origins[i] = new Vector3f(local);
                rotations[i] = new Quaternionf(bone.restRotation);
            }
            if (animated != null) {
                // Animation rotations are applied on top of the rest rotation
                rotations[i].mul(animated.rotation());
            }

            if (bone.quads.length == 0 || (animated != null && !animated.visible())) {
                continue;
            }

            Vector3f center = new Vector3f(bone.shapeOffset);
            rotations[i].transform(center);
            center.add(origins[i]).div(TransformCalculator.UNITS_PER_BLOCK);

            poseStack.pushPose();
            poseStack.translate(center.x, center.y, center.z);
            poseStack.mulPose(rotations[i]);
            if (animated != null) {
                Vector3f scale = animated.scale();
                if (scale.x != 1.0f || scale.y != 1.0f || scale.z != 1.0f) {
                    poseStack.scale(scale.x, scale.y, scale.z);
                }
            }

            collector.submitCustomGeometry(poseStack, renderType,
                    (pose, buffer) -> writeQuads(bone.quads, pose, buffer, blockLight, skyLight, overlayCoords, color));

            poseStack.popPose();
        }
    }

    private static void writeQuads(Quad[] quads, PoseStack.Pose pose, VertexConsumer buffer,
                                   int blockLight, int skyLight, int overlayCoords, int color) {
        Vector3f normal = new Vector3f();
        for (Quad quad : quads) {
            normal.set(quad.nx, quad.ny, quad.nz);
            pose.normal().transform(normal);

            for (int i = 0; i < 4; i++) {
                buffer.addVertex(pose.pose(), quad.positions[i * 3], quad.positions[i * 3 + 1], quad.positions[i * 3 + 2])
                        .setColor(color)
                        .setUv(quad.uvs[i * 2], quad.uvs[i * 2 + 1])
                        .setOverlay(overlayCoords)
                        .setUv2(blockLight, skyLight)
                        .setNormal(normal.x, normal.y, normal.z);
            }
        }
    }

    /**
     * One node with its rest transform and the faces of its shape
     * Nodes without a shape are kept because their children still need their transform
     */
    private record Bone(String id, String name, int parent, Vector3f restPosition, Quaternionf restRotation,
                        Vector3f attach, Vector3f shapeOffset, Quad[] quads) {
    }

    /**
     * One face, already in final vertex order
     */
    private record Quad(float[] positions, float[] uvs, float nx, float ny, float nz) {
    }
}