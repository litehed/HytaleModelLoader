package com.litehed.hytalemodels.init;

import com.litehed.hytalemodels.HytaleModelLoader;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ItemInit {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HytaleModelLoader.MODID);

    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("pot", BlockInit.POT);
    public static final DeferredItem<BlockItem> CRYSTAL = ITEMS.registerSimpleBlockItem("crystal_big", BlockInit.CRYSTAL_BIG);
    public static final DeferredItem<BlockItem> BED = ITEMS.registerSimpleBlockItem("bed", BlockInit.BED);
    public static final DeferredItem<BlockItem> COFFIN = ITEMS.registerSimpleBlockItem("coffin", BlockInit.COFFIN);
    public static final DeferredItem<BlockItem> SLOPE = ITEMS.registerSimpleBlockItem("slope", BlockInit.SLOPE);
    public static final DeferredItem<BlockItem> CHAIR = ITEMS.registerSimpleBlockItem("chair", BlockInit.CHAIR);
    public static final DeferredItem<BlockItem> TABLE = ITEMS.registerSimpleBlockItem("table", BlockInit.TABLE);

    public static final DeferredItem<Item> ADAMANTITE_PICK = ITEMS.registerSimpleItem("adamantite");
}
