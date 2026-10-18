package com.litehed.hytalemodels.init;

import com.litehed.hytalemodels.HytaleModelLoader;
import com.litehed.hytalemodels.entities.GoblinEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class EntityInit {

    public static final DeferredRegister.Entities ENTITY_TYPES =
            DeferredRegister.createEntities(HytaleModelLoader.MODID);

    // MISC so it never spawns naturally and never despawns; summon with /summon hytalemodelloader:goblin
    public static final Supplier<EntityType<GoblinEntity>> GOBLIN = ENTITY_TYPES.registerEntityType(
            "goblin",
            GoblinEntity::new,
            MobCategory.MISC,
            builder -> builder.sized(0.7f, 1.6f)
    );
}