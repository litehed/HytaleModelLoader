package com.litehed.hytalemodels;

import com.litehed.hytalemodels.blocks.entity.ChestSmallRenderer;
import com.litehed.hytalemodels.entities.GoblinRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Client side of {@link TestContent}
 */
public final class TestContentClient {

    private TestContentClient() {
    }

    /**
     * Registers the test renderers
     *
     * @param modBus the mod event bus
     */
    public static void register(IEventBus modBus) {
        modBus.addListener(TestContentClient::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TestContent.CHEST_SMALL_BE.get(), ChestSmallRenderer::new);
        event.registerEntityRenderer(TestContent.GOBLIN.get(), GoblinRenderer::new);
    }
}