package com.litehed.hytalemodels.blocks;

import com.litehed.hytalemodels.TestContent;
import com.litehed.hytalemodels.api.block.HytaleBlockBase;
import com.litehed.hytalemodels.blocks.entity.ChestSmallBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * A chest that opens and closes on right click, with no inventory
 */
public class ChestSmallBlock extends HytaleBlockBase implements EntityBlock {

    public ChestSmallBlock(Properties properties) {
        super(properties);
    }

    // The renderer draws the chest, so the block model is not drawn in the world
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChestSmallBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (!level.isClientSide || type != TestContent.CHEST_SMALL_BE.get()) {
            return null;
        }
        return (lvl, pos, st, blockEntity) -> ((ChestSmallBlockEntity) blockEntity).clientTick();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ChestSmallBlockEntity chest) {
            chest.toggle();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}