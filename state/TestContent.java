package com.litehed.hytalemodels;

import com.litehed.hytalemodels.blocks.ChestSmallBlock;
import com.litehed.hytalemodels.blocks.entity.ChestSmallBlockEntity;
import com.litehed.hytalemodels.entities.Goblin;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Example chest and goblin used to try the loader in game
 */
public final class TestContent {

    public static final String MOD_ID = "hytalemodelloader";

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);

    public static final DeferredBlock<ChestSmallBlock> CHEST_SMALL = BLOCKS.registerBlock("chest_small",
            ChestSmallBlock::new, BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOD));

    public static final DeferredItem<BlockItem> CHEST_SMALL_ITEM =
            ITEMS.registerSimpleBlockItem("chest_small", CHEST_SMALL);

    public static final Supplier<BlockEntityType<ChestSmallBlockEntity>> CHEST_SMALL_BE =
            BLOCK_ENTITY_TYPES.register("chest_small", () ->
                    BlockEntityType.Builder.of(ChestSmallBlockEntity::new, CHEST_SMALL.get()).build(null));

    public static final Supplier<EntityType<Goblin>> GOBLIN = ENTITY_TYPES.register("goblin", () ->
            EntityType.Builder.of(Goblin::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.6F)
                    .clientTrackingRange(8)
                    .build("goblin"));

    private TestContent() {
    }

    /**
     * Registers the test content, call it once from the mod constructor
     *
     * @param modBus the mod event bus
     */
    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        ENTITY_TYPES.register(modBus);
        modBus.addListener(TestContent::addCreative);
        modBus.addListener(TestContent::registerAttributes);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            TestContentClient.register(modBus);
        }
    }

    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(CHEST_SMALL_ITEM);
        }
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(GOBLIN.get(), Goblin.createAttributes().build());
    }
}