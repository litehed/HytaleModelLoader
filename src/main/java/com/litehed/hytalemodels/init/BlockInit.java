package com.litehed.hytalemodels.init;

import com.litehed.hytalemodels.HytaleModelLoader;
import com.litehed.hytalemodels.blocks.HytaleChest;
import com.litehed.hytalemodels.blocks.HytaleTestBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BlockInit {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HytaleModelLoader.MODID);

    public static final DeferredBlock<Block> POT = BLOCKS.registerSimpleBlock("pot", () -> BlockBehaviour.Properties.of().noOcclusion());
    public static final DeferredBlock<Block> CRYSTAL_BIG = BLOCKS.registerSimpleBlock("crystal_big", () -> BlockBehaviour.Properties.of().noOcclusion());
    public static final DeferredBlock<Block> BED = BLOCKS.registerSimpleBlock("bed", () -> BlockBehaviour.Properties.of().noOcclusion());
    public static final DeferredBlock<Block> COFFIN = BLOCKS.registerSimpleBlock("coffin", () -> BlockBehaviour.Properties.of().noOcclusion());
    public static final DeferredBlock<Block> SLOPE = BLOCKS.registerBlock("slope", HytaleTestBlock::new);
    public static final DeferredBlock<Block> SMALL_CHEST = BLOCKS.registerBlock("chest_small", HytaleChest::new);
    public static final DeferredBlock<Block> CHAIR = BLOCKS.registerSimpleBlock("chair", () -> BlockBehaviour.Properties.of().noOcclusion());
    public static final DeferredBlock<Block> TABLE = BLOCKS.registerSimpleBlock("table", () -> BlockBehaviour.Properties.of().noOcclusion());
}
