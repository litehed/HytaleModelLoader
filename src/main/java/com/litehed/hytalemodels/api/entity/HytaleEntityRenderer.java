package com.litehed.hytalemodels.api.entity;

import com.litehed.hytalemodels.HytaleModelLoader;
import com.litehed.hytalemodels.api.BlockyPose;
import com.litehed.hytalemodels.blockymodel.BlockyModelGeometry;
import com.litehed.hytalemodels.blockymodel.BlockyModelLoader;
import com.litehed.hytalemodels.blockymodel.PreparedBlockyModel;
import com.litehed.hytalemodels.blockymodel.UVRegion;
import com.litehed.hytalemodels.blockymodel.animations.BlockyAnimationLoader;
import com.litehed.hytalemodels.blockymodel.animations.BlockyAnimationPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Mob;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Base renderer for mobs drawn from a .blockymodel
 *
 * @param <T> the entity type
 */
public abstract class HytaleEntityRenderer<T extends Mob> extends MobRenderer<T, BlockyEntityModel<T>> {

    // Renderers are recreated on resource reload so these stay fresh
    private final Map<ResourceLocation, Optional<PreparedBlockyModel>> preparedCache = new HashMap<>();
    private final Map<ResourceLocation, Optional<BlockyAnimationPlayer>> animationCache = new HashMap<>();

    protected HytaleEntityRenderer(EntityRendererProvider.Context context, float shadowRadius) {
        super(context, new BlockyEntityModel<>(), shadowRadius);
        this.model.bind(this);
    }

    /**
     * @param entity the entity being rendered
     * @return the model name, resolved to models/name.blockymodel
     */
    protected abstract String getModelName(T entity);

    /**
     * Scale of the whole model, on top of the scale attribute
     *
     * @return the scale, 1.0 by default
     */
    protected float getModelScale() {
        return 1.0f;
    }

    /**
     * Animation units that pass per game tick, same as the block entity renderers
     * Multiply ageInTicks by this when playing an animation
     *
     * @return animation units per tick, 4 by default
     */
    protected float getAnimationUnitsPerTick() {
        return 4.0f;
    }

    /**
     * Poses the model for this frame, the same values vanilla passes to a model's setupAnim
     * Everything added to the pose is layered on the rest pose
     *
     * @param entity          the entity being rendered
     * @param pose            the pose to add to
     * @param limbSwing       how far the entity has walked, for leg swing
     * @param limbSwingAmount how fast the entity is walking, from 0 to 1
     * @param ageInTicks      the age of the entity in ticks, including the partial tick
     * @param netHeadYaw      the head yaw relative to the body, in degrees
     * @param headPitch       the head pitch, in degrees
     */
    protected void setupAnim(T entity, BlockyPose pose, float limbSwing, float limbSwingAmount, float ageInTicks,
                             float netHeadYaw, float headPitch) {
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return getEntityTexture(getNamespace(entity), getModelName(entity));
    }

    @Override
    @Nullable
    protected RenderType getRenderType(T entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        ResourceLocation texture = getTextureLocation(entity);
        if (translucent) {
            return RenderType.itemEntityTranslucentCull(texture);
        }
        if (bodyVisible) {
            return createRenderType(texture);
        }
        return glowing ? RenderType.outline(texture) : null;
    }

    /**
     * Render type used to draw the model. Override for no culling or transparency
     *
     * @param texture the model's texture
     * @return the render type
     */
    protected RenderType createRenderType(ResourceLocation texture) {
        return RenderType.entityCutout(texture);
    }

    /**
     * Loads a keyframe animation for the entity's model, cached per renderer
     *
     * @param entity        the entity being rendered
     * @param animationName the animation's file name without extension. Must be lower-case
     * @return the animation, or null if it is missing or invalid
     */
    @Nullable
    protected BlockyAnimationPlayer getAnimation(T entity, String animationName) {
        ResourceLocation location = getAnimationLocation(getNamespace(entity), getModelName(entity), animationName);
        return animationCache.computeIfAbsent(location, id ->
                Optional.ofNullable(BlockyAnimationLoader.INSTANCE.loadAnimation(id)).map(BlockyAnimationPlayer::new)
        ).orElse(null);
    }

    protected ResourceLocation getModelLocation(String namespace, String modelName) {
        return ResourceLocation.fromNamespaceAndPath(namespace, "models/" + modelName + ".blockymodel");
    }

    /**
     * @return the full resource path of the model's PNG texture
     */
    protected ResourceLocation getEntityTexture(String namespace, String modelName) {
        return ResourceLocation.fromNamespaceAndPath(namespace, "textures/entity/" + modelName + ".png");
    }

    protected ResourceLocation getAnimationLocation(String namespace, String modelName, String animationName) {
        return ResourceLocation.fromNamespaceAndPath(namespace,
                "animations/" + modelName + "/" + animationName + ".blockyanim");
    }

    private String getNamespace(T entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace();
    }

    @Nullable
    PreparedBlockyModel getPrepared(T entity) {
        String namespace = getNamespace(entity);
        String modelName = getModelName(entity);
        ResourceLocation key = ResourceLocation.fromNamespaceAndPath(namespace, modelName);
        return preparedCache.computeIfAbsent(key, id -> prepare(namespace, modelName)).orElse(null);
    }

    private Optional<PreparedBlockyModel> prepare(String namespace, String modelName) {
        ResourceLocation modelLocation = getModelLocation(namespace, modelName);
        ResourceLocation texture = getEntityTexture(namespace, modelName);
        try {
            BlockyModelGeometry geometry = BlockyModelLoader.INSTANCE.loadGeometry(
                    new BlockyModelGeometry.Settings(modelLocation));

            int[] size = readPngSize(texture);
            return Optional.of(PreparedBlockyModel.create(geometry, UVRegion.fullTexture(size[0], size[1])));
        } catch (Exception e) {
            // Cached as empty so it only logs once
            HytaleModelLoader.LOGGER.error("Failed to prepare model {} with texture {}: {}",
                    modelLocation, texture, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Reads a PNG's size from its header without decoding the image
     *
     * @param texture the full resource path of the PNG
     * @return {width, height}
     * @throws IOException if the texture is missing or is not a valid PNG
     */
    private static int[] readPngSize(ResourceLocation texture) throws IOException {
        Resource resource = Minecraft.getInstance().getResourceManager().getResource(texture)
                .orElseThrow(() -> new FileNotFoundException("Missing texture " + texture));
        try (DataInputStream in = new DataInputStream(resource.open())) {
            // Skip the signature and chunk header, then read width and height
            in.skipNBytes(16);
            return new int[]{in.readInt(), in.readInt()};
        }
    }
}