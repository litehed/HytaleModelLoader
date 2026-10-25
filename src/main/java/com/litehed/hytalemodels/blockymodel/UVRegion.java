package com.litehed.hytalemodels.blockymodel;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * The rectangle of a texture that a model's pixel-based UV layout is mapped into
 *
 * @param u0     left edge, in 0-1 texture space
 * @param v0     top edge, in 0-1 texture space
 * @param u1     right edge, in 0-1 texture space
 * @param v1     bottom edge, in 0-1 texture space
 * @param width  width of the region in pixels
 * @param height height of the region in pixels
 */
public record UVRegion(float u0, float v0, float u1, float v1, int width, int height) {

    /**
     * @param sprite an atlas sprite
     * @return the region covered by the sprite inside its atlas
     */
    public static UVRegion of(TextureAtlasSprite sprite) {
        return new UVRegion(sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(),
                sprite.contents().width(), sprite.contents().height());
    }

    /**
     * @param width  texture width in pixels
     * @param height texture height in pixels
     * @return a region covering an entire standalone texture
     */
    public static UVRegion fullTexture(int width, int height) {
        return new UVRegion(0.0f, 0.0f, 1.0f, 1.0f, width, height);
    }
}