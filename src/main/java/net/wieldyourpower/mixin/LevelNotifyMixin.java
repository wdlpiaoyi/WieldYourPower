package net.wieldyourpower.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.wieldyourpower.util.NoUpdateMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Second layer of the "no update" suppression. {@link LevelSetBlockMixin} only reworks the flags of
 * {@code Level.setBlock}, which covers the vanilla notify path, but a whole family of blocks notifies
 * its neighbours by hand from {@code onPlace}/{@code onRemove} - {@code RedstoneTorchBlock} (and its
 * wall variant) and {@code RedStoneWireBlock} loop over every direction calling
 * {@code Level.updateNeighborsAt}, {@code DiodeBlock} calls {@code updateNeighborsInFront} and
 * {@code LeverBlock} calls {@code updateNeighbours}. Those calls run from
 * {@code LevelChunk.setBlockState} no matter which flags were passed in, so flag surgery alone cannot
 * silence them: a redstone torch could still retract a piston while the mode was on.
 *
 * <p><b>Important:</b> this mixin alone is not enough on a server. {@code ServerLevel} overrides all four
 * notify entry points (and forwards them to its {@code NeighborUpdater}), while the bodies here are only
 * the Forge event hook or a literal {@code return}, so virtual dispatch on a server level never reaches
 * these injections - {@link ServerLevelNotifyMixin} mirrors them on {@code ServerLevel} and is what
 * actually silences redstone there. Keep both.</p>
 *
 * <p>While the mode is suppressing, these notify entry points are dropped outright, which also covers
 * comparator/observer output updates ({@code updateNeighbourForOutputSignal}, which {@code ServerLevel}
 * does <i>not</i> override, so that one does take effect here). Client sync is
 * deliberately untouched: {@code sendBlockUpdated} still runs, so the changed block itself reaches
 * every client and no ghost blocks appear.</p>
 */
@Mixin(Level.class)
public abstract class LevelNotifyMixin {

    @Inject(
            method = "updateNeighborsAt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void wieldyourpower$noUpdateNeighborsAt(BlockPos pos, Block block, CallbackInfo ci) {
        if (NoUpdateMode.isSuppressing((Level) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "updateNeighborsAtExceptFromFacing(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/Direction;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void wieldyourpower$noUpdateNeighborsAtExceptFromFacing(BlockPos pos, Block block, Direction skipDirection, CallbackInfo ci) {
        if (NoUpdateMode.isSuppressing((Level) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "neighborChanged(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void wieldyourpower$noNeighborChanged(BlockPos pos, Block block, BlockPos fromPos, CallbackInfo ci) {
        if (NoUpdateMode.isSuppressing((Level) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "neighborChanged(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos;Z)V",
            at = @At("HEAD"),
            cancellable = true)
    private void wieldyourpower$noNeighborChangedState(BlockState state, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving, CallbackInfo ci) {
        if (NoUpdateMode.isSuppressing((Level) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "updateNeighbourForOutputSignal(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void wieldyourpower$noNeighbourOutputSignal(BlockPos pos, Block block, CallbackInfo ci) {
        if (NoUpdateMode.isSuppressing((Level) (Object) this)) {
            ci.cancel();
        }
    }
}
