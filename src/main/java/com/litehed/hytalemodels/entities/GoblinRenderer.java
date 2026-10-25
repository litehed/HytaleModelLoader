package com.litehed.hytalemodels.entities;

import com.litehed.hytalemodels.api.BlockyPose;
import com.litehed.hytalemodels.api.entity.HytaleEntityRenderer;
import com.litehed.hytalemodels.blockymodel.animations.BlockyAnimationPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class GoblinRenderer extends HytaleEntityRenderer<Goblin> {

    private static final String[] ATTACKS = {"bite", "swing_left", "swing_right"};

    public GoblinRenderer(EntityRendererProvider.Context context) {
        super(context, 0.4F);
    }

    @Override
    protected String getModelName(Goblin entity) {
        return "goblin";
    }

    // The death animation does the falling over, so vanilla's tip over is turned off
    @Override
    protected float getFlipDegrees(Goblin entity) {
        return 0.0F;
    }

    @Override
    protected void setupAnim(Goblin entity, BlockyPose pose, float limbSwing, float limbSwingAmount,
                             float ageInTicks, float netHeadYaw, float headPitch) {
        float units = getAnimationUnitsPerTick();
        float partialTick = ageInTicks - entity.tickCount;
        boolean moving = limbSwingAmount > 0.02F;

        if (entity.isDeadOrDying()) {
            pose.play(getAnimation(entity, "death"), (entity.deathTime + partialTick) * units);
        } else if (entity.hurtTime > 0) {
            float elapsed = entity.hurtDuration - entity.hurtTime + partialTick;
            pose.play(getAnimation(entity, "hurt"), elapsed * units);
        } else if (entity.swinging) {
            String attack = ATTACKS[Math.floorMod(entity.getId(), ATTACKS.length)] + (moving ? "_moving" : "");
            BlockyAnimationPlayer player = getAnimation(entity, attack);
            if (player != null) {
                pose.play(player, entity.getAttackAnim(partialTick) * player.getAnimationDuration());
            }
        } else {
            pose.play(getAnimation(entity, moving ? "walk" : "idle"), ageInTicks * units);
        }
    }
}