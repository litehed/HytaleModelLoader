package com.litehed.hytalemodels.blocks.entity;

import com.litehed.hytalemodels.TestContent;
import com.litehed.hytalemodels.api.BlockyPose;
import com.litehed.hytalemodels.api.block.entity.HytaleBlockEntityRenderer;
import com.litehed.hytalemodels.blockymodel.animations.BlockyAnimationPlayer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class ChestSmallRenderer extends HytaleBlockEntityRenderer<ChestSmallBlockEntity> {

    public ChestSmallRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void setupAnim(ChestSmallBlockEntity chest, BlockyPose pose, float animationTime) {
        boolean open = chest.isOpen();
        BlockyAnimationPlayer player = getOrCreatePlayer(animation(open ? "chest_open" : "chest_close"));
        if (player == null) {
            return;
        }

        float duration = player.getAnimationDuration();
        if (open) {
            // Stay on the last frame, even if the file is set to loop
            pose.play(player, Math.min(animationTime, Math.nextDown(duration)));
        } else if (animationTime < duration) {
            // Once it has finished closing the rest pose takes over
            pose.play(player, animationTime);
        }
    }

    private static ResourceLocation animation(String name) {
        return ResourceLocation.fromNamespaceAndPath(TestContent.MOD_ID,
                "animations/chest_small/" + name + ".blockyanim");
    }
}