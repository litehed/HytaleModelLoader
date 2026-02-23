package com.litehed.hytalemodels.init;

import com.litehed.hytalemodels.HytaleModelLoader;
import com.litehed.hytalemodels.blocks.entity.AnimatedChestBlockEntity;
import com.litehed.hytalemodels.blocks.entity.CoffinBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class BlockEntityInit {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, HytaleModelLoader.MODID);

    public static final Supplier<BlockEntityType<AnimatedChestBlockEntity>> CHEST_TEST_ENT = BLOCK_ENTITIES.register(
            "hytale_chest", () -> new BlockEntityType<>(
                    AnimatedChestBlockEntity::new,
                    false,
                    BlockInit.SMALL_CHEST.get()
            )
    );

    public static final Supplier<BlockEntityType<CoffinBlockEntity>> COFFIN_ENT = BLOCK_ENTITIES.register(
            "coffin", () -> new BlockEntityType<>(
                    CoffinBlockEntity::new,
                    false,
                    BlockInit.COFFIN.get()
            )
    );
}
