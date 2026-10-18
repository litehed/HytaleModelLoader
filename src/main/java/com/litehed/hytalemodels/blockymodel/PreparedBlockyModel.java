package com.litehed.hytalemodels.blockymodel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * A static, precomputed model snapshot with node transforms and baked face geometry.
 * Rebuilt whenever the texture region changes, such as after resource reloads.
 */
public final class PreparedBlockyModel {

    private static final float UNITS_PER_BLOCK = 32.0f; // Same as TransformCalculator.POSITION_SCALE

    private final List<Part> parts;

    private PreparedBlockyModel(List<Part> parts) {
        this.parts = parts;
    }

    /**
     * Precomputes render data for every node of the geometry that has visible faces
     *
     * @param geometry the parsed model
     * @param sprite   the atlas sprite the model's UVs are resolved against
     * @return the prepared model
     */
    public static PreparedBlockyModel create(BlockyModelGeometry geometry, TextureAtlasSprite sprite) {
        return create(geometry, UVRegion.of(sprite));
    }

    /**
     * Precomputes render data for every node of the geometry that has visible faces
     *
     * @param geometry the parsed model
     * @param region   the texture region the model's UVs are resolved against
     * @return the prepared model
     */
    public static PreparedBlockyModel create(BlockyModelGeometry geometry, UVRegion region) {
        List<Part> parts = new ArrayList<>();
        for (BlockyModelGeometry.BlockyNode node : geometry.getNodes()) {
            if (!node.hasShape()) {
                continue;
            }
            Part part = buildPart(node, region);
            if (part != null) {
                parts.add(part);
            }
        }
        return new PreparedBlockyModel(parts);
    }

    private static Part buildPart(BlockyModelGeometry.BlockyNode node, UVRegion region) {
        BlockyModelGeometry.BlockyShape shape = node.getShape();

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

        // Pivot-only nodes
        if (quads.isEmpty()) {
            return null;
        }

        Vector3f worldPos = TransformCalculator.calculateWorldPosition(node);
        Quaternionf worldRot = TransformCalculator.calculateWorldOrientation(node);

        Vector3f rotatedOffset = shape.getOffset();
        worldRot.transform(rotatedOffset);

        Vector3f center = new Vector3f(
                (worldPos.x + rotatedOffset.x) / UNITS_PER_BLOCK,
                (worldPos.y + rotatedOffset.y) / UNITS_PER_BLOCK,
                (worldPos.z + rotatedOffset.z) / UNITS_PER_BLOCK
        );

        return new Part(node.getId(), center, worldRot, quads.toArray(new Quad[0]));
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
     * Submits every part of the model. The pose stack's current transform is treated as the model origin
     *
     * @param poseStack     the pose stack, already positioned/rotated/scaled for the model as a whole
     * @param collector     the collector to submit geometry to
     * @param renderType    the render type to draw with
     * @param lightCoords   packed light coordinates for the whole model
     * @param overlayCoords packed overlay coordinates (hurt flash); use {@code OverlayTexture.NO_OVERLAY} for none
     *                      Only has an effect with render types whose vertex format includes the overlay element
     * @param color         packed ARGB tint multiplied into every vertex; -1 (opaque white) for none
     */
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType,
                       int lightCoords, int overlayCoords, int color) {
        int blockLight = lightCoords & 0xFFFF;
        int skyLight = (lightCoords >> 16) & 0xFFFF;

        for (Part part : parts) {
            poseStack.pushPose();
            poseStack.translate(part.center.x, part.center.y, part.center.z);
            poseStack.mulPose(part.rotation);

            collector.submitCustomGeometry(poseStack, renderType,
                    (pose, buffer) -> writePart(part, pose, buffer, blockLight, skyLight, overlayCoords, color));

            poseStack.popPose();
        }
    }

    private static void writePart(Part part, PoseStack.Pose pose, VertexConsumer buffer,
                                  int blockLight, int skyLight, int overlayCoords, int color) {
        Vector3f normal = new Vector3f();
        for (Quad quad : part.quads) {
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
     * One node's worth of geometry. The node id is kept so animation can address parts later.
     */
    private record Part(String nodeId, Vector3f center, Quaternionf rotation, Quad[] quads) {
    }

    /**
     * One face, already in final vertex order (winding reversal baked in).
     */
    private record Quad(float[] positions, float[] uvs, float nx, float ny, float nz) {
    }
}