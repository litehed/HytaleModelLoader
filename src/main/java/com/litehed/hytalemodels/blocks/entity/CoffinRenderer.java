package com.litehed.hytalemodels.blocks.entity;

import com.litehed.hytalemodels.HytaleModelLoader;
import com.litehed.hytalemodels.blockymodel.BlockyModelGeometry;
import com.litehed.hytalemodels.blockymodel.animations.BlockyAnimationPlayer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public class CoffinRenderer extends HytaleBlockEntityRenderer<CoffinBlockEntity, AnimatedChestRenderState> {

    private static final Identifier ANIM_OPEN =
            Identifier.fromNamespaceAndPath(HytaleModelLoader.MODID,
                    "animations/coffin/coffin_open.blockyanim");

    private static final Identifier ANIM_CLOSE =
            Identifier.fromNamespaceAndPath(HytaleModelLoader.MODID,
                    "animations/coffin/coffin_close.blockyanim");

    public CoffinRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public AnimatedChestRenderState createRenderState() {
        return new AnimatedChestRenderState();
    }

    @Override
    protected void extractAdditionalRenderState(CoffinBlockEntity blockEntity,
                                                AnimatedChestRenderState renderState,
                                                float partialTick) {
        renderState.isOpen = blockEntity.isOpen();
    }

    @Override
    protected Map<String, NodeTransform> calculateAnimationTransforms(AnimatedChestRenderState renderState,
                                                                      BlockyModelGeometry geometry) {
        Map<String, NodeTransform> transforms = new HashMap<>();

        Identifier animId = renderState.isOpen ? ANIM_OPEN : ANIM_CLOSE;

        BlockyAnimationPlayer player = getOrCreatePlayer(animId);
        if (player != null) {
            transforms.putAll(player.calculateTransforms(renderState.ageInTicks));
        }

        return transforms;
    }
}