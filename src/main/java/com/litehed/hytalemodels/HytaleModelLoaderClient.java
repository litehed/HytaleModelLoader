package com.litehed.hytalemodels;

import com.litehed.hytalemodels.blockymodel.BlockyModelLoader;
import com.litehed.hytalemodels.blockymodel.animations.BlockyAnimationLoader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = HytaleModelLoader.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = HytaleModelLoader.MODID, value = Dist.CLIENT)
public class HytaleModelLoaderClient {
    public HytaleModelLoaderClient(ModContainer container) {
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(BlockyModelLoader.ID, BlockyModelLoader.INSTANCE);
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(BlockyModelLoader.INSTANCE);
        event.registerReloadListener(BlockyAnimationLoader.INSTANCE);
    }
}
