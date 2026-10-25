package com.litehed.hytalemodels.api.block.entity;

import com.litehed.hytalemodels.HytaleModelLoader;
import com.litehed.hytalemodels.api.BlockyPose;
import com.litehed.hytalemodels.api.block.HytaleBlockBase;
import com.litehed.hytalemodels.blockymodel.BlockyModelGeometry;
import com.litehed.hytalemodels.blockymodel.BlockyModelLoader;
import com.litehed.hytalemodels.blockymodel.PreparedBlockyModel;
import com.litehed.hytalemodels.blockymodel.TransformCalculator;
import com.litehed.hytalemodels.blockymodel.animations.BlockyAnimationLoader;
import com.litehed.hytalemodels.blockymodel.animations.BlockyAnimationPlayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class HytaleBlockEntityRenderer<T extends HytaleBlockEntity, S extends HytaleBlockEntityRenderState>
        implements BlockEntityRenderer<T, S> {

    // Renderers are recreated on resource reload so these stay fresh
    private final Map<Identifier, Optional<PreparedBlockyModel>> preparedCache = new HashMap<>();
    private final Map<Identifier, Optional<BlockyAnimationPlayer>> playerCache = new HashMap<>();

    public HytaleBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void extractRenderState(T blockEntity, S renderState, float partialTick,
                                   Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);

        renderState.modelName = blockEntity.getModelName();
        renderState.modelNamespace = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()).getNamespace();
        renderState.animationTick = blockEntity.getAnimationTick();
        renderState.partialTick = partialTick;
        renderState.ageInTicks = (blockEntity.getAnimationTick() + partialTick) * getAnimationUnitsPerTick();

        if (blockEntity.getLevel() != null) {
            BlockState blockState = blockEntity.getLevel().getBlockState(blockEntity.getBlockPos());
            if (blockState.hasProperty(HytaleBlockBase.FACING)) {
                renderState.facing = blockState.getValue(HytaleBlockBase.FACING);
            }
        }

        extractAdditionalRenderState(blockEntity, renderState, partialTick);
    }

    /**
     * Copies extra data from the block entity into the render state
     *
     * @param blockEntity the block entity being rendered
     * @param renderState the render state to fill
     * @param partialTick the current partial tick
     */
    protected void extractAdditionalRenderState(T blockEntity, S renderState, float partialTick) {
    }

    @Override
    public void submit(S renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState cameraRenderState) {
        if (renderState.modelName == null || renderState.modelNamespace == null) {
            return;
        }

        PreparedBlockyModel prepared = getPrepared(renderState.modelNamespace, renderState.modelName);
        if (prepared == null) {
            return;
        }

        BlockyPose pose = new BlockyPose();
        setupAnim(renderState, pose);

        poseStack.pushPose();

        // Model origin is the middle of the bottom face of the block
        poseStack.translate(0.5, 0.5 - TransformCalculator.ORIGIN_Y_OFFSET / TransformCalculator.UNITS_PER_BLOCK, 0.5);

        float yRotDegrees = getFacingYRotation(renderState.facing);
        if (yRotDegrees != 0f) {
            poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(yRotDegrees)));
        }

        prepared.submit(poseStack, submitNodeCollector, getRenderType(), renderState.lightCoords,
                OverlayTexture.NO_OVERLAY, -1, pose);

        poseStack.popPose();
    }

    /**
     * Y rotation in degrees for a facing. North is 180, east 90, west 270 and south 0
     *
     * @param facing the facing of the block, null if it has none
     * @return the rotation in degrees
     */
    protected float getFacingYRotation(Direction facing) {
        if (facing == null) {
            return 0f;
        }
        return switch (facing) {
            case NORTH -> 180f;
            case EAST -> 90f;
            case WEST -> 270f;
            default -> 0f;
        };
    }

    /**
     * Poses the model for this frame
     * Everything added to the pose is layered on the rest pose
     *
     * @param renderState the render state, ageInTicks is the animation time in blockyanim units
     * @param pose        the pose to add to
     */
    protected void setupAnim(S renderState, BlockyPose pose) {
    }

    /**
     * Animation units that pass per animation tick
     * The animation tick of the block entity is multiplied by this
     *
     * @return animation units per tick, 4 by default
     */
    protected float getAnimationUnitsPerTick() {
        return 4.0f;
    }

    /**
     * Location of the model file
     *
     * @param namespace the namespace of the block entity's mod
     * @param modelName the name of the model
     * @return the location of the .blockymodel file
     */
    protected Identifier getModelLocation(String namespace, String modelName) {
        return Identifier.fromNamespaceAndPath(namespace, "models/" + modelName + ".blockymodel");
    }

    /**
     * Texture used by the model
     *
     * @param namespace the namespace of the block entity's mod
     * @param modelName the name of the model
     * @return the texture material
     */
    protected Material getTextureMaterial(String namespace, String modelName) {
        return new Material(TextureAtlas.LOCATION_BLOCKS,
                Identifier.fromNamespaceAndPath(namespace, "block/" + modelName + "_texture"));
    }

    /**
     * Render type used to draw the model
     *
     * @return the render type
     */
    protected RenderType getRenderType() {
        return RenderTypes.cutoutMovingBlock();
    }

    /**
     * Gets an animation player, cached per renderer
     *
     * @param animId the location of the animation file
     * @return the player, or null if the animation can't be loaded
     */
    protected BlockyAnimationPlayer getOrCreatePlayer(Identifier animId) {
        return playerCache.computeIfAbsent(animId, id ->
                Optional.ofNullable(BlockyAnimationLoader.INSTANCE.loadAnimation(id)).map(BlockyAnimationPlayer::new)
        ).orElse(null);
    }

    private PreparedBlockyModel getPrepared(String namespace, String modelName) {
        Identifier modelLocation = getModelLocation(namespace, modelName);
        return preparedCache.computeIfAbsent(modelLocation, location -> prepare(location, namespace, modelName))
                .orElse(null);
    }

    private Optional<PreparedBlockyModel> prepare(Identifier modelLocation, String namespace, String modelName) {
        try {
            BlockyModelGeometry geometry = BlockyModelLoader.INSTANCE.loadGeometry(
                    new BlockyModelGeometry.Settings(modelLocation));
            TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
                    .get(getTextureMaterial(namespace, modelName));
            return Optional.of(PreparedBlockyModel.create(geometry, sprite));
        } catch (Exception e) {
            // Cached as empty so it only logs once
            HytaleModelLoader.LOGGER.error("Failed to prepare model {}: {}", modelLocation, e.getMessage());
            return Optional.empty();
        }
    }
}