package net.wieldyourpower.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.wieldyourpower.compat.BlockProtectionBypass;

/**
 * Server-side "hard" block removal shared by the creative bypass and the admin Block Breaker.
 *
 * <p>Unlike a normal break it bypasses hardness, {@code getDestroyProgress} overrides and protection
 * events. It runs the block's own destroy hook (so container contents / linked data are handled), then
 * removes the block outright. Multi-block structures are not special-cased: break the other half if
 * needed.</p>
 *
 * <p>A protection mod may keep the block in place at the {@code Level}/{@code LevelChunk} level; the
 * removal is wrapped in {@link BlockProtectionBypass}, a keyword-driven switch that disables such a
 * guard for the duration when one is present.</p>
 */
public final class ForceBlockBreak {

    private ForceBlockBreak() {
    }

    public static boolean breakBlock(Level level, BlockPos pos, Player player, boolean drop) {
        if (level.isClientSide) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }

        boolean bypass = BlockProtectionBypass.enter();
        try {
            // Let the block run its own destroy logic (e.g. dropping an altar's contents).
            try {
                state.getBlock().playerWillDestroy(level, pos, state, player);
            } catch (Throwable ignored) {
            }

            state = level.getBlockState(pos);
            if (state.isAir()) {
                return true;
            }

            if (drop) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                Block.dropResources(state, level, pos, blockEntity, player, ItemStack.EMPTY);
            }

            // Do not remove the block entity first: the block's own onRemove (which drops container
            // contents, as vanilla does) needs it still present. setBlock removes it afterwards.
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            if (!level.getBlockState(pos).isAir()) {
                forceChunkClear(level, pos, state);
            }
            return true;
        } finally {
            BlockProtectionBypass.exit(bypass);
        }
    }

    /**
     * Deeper fallback: write the chunk directly and push the change to clients, bypassing any guard that
     * sits on {@code Level.setBlock}.
     */
    private static void forceChunkClear(Level level, BlockPos pos, BlockState oldState) {
        try {
            LevelChunk chunk = level.getChunkAt(pos);
            BlockState air = Blocks.AIR.defaultBlockState();
            chunk.setBlockState(pos, air, false);
            level.sendBlockUpdated(pos, oldState, air, 3);
            level.updateNeighborsAt(pos, oldState.getBlock());
        } catch (Throwable ignored) {
        }
    }
}
