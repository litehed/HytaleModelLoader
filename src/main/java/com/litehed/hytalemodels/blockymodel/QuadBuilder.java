package com.litehed.hytalemodels.blockymodel;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class QuadBuilder {

    private static final int TINT_INDEX_NONE = -1;

    private QuadBuilder() {
    }

    /**
     * Creates the baked quad for one face
     *
     * @param face      the direction of the face
     * @param min       the minimum corner of the shape
     * @param max       the maximum corner of the shape
     * @param sprite    the sprite the quad uses
     * @param texLayout the texture layout for this face
     * @param size      the original size of the shape, used for UVs
     * @param transform the transformation to apply to the quad
     * @param reversed  true to wind the face the other way, for negative stretch and back faces
     * @return the baked quad
     */
    public static BakedQuad createQuad(
            Direction face,
            Vector3f min,
            Vector3f max,
            TextureAtlasSprite sprite,
            BlockyModelGeometry.FaceTextureLayout texLayout,
            Vector3f size,
            Transformation transform,
            boolean reversed) {

        QuadBakingVertexConsumer baker = new QuadBakingVertexConsumer();
        baker.setSprite(sprite);
        baker.setDirection(face);
        baker.setTintIndex(TINT_INDEX_NONE);
        baker.setShade(true);

        float[][] uvCoords = calculateUVCoordinates(face, texLayout, size, sprite);
        Vector3f[] vertices = getFaceVertices(face, min, max);

        bakeVertices(baker, vertices, uvCoords, face, transform, reversed);
        return baker.bakeQuad();
    }

    /**
     * Calculates the UV coordinates of a face inside an atlas sprite
     *
     * @param face   the direction of the face
     * @param layout the texture layout for the face
     * @param size   the original size of the shape
     * @param sprite the sprite to map into
     * @return the UV coordinates of the 4 vertices
     */
    public static float[][] calculateUVCoordinates(Direction face, BlockyModelGeometry.FaceTextureLayout layout,
                                                   Vector3f size, TextureAtlasSprite sprite) {
        return calculateUVCoordinates(face, layout, size, UVRegion.of(sprite));
    }

    /**
     * Calculates the UV coordinates of a face inside any texture region
     *
     * @param face   the direction of the face
     * @param layout the texture layout for the face
     * @param size   the original size of the shape
     * @param region the region to map into, such as a whole standalone texture
     * @return the UV coordinates of the 4 vertices
     */
    public static float[][] calculateUVCoordinates(Direction face, BlockyModelGeometry.FaceTextureLayout layout,
                                                   Vector3f size, UVRegion region) {
        UVSize uvSize = getUVSizeForFace(face, size);
        UVBounds bounds = calculateUVBounds(layout, uvSize, region);
        return transformUVCoordinates(bounds, layout, uvSize, region);
    }

    private static UVSize getUVSizeForFace(Direction face, Vector3f size) {
        return switch (face) {
            case UP, DOWN -> new UVSize(size.x, size.z);
            case WEST, EAST -> new UVSize(size.z, size.y);
            default -> new UVSize(size.x, size.y);
        };
    }

    private static UVBounds calculateUVBounds(BlockyModelGeometry.FaceTextureLayout layout, UVSize uvSize,
                                              UVRegion region) {
        int textureWidth = region.width();
        int textureHeight = region.height();

        float uMin = layout.offsetX() / (float) textureWidth;
        float vMin = layout.offsetY() / (float) textureHeight;
        float uMax = (layout.offsetX() + (layout.mirrorX() ? -uvSize.u : uvSize.u)) / (float) textureWidth;
        float vMax = (layout.offsetY() + (layout.mirrorY() ? -uvSize.v : uvSize.v)) / (float) textureHeight;

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

        // Map into the texture region
        uMin = region.u0() + (region.u1() - region.u0()) * uMin;
        vMin = region.v0() + (region.v1() - region.v0()) * vMin;
        uMax = region.u0() + (region.u1() - region.u0()) * uMax;
        vMax = region.v0() + (region.v1() - region.v0()) * vMax;
        return new UVBounds(uMin, vMin, uMax, vMax);
    }

    private static float[][] transformUVCoordinates(
            UVBounds bounds,
            BlockyModelGeometry.FaceTextureLayout layout,
            UVSize uvSize,
            UVRegion region) {

        int textureWidth = region.width();
        int textureHeight = region.height();
        int angle = layout.angle();
        boolean mirrorX = layout.mirrorX();
        boolean mirrorY = layout.mirrorY();

        float[][] uvs = new float[4][2];

        if (angle != 0) {
            float pivotUPx = layout.offsetX();
            float pivotVPx = layout.offsetY();

            float uSize = mirrorX ? -uvSize.u : uvSize.u;
            float vSize = mirrorY ? -uvSize.v : uvSize.v;

            float[][] cornersPx = new float[4][2];
            cornersPx[0] = new float[]{pivotUPx, pivotVPx};                 // Top-left (pivot)
            cornersPx[1] = new float[]{pivotUPx + uSize, pivotVPx};         // Top-right
            cornersPx[2] = new float[]{pivotUPx + uSize, pivotVPx + vSize}; // Bottom-right
            cornersPx[3] = new float[]{pivotUPx, pivotVPx + vSize};         // Bottom-left

            for (int i = 0; i < 4; i++) {
                float[] rotated = rotatePointClockwise(
                        cornersPx[i][0], cornersPx[i][1],
                        pivotUPx, pivotVPx,
                        angle
                );

                uvs[i][0] = normalizeU(rotated[0], region, textureWidth);
                uvs[i][1] = normalizeV(rotated[1], region, textureHeight);
            }
        } else {
            uvs[0] = new float[]{bounds.uMin, bounds.vMax};
            uvs[1] = new float[]{bounds.uMax, bounds.vMax};
            uvs[2] = new float[]{bounds.uMax, bounds.vMin};
            uvs[3] = new float[]{bounds.uMin, bounds.vMin};

            if (mirrorX) {
                float[] temp = uvs[0];
                uvs[0] = uvs[1];
                uvs[1] = temp;

                temp = uvs[3];
                uvs[3] = uvs[2];
                uvs[2] = temp;
            }
            if (mirrorY) {
                float[] temp = uvs[0];
                uvs[0] = uvs[3];
                uvs[3] = temp;

                temp = uvs[1];
                uvs[1] = uvs[2];
                uvs[2] = temp;
            }
        }

        return uvs;
    }

    private static float[] rotatePointClockwise(float x, float y, float pivotX, float pivotY, int angle) {
        float dx = x - pivotX;
        float dy = y - pivotY;

        float newDx, newDy;
        newDy = switch (angle) {
            case 90 -> {
                newDx = -dy;
                yield dx;
            }
            case 180 -> {
                newDx = -dx;
                yield -dy;
            }
            case 270 -> {
                newDx = dy;
                yield -dx;
            }
            default -> {
                newDx = dx;
                yield dy;
            }
        };

        return new float[]{pivotX + newDx, pivotY + newDy};
    }

    private static float normalizeU(float pixelU, UVRegion region, int textureWidth) {
        float relativeU = pixelU / textureWidth;
        return region.u0() + (region.u1() - region.u0()) * relativeU;
    }

    private static float normalizeV(float pixelV, UVRegion region, int textureHeight) {
        float relativeV = pixelV / textureHeight;
        return region.v0() + (region.v1() - region.v0()) * relativeV;
    }

    /**
     * Gets the 4 vertices of a face
     *
     * @param face the direction of the face
     * @param min  the minimum corner of the shape
     * @param max  the maximum corner of the shape
     * @return the 4 vertices
     */
    public static Vector3f[] getFaceVertices(Direction face, Vector3f min, Vector3f max) {
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

    private static void bakeVertices(QuadBakingVertexConsumer baker, Vector3f[] vertices, float[][] uvCoords,
                                     Direction face, Transformation transform, boolean reversed) {
        int sign = reversed ? -1 : 1;
        Vector3f normal = new Vector3f(face.getStepX() * sign, face.getStepY() * sign, face.getStepZ() * sign);
        boolean hasTransform = !transform.isIdentity();

        for (int n = 0; n < 4; n++) {
            int i = reversed ? 3 - n : n;
            Vector4f pos = new Vector4f(vertices[i].x, vertices[i].y, vertices[i].z, 1.0f);
            Vector3f norm = new Vector3f(normal);

            if (hasTransform) {
                transform.transformPosition(pos);
                transform.transformNormal(norm);
            }

            baker.addVertex(pos.x(), pos.y(), pos.z());
            baker.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            baker.setUv(uvCoords[i][0], uvCoords[i][1]);
            baker.setNormal(norm.x(), norm.y(), norm.z());
        }
    }

    private record UVSize(float u, float v) {
    }

    private record UVBounds(float uMin, float vMin, float uMax, float vMax) {
    }
}