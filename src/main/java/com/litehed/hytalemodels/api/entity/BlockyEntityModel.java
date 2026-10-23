package com.litehed.hytalemodels.api.entity;

import com.litehed.hytalemodels.api.BlockyPose;
import com.litehed.hytalemodels.blockymodel.PreparedBlockyModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Mob;

/**
 * The model vanilla's renderer calls to draw a Blocky entity
 * It hands the work to the {@link HytaleEntityRenderer} that owns it
 *
 * @param <T> the entity type
 */
public class BlockyEntityModel<T extends Mob> extends EntityModel<T> {

    // Vanilla puts the model origin this far above the feet
    private static final float VANILLA_MODEL_OFFSET = 1.501F;

    private HytaleEntityRenderer<T> renderer;
    private PreparedBlockyModel prepared;
    private BlockyPose pose = new BlockyPose();

    public BlockyEntityModel() {
        super(RenderType::entityCutout);
    }

    void bind(HytaleEntityRenderer<T> renderer) {
        this.renderer = renderer;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        prepared = renderer.getPrepared(entity);
        pose = new BlockyPose();
        if (prepared != null) {
            renderer.setupAnim(entity, pose, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               int color) {
        if (prepared == null) {
            return;
        }

        poseStack.pushPose();

        // Undo vanilla's flipped model space
        poseStack.translate(0.0F, VANILLA_MODEL_OFFSET, 0.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        // Vanilla models face -Z, Blocky models face +Z
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        float scale = renderer.getModelScale();
        poseStack.scale(scale, scale, scale);

        prepared.render(poseStack, buffer, packedLight, packedOverlay, color, pose);

        poseStack.popPose();
    }
}