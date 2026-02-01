package com.litehed.hytalemodels.modelstuff;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public class QuadBuilder {

    private static final int TINT_INDEX_NONE = -1;                          // No tinting -1
    private static final float[] COLOR_WHITE = {1.0f, 1.0f, 1.0f, 1.0f};

    public static final boolean DEBUG_BORDERS = false;
    private static final float BORDER_THICKNESS = 0.002f;

    /**
     * Create a single quad for a given face
     *
     * @param face      the Direction of the face
     * @param min       the minimum coordinates of the face
     * @param max       the maximum coordinates of the face
     * @param sprite    the TextureAtlasSprite to use for the quad
     * @param texLayout the texture layout for this face
     * @param size      the size of the block in each axis (x, y, z)
     * @param transform the transformation to apply to the quad
     * @return a Pair containing the BakedQuad and its cull face Direction
     */
    public static Pair<BakedQuad, Direction> createQuad(
            Direction face,
            Vector3f min,
            Vector3f max,
            TextureAtlasSprite sprite,
            BlockyModelGeometry.FaceTextureLayout texLayout,
            Vector3f size,
            Transformation transform) {

        QuadBakingVertexConsumer baker = setupQuadBaker(face, sprite);

        float[][] uvCoords = calculateUVCoordinates(face, texLayout, size, sprite);
        Vector3f[] vertices = getFaceVertices(face, min, max);

        bakeVertices(baker, vertices, uvCoords, face, transform);

        Direction cullFace = calculateCullFace(face, min, max);
        return Pair.of(baker.bakeQuad(), cullFace);
    }

    /**
     * Create a reversed quad for backfaces
     *
     * @param face      the Direction of the face
     * @param min       the minimum coordinates of the face
     * @param max       the maximum coordinates of the face
     * @param sprite    the TextureAtlasSprite to use for the quad
     * @param texLayout the texture layout for this face
     * @param size      the size of the block in each axis (x, y, z)
     * @param transform the transformation to apply to the quad
     * @return a Pair containing the BakedQuad and null cull face
     */
    public static Pair<BakedQuad, Direction> createReversedQuad(
            Direction face,
            Vector3f min,
            Vector3f max,
            TextureAtlasSprite sprite,
            BlockyModelGeometry.FaceTextureLayout texLayout,
            Vector3f size,
            Transformation transform) {

        QuadBakingVertexConsumer baker = setupQuadBaker(face, sprite);

        float[][] uvCoords = calculateUVCoordinates(face, texLayout, size, sprite);
        Vector3f[] vertices = getFaceVertices(face, min, max);

        // Bake in reverse order with flipped normals
        bakeVerticesReversed(baker, vertices, uvCoords, face, transform);
        // No cull, it breaks the backface
        return Pair.of(baker.bakeQuad(), null);
    }

    /**
     * Set up the quad baker with common parameters
     *
     * @param face   the Direction of the face
     * @param sprite the TextureAtlasSprite to use for the quad
     * @return a QuadBakingVertexConsumer instance
     */
    private static QuadBakingVertexConsumer setupQuadBaker(Direction face, TextureAtlasSprite sprite) {
        QuadBakingVertexConsumer baker = new QuadBakingVertexConsumer();
        baker.setSprite(sprite);
        baker.setDirection(face);
        baker.setTintIndex(TINT_INDEX_NONE);
        baker.setShade(true);
        return baker;
    }

    /**
     * Create border quads for debugging (4 thin strips around the edge)
     *
     * @param face      the Direction of the face
     * @param min       the minimum coordinates of the face
     * @param max       the maximum coordinates of the face
     * @param sprite    the TextureAtlasSprite to use for the border quads
     * @param transform the transformation to apply to each border quad
     * @return a list of BakedQuad instances representing the border strips
     */
    public static List<BakedQuad> createBorderQuads(
            Direction face,
            Vector3f min,
            Vector3f max,
            TextureAtlasSprite sprite,
            Transformation transform) {

        List<BakedQuad> borderQuads = new ArrayList<>();
        Vector3f[] vertices = getFaceVertices(face, min, max);

        float[] color = getDebugColor(face);
        float[][] dummyUVs = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        float borderWidth = 0.05f;

        for (int i = 0; i < 4; i++) {
            QuadBakingVertexConsumer baker = setupQuadBaker(face, sprite);
            Vector3f[] borderVerts = createBorderStrip(vertices, i, borderWidth);
            bakeBorderVertices(baker, borderVerts, dummyUVs, face, transform, color);
            borderQuads.add(baker.bakeQuad());
        }

        return borderQuads;
    }

    /**
     * Create a border strip along one edge of the quad
     *
     * @param vertices Original quad vertices
     * @param edge     Edge index (0=bottom, 1=right, 2=top, 3=left)
     * @param width    Width of the border strip
     * @return Vertices of the border strip quad
     */
    private static Vector3f[] createBorderStrip(Vector3f[] vertices, int edge, float width) {
        Vector3f[] strip = new Vector3f[4];

        int v1 = (edge + 1) % 4;

        Vector3f start = new Vector3f(vertices[edge]);
        Vector3f end = new Vector3f(vertices[v1]);

        // Calculate inward direction
        Vector3f center = new Vector3f(vertices[0]).add(vertices[1]).add(vertices[2]).add(vertices[3]).mul(0.25f);
        Vector3f toCenter = new Vector3f(start).add(end).mul(0.5f);
        toCenter.sub(center);
        toCenter.normalize().mul(width);

        strip[0] = new Vector3f(start);
        strip[1] = new Vector3f(end);
        strip[2] = new Vector3f(end).add(toCenter);
        strip[3] = new Vector3f(start).add(toCenter);

        return strip;
    }


    /**
     * Calculate UV coordinates for a face
     *
     * @param face   the Direction of the face
     * @param layout the texture layout for the face
     * @param size   the size of the face
     * @param sprite the TextureAtlasSprite to use for UV calculation
     * @return an array of UV coordinates for each vertex of the face
     */
    private static float[][] calculateUVCoordinates(Direction face, BlockyModelGeometry.FaceTextureLayout layout,
                                                    Vector3f size, TextureAtlasSprite sprite) {
        UVSize uvSize = getUVSizeForFace(face, size, layout.angle());
        UVBounds bounds = calculateUVBounds(layout, uvSize, sprite);
        return rotateUVCoordinates(bounds, layout.angle(), layout.mirrorX(), layout.mirrorY());
    }

    /**
     * Get UV size based on face orientation
     *
     * @param face  the Direction of the face
     * @param size  the size of the face
     * @param angle the rotation angle
     * @return the UVSize for the given face and rotation
     */
    private static UVSize getUVSizeForFace(Direction face, Vector3f size, int angle) {
        UVSize baseSize = switch (face) {
            case UP, DOWN -> new UVSize(size.x, size.z);
            case WEST, EAST -> new UVSize(size.z, size.y);
            default -> new UVSize(size.x, size.y);
        };

        // Swap dimensions for 90/270 degree rotations
//        if (angle == 90 || angle == 270) {
//            return new UVSize(baseSize.v, baseSize.u);
//        }
        return baseSize;
    }

    /**
     * Calculate UV bounds using layout and sprite
     *
     * @param layout the texture layout for the face
     * @param uvSize the UV size for the face
     * @param sprite the TextureAtlasSprite to use for UV calculation
     * @return the UV bounds for the given layout and sprite
     */
    private static UVBounds calculateUVBounds(BlockyModelGeometry.FaceTextureLayout layout, UVSize uvSize,
                                              TextureAtlasSprite sprite) {
        int textureWidth = sprite.contents().width();
        int textureHeight = sprite.contents().height();

        // Calculate base UV bounds
        float uMin = layout.offsetX() / (float) textureWidth;
        float vMin = layout.offsetY() / (float) textureHeight;
        float uMax = (layout.offsetX() + (layout.mirrorX() ? -uvSize.u : uvSize.u)) / (float) textureWidth;
        float vMax = (layout.offsetY() + (layout.mirrorY() ? -uvSize.v : uvSize.v)) / (float) textureHeight;

        // Apply mirroring
        if (layout.mirrorX()) {
            float temp = uMin;
            uMin = uMax;
            uMax = temp;
        }
        if (layout.mirrorY()) {
            float temp = vMin;
            vMin = vMax;
            vMax = temp;
        }

        // Map to sprite atlas
        uMin = sprite.getU0() + (sprite.getU1() - sprite.getU0()) * uMin;
        vMin = sprite.getV0() + (sprite.getV1() - sprite.getV0()) * vMin;
        uMax = sprite.getU0() + (sprite.getU1() - sprite.getU0()) * uMax;
        vMax = sprite.getV0() + (sprite.getV1() - sprite.getV0()) * vMax;
        return new UVBounds(uMin, vMin, uMax, vMax);
    }

    /**
     * Rotate UV coordinates based on angle
     *
     * @param bounds  the UV bounds
     * @param angle   the rotation angle
     * @param mirrorX whether to mirror horizontally
     * @param mirrorY whether to mirror vertically
     * @return the rotated UV coordinates
     */
    private static float[][] rotateUVCoordinates(UVBounds bounds, int angle, boolean mirrorX, boolean mirrorY) {
        float[][] uvs = new float[4][2];
        if (angle != 0) {
            float[][] rotated = rotatePair(bounds.uMin, bounds.vMin, bounds.uMin, bounds.vMin, angle);
            uvs[0] = rotated[0];
            rotated = rotatePair(bounds.uMax, bounds.vMin, bounds.uMin, bounds.vMin, angle);
            uvs[1] = rotated[0];
            rotated = rotatePair(bounds.uMax, bounds.vMax, bounds.uMin, bounds.vMin, angle);
            uvs[2] = rotated[0];
            rotated = rotatePair(bounds.uMin, bounds.vMax, bounds.uMin, bounds.vMin, angle);
            uvs[3] = rotated[0];
        } else {
            uvs[0] = new float[]{bounds.uMin, bounds.vMax};
            uvs[1] = new float[]{bounds.uMax, bounds.vMax};
            uvs[2] = new float[]{bounds.uMax, bounds.vMin};
            uvs[3] = new float[]{bounds.uMin, bounds.vMin};

        }
        if (mirrorX) {
            for (int i = 0; i < 4; i++) {
                float temp = uvs[i][0];
                uvs[i][0] = bounds.uMin + bounds.uMax - temp;  // Flip around center
            }
        }
        if (mirrorY) {
            for (int i = 0; i < 4; i++) {
                float temp = uvs[i][1];
                uvs[i][1] = bounds.vMin + bounds.vMax - temp;  // Flip around center
            }
        }

        return uvs;
    }

    /**
     * Rotate a single UV pair around a pivot point
     *
     * @param u      the U coordinate
     * @param v      the V coordinate
     * @param pivotU the pivot U coordinate
     * @param pivotV the pivot V coordinate
     * @param angle  the rotation angle
     * @return the rotated UV coordinates
     */
    private static float[][] rotatePair(float u, float v, float pivotU, float pivotV, int angle) {
        float sin = (float) Math.sin(Math.toRadians(angle));
        float cos = (float) Math.cos(Math.toRadians(angle));
        float rotatedU = pivotU + (u - pivotU) * cos - (v - pivotV) * sin;
        float rotatedV = pivotV + (u - pivotU) * sin + (v - pivotV) * cos;
        return new float[][]{{rotatedU, rotatedV}};
    }

    /**
     * Get the 4 vertices for a given face
     *
     * @param face the Direction of the face
     * @param min  the minimum coordinates of the face
     * @param max  the maximum coordinates of the face
     * @return an array of 4 Vector3f vertices
     */
    private static Vector3f[] getFaceVertices(Direction face, Vector3f min, Vector3f max) {
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

    /**
     * Bake vertices for the quad
     *
     * @param baker     the QuadBakingVertexConsumer
     * @param vertices  the vertices of the quad
     * @param uvCoords  the UV coordinates for the quad
     * @param face      the Direction of the face
     * @param transform the transformation to apply
     */
    private static void bakeVertices(QuadBakingVertexConsumer baker, Vector3f[] vertices,
                                     float[][] uvCoords, Direction face, Transformation transform) {
        Vector3f normal = new Vector3f(face.getStepX(), face.getStepY(), face.getStepZ());
        boolean hasTransform = !transform.isIdentity();

        for (int i = 0; i < 4; i++) {
            Vector4f pos = new Vector4f(vertices[i].x, vertices[i].y, vertices[i].z, 1.0f);
            Vector3f norm = new Vector3f(normal);

            if (hasTransform) {
                transform.transformPosition(pos);
                transform.transformNormal(norm);
            }

            baker.addVertex(pos.x(), pos.y(), pos.z());
            baker.setColor(COLOR_WHITE[0], COLOR_WHITE[1], COLOR_WHITE[2], COLOR_WHITE[3]);
            baker.setUv(uvCoords[i][0], uvCoords[i][1]);
            baker.setNormal(norm.x(), norm.y(), norm.z());
        }
    }

    /**
     * Bake vertices in reverse order for backfaces
     *
     * @param baker     the QuadBakingVertexConsumer
     * @param vertices  the vertices of the quad
     * @param uvCoords  the UV coordinates for the quad
     * @param face      the Direction of the face
     * @param transform the transformation to apply
     */
    private static void bakeVerticesReversed(QuadBakingVertexConsumer baker, Vector3f[] vertices,
                                             float[][] uvCoords, Direction face, Transformation transform) {
        Vector3f normal = new Vector3f(-face.getStepX(), -face.getStepY(), -face.getStepZ());
        boolean hasTransform = !transform.isIdentity();

        // Reverse order
        for (int i = 3; i >= 0; i--) {
            Vector4f pos = new Vector4f(vertices[i].x, vertices[i].y, vertices[i].z, 1.0f);
            Vector3f norm = new Vector3f(normal);

            if (hasTransform) {
                transform.transformPosition(pos);
                transform.transformNormal(norm);
            }

            baker.addVertex(pos.x(), pos.y(), pos.z());
            baker.setColor(COLOR_WHITE[0], COLOR_WHITE[1], COLOR_WHITE[2], COLOR_WHITE[3]);
            baker.setUv(uvCoords[i][0], uvCoords[i][1]);
            baker.setNormal(norm.x(), norm.y(), norm.z());
        }
    }

    /**
     * Bake border vertices for debugging
     *
     * @param baker     the QuadBakingVertexConsumer
     * @param vertices  the vertices of the border strip
     * @param uvCoords  the UV coordinates for the border strip
     * @param face      the Direction of the face
     * @param transform the transformation to apply
     * @param color     the color to use for debugging
     */
    private static void bakeBorderVertices(QuadBakingVertexConsumer baker, Vector3f[] vertices,
                                           float[][] uvCoords, Direction face, Transformation transform, float[] color) {
        Vector3f normal = new Vector3f(face.getStepX(), face.getStepY(), face.getStepZ());
        boolean hasTransform = !transform.isIdentity();

        // Offset vertices slightly outward to prevent z-fighting
        Vector3f offset = new Vector3f(normal).mul(BORDER_THICKNESS);

        for (int i = 0; i < 4; i++) {
            Vector3f offsetVert = new Vector3f(vertices[i]).add(offset);
            Vector4f pos = new Vector4f(offsetVert.x, offsetVert.y, offsetVert.z, 1.0f);
            Vector3f norm = new Vector3f(normal);

            if (hasTransform) {
                transform.transformPosition(pos);
                transform.transformNormal(norm);
            }

            baker.addVertex(pos.x(), pos.y(), pos.z());
            baker.setColor(color[0], color[1], color[2], color[3]);
            baker.setUv(uvCoords[i][0], uvCoords[i][1]);
            baker.setNormal(norm.x(), norm.y(), norm.z());
        }
    }

    /**
     * Get debug color for vertex based on index and face
     * Creates a colored border effect to visualize quad boundaries
     *
     * @param face the Direction of the face
     * @return the RGBA color array
     */
    private static float[] getDebugColor(Direction face) {
        // Assign different colors to different faces
        return switch (face) {
            case UP -> new float[]{1.0f, 0.0f, 0.0f, 1.0f};      // Red
            case DOWN -> new float[]{0.0f, 1.0f, 0.0f, 1.0f};    // Green
            case NORTH -> new float[]{0.0f, 0.0f, 1.0f, 1.0f};   // Blue
            case SOUTH -> new float[]{1.0f, 1.0f, 0.0f, 1.0f};   // Yellow
            case WEST -> new float[]{1.0f, 0.0f, 1.0f, 1.0f};    // Magenta
            case EAST -> new float[]{0.0f, 1.0f, 1.0f, 1.0f};    // Cyan
        };
    }

    /**
     * Calculate cull face based on quad orientation
     *
     * @param face the Direction of the face
     * @param min  the minimum coordinates of the face
     * @param max  the maximum coordinates of the face
     * @return the cull face Direction, or null if none
     */
    private static Direction calculateCullFace(Direction face, Vector3f min, Vector3f max) {
        // Currently disabled
        return null;
    }

    // Helper records for clean data passing
    private record UVSize(float u, float v) {
    }

    private record UVBounds(float uMin, float vMin, float uMax, float vMax) {
    }
}