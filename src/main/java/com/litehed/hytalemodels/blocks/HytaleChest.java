package com.litehed.hytalemodels.blocks;

import com.litehed.hytalemodels.blocks.entity.AnimatedChestBlockEntity;
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
import org.jspecify.annotations.Nullable;

public class HytaleChest extends HytaleBlockBase implements EntityBlock {
    public HytaleChest(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new AnimatedChestBlockEntity(blockPos, blockState);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof AnimatedChestBlockEntity chest) {
                if (chest.isOpen()) {
                    chest.closeChest();
                } else {
                    chest.openChest();
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (!level.isClientSide()) return null;
        return (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof AnimatedChestBlockEntity chest) {
                AnimatedChestBlockEntity.clientTick(lvl, pos, blockState, chest);
            }
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
