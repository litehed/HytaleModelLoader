package com.litehed.hytalemodels;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(HytaleModelLoader.MODID)
public class HytaleModelLoader {
    public static final String MODID = "hytalemodelloader";
    public static final Logger LOGGER = LogUtils.getLogger();

    public HytaleModelLoader(IEventBus modEventBus, ModContainer modContainer) {

    }
}
