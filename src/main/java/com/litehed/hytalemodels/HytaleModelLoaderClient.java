package com.litehed.hytalemodels;

import com.litehed.hytalemodels.modelstuff.BlockyModelLoader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.resources.VanillaClientListeners;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = HytaleModelLoader.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = HytaleModelLoader.MODID, value = Dist.CLIENT)
public class HytaleModelLoaderClient {
    public HytaleModelLoaderClient(ModContainer container) {
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterLoaders(ModelEvent.RegisterLoaders event) {
        event.register(BlockyModelLoader.ID, BlockyModelLoader.INSTANCE);
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(BlockyModelLoader.ID, BlockyModelLoader.INSTANCE);
        event.addDependency(BlockyModelLoader.ID, VanillaClientListeners.MODELS);
    }
}
