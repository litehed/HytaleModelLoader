package com.litehed.hytalemodels;

import com.litehed.hytalemodels.entities.GoblinEntity;
import com.litehed.hytalemodels.init.BlockEntityInit;
import com.litehed.hytalemodels.init.BlockInit;
import com.litehed.hytalemodels.init.EntityInit;
import com.litehed.hytalemodels.init.ItemInit;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(HytaleModelLoader.MODID)
public class HytaleModelLoader {
    public static final String MODID = "hytalemodelloader";
    public static final Logger LOGGER = LogUtils.getLogger();

    public HytaleModelLoader(IEventBus modEventBus, ModContainer modContainer) {
        // Remember to comment these out for version releases
        BlockInit.BLOCKS.register(modEventBus);
        BlockEntityInit.BLOCK_ENTITIES.register(modEventBus);
        ItemInit.ITEMS.register(modEventBus);
        EntityInit.ENTITY_TYPES.register(modEventBus);

        modEventBus.addListener(HytaleModelLoader::registerEntityAttributes);
    }

    private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(EntityInit.GOBLIN.get(), GoblinEntity.createAttributes().build());
    }
}
