package com.litehed.hytalemodels.api.entity;

import com.litehed.hytalemodels.HytaleModelLoader;
import com.litehed.hytalemodels.blockymodel.BlockyModelGeometry;
import com.litehed.hytalemodels.blockymodel.BlockyModelLoader;
import com.litehed.hytalemodels.blockymodel.PreparedBlockyModel;
import com.litehed.hytalemodels.blockymodel.UVRegion;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Base renderer for living entities drawn from a blockymodel. Static pose only for now
 *
 * @param <T> the entity type
 * @param <S> the render state type
 */
public abstract class HytaleEntityRenderer<T extends Mob, S extends HytaleEntityRenderState>
        extends MobRenderer<T, S, BlockyPlaceholderModel<S>> {

    // Renderers are recreated on resource reload, so this cache never outlives its textures
    private final Map<String, Optional<Prepared>> preparedCache = new HashMap<>();

    // Set for the duration of submit(); getRenderType() has no access to them otherwise
    private PoseStack activePoseStack;
    private SubmitNodeCollector activeCollector;

    protected HytaleEntityRenderer(EntityRendererProvider.Context context, float shadowRadius) {
        super(context, new BlockyPlaceholderModel<>(), shadowRadius);
    }

    /**
     * @param entity the entity being rendered
     * @return the model name
     */
    protected abstract String getModelName(T entity);

    /**
     * Uniform scale applied to the model on top of vanilla's scale attribute. Hytale models are drawn at
     * 32 units per block, which makes some characters larger than Minecraft mobs.
     *
     * @return the scale, 1.0 by default
     */
    protected float getModelScale() {
        return 1.0f;
    }

    @Override
    public void extractRenderState(T entity, S renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);
        renderState.modelName = getModelName(entity);
    }

    @Override
    public void submit(S renderState, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraRenderState) {
        this.activePoseStack = poseStack;
        this.activeCollector = collector;
        try {
            super.submit(renderState, poseStack, collector, cameraRenderState);
        } finally {
            this.activePoseStack = null;
            this.activeCollector = null;
        }
    }

    @Override
    protected @Nullable RenderType getRenderType(S renderState, boolean visible, boolean translucent, boolean glowing) {
        // Glowing-but-invisible would need vanilla's outline path, which custom geometry does not have
        if (activePoseStack != null && activeCollector != null && (visible || translucent)) {
            submitBlockyModel(renderState, translucent);
        }
        return null;
    }

    private void submitBlockyModel(S renderState, boolean translucent) {
        Prepared prepared = getOrPrepare(renderState.modelName);
        if (prepared == null) {
            return;
        }

        PoseStack poseStack = activePoseStack;
        poseStack.pushPose();

        // Vanilla's model space is Y-down with the origin 1.501 above the feet. Undo that so we are
        // back in the entity's own frame, with Y up and the origin at the feet
        poseStack.translate(0.0F, 1.501F, 0.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        // Vanilla models face -Z, Blocky models face +Z
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        float modelScale = getModelScale();
        poseStack.scale(modelScale, modelScale, modelScale);

        // Same values vanilla uses for submitModel
        RenderType renderType = translucent
                ? RenderTypes.itemEntityTranslucentCull(getEntityTexture(renderState.modelName))
                : prepared.renderType();
        int overlay = getOverlayCoords(renderState, getWhiteOverlayProgress(renderState));
        int tint = ARGB.multiply(translucent ? 654311423 : -1, getModelTint(renderState));

        prepared.model().submit(poseStack, activeCollector, renderType, renderState.lightCoords, overlay, tint);

        poseStack.popPose();
    }

    @Override
    public Identifier getTextureLocation(S renderState) {
        return getEntityTexture(renderState.modelName);
    }

    protected RenderType createRenderType(Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }

    protected Identifier getModelLocation(String modelName) {
        return Identifier.fromNamespaceAndPath(HytaleModelLoader.MODID, "models/" + modelName + ".blockymodel");
    }

    protected Identifier getEntityTexture(String modelName) {
        return Identifier.fromNamespaceAndPath(HytaleModelLoader.MODID, "textures/entity/" + modelName + ".png");
    }

    private Prepared getOrPrepare(String modelName) {
        if (modelName == null) {
            return null;
        }
        return preparedCache.computeIfAbsent(modelName, this::prepare).orElse(null);
    }

    private Optional<Prepared> prepare(String modelName) {
        Identifier modelLocation = getModelLocation(modelName);
        Identifier texture = getEntityTexture(modelName);
        try {
            BlockyModelGeometry geometry = BlockyModelLoader.INSTANCE.loadGeometry(
                    new BlockyModelGeometry.Settings(modelLocation));

            int[] size = readPngSize(texture);
            PreparedBlockyModel model = PreparedBlockyModel.create(geometry, UVRegion.fullTexture(size[0], size[1]));

            return Optional.of(new Prepared(model, createRenderType(texture)));
        } catch (Exception e) {
            // Cached as empty so a broken model logs once instead of every frame
            HytaleModelLoader.LOGGER.error("Failed to prepare model {} with texture {}: {}",
                    modelLocation, texture, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Reads a PNGs pixel size from its header without decoding the image
     *
     * @param texture the full resource path of the PNG
     * @return {width, height}
     * @throws IOException if the texture is missing or is not a valid PNG
     */
    private static int[] readPngSize(Identifier texture) throws IOException {
        Resource resource = Minecraft.getInstance().getResourceManager().getResource(texture)
                .orElseThrow(() -> new FileNotFoundException("Missing texture " + texture));
        try (DataInputStream in = new DataInputStream(resource.open())) {
            // 8 byte signature + 4 byte chunk length + 4 byte "IHDR", then width and height as big-endian ints
            in.skipNBytes(16);
            return new int[]{in.readInt(), in.readInt()};
        }
    }

    private record Prepared(PreparedBlockyModel model, RenderType renderType) {
    }
}