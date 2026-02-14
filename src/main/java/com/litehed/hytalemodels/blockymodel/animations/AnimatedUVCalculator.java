package com.litehed.hytalemodels.blockymodel.animations;

import com.litehed.hytalemodels.HytaleModelLoader;
import com.litehed.hytalemodels.blockymodel.BlockyModelGeometry;
import com.litehed.hytalemodels.blockymodel.QuadBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

import java.lang.reflect.Method;

public class AnimatedUVCalculator {

    private static Method calculateUVCoordinatesMethod;

    // Static block to initialize the reflection method
    static {
        try {
            calculateUVCoordinatesMethod = QuadBuilder.class.getDeclaredMethod(
                    "calculateUVCoordinates",
                    Direction.class,
                    BlockyModelGeometry.FaceTextureLayout.class,
                    Vector3f.class,
                    TextureAtlasSprite.class
            );
            calculateUVCoordinatesMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            HytaleModelLoader.LOGGER.error("Failed to find QuadBuilder.calculateUVCoordinates method: {}", e.getMessage());
        }
    }

    /**
     * Calculate UV coordinates for a face using QuadBuilder's logic
     * Returns a 2D array where each element is [u, v] for each of the 4 vertices
     *
     * @param direction    the face direction
     * @param texLayout    the texture layout for this face
     * @param originalSize the original size of the shape
     * @param sprite       the texture sprite
     * @return 2D array of UV coordinates [[u0,v0], [u1,v1], [u2,v2], [u3,v3]]
     */
    public static float[][] calculateUVs(Direction direction, BlockyModelGeometry.FaceTextureLayout texLayout,
                                         Vector3f originalSize, TextureAtlasSprite sprite) {
        if (calculateUVCoordinatesMethod != null) {
            try {
                return (float[][]) calculateUVCoordinatesMethod.invoke(
                        null, direction, texLayout, originalSize, sprite
                );
            } catch (Exception e) {
                HytaleModelLoader.LOGGER.error("Failed to invoke QuadBuilder.calculateUVCoordinates: {}", e.getMessage());
            }
        }

        return calculateUVsFallback(direction, texLayout, originalSize, sprite);
    }

    /**
     * Fallback method to calculate UVs if reflection fails. This is a simplified version and may not match QuadBuilder's logic exactly.
     *
     * @param direction    the face direction
     * @param texLayout    the texture layout for this face
     * @param originalSize the original size of the shape
     * @param sprite       the texture sprite
     * @return 2D array of UV coordinates [[u0,v0], [u1,v1], [u2,v2], [u3,v3]]
     */
    private static float[][] calculateUVsFallback(Direction direction, BlockyModelGeometry.FaceTextureLayout texLayout,
                                                  Vector3f originalSize, TextureAtlasSprite sprite) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        return new float[][]{
                {u0, v0},
                {u1, v0},
                {u1, v1},
                {u0, v1}
        };
    }
}