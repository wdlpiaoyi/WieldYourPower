package net.wieldyourpower.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.wieldyourpower.util.NoUpdateMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@link LevelNotifyMixin} alone does not silence the server. {@code ServerLevel} overrides every
 * neighbour-notify entry point and forwards it to its {@code NeighborUpdater}
 * ({@code ServerLevel.updateNeighborsAt} calls {@code neighborUpdater.updateNeighborsAtExceptFromFacing},
 * the two {@code neighborChanged} overloads are one-line forwards), while the {@code Level} bodies are
 * only a bare Forge event hook ({@code updateNeighborsAt}) or a literal {@code return}
 * (both {@code neighborChanged} overloads). Virtual dispatch therefore never reaches the {@code Level}
 * injections on a server level, which is why a redstone torch could still retract a piston with the mode
 * on: {@code RedstoneTorchBlock.onRemove} calls {@code Level.updateNeighborsAt}, which resolves to the
 * {@code ServerLevel} override. This mixin mirrors the four entry points on {@code ServerLevel} itself.
 *
 * <p>Cancelling at the entry point rather than at the {@code NeighborUpdater} implementation is
 * deliberate: the update is then never queued in the first place, so it cannot be replayed after the
 * action window has closed. {@code blockUpdated} is left alone because it only forwards to
 * {@code updateNeighborsAt}, and {@code sendBlockUpdated} is never touched, so clients keep receiving the
 * changed block itself and no ghost blocks appear.</p>
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelNotifyMixin {

    @Inject(
            method = "updateNeighborsAt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void wieldyourpower$serverNoUpdateNeighborsAt(BlockPos pos, Block block, CallbackInfo ci) {
        if (NoUpdateMode.isSuppressing((ServerLevel) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "updateNeighborsAtExceptFromFacing(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/Direction;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void wieldyourpower$serverNoUpdateNeighborsAtExceptFromFacing(BlockPos pos, Block block, Direction skipDirection, CallbackInfo ci) {
        if (NoUpdateMode.isSuppressing((ServerLevel) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "neighborChanged(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void wieldyourpower$serverNoNeighborChanged(BlockPos pos, Block block, BlockPos fromPos, CallbackInfo ci) {
        if (NoUpdateMode.isSuppressing((ServerLevel) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "neighborChanged(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos;Z)V",
            at = @At("HEAD"),
            cancellable = true)
    private void wieldyourpower$serverNoNeighborChangedState(BlockState state, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving, CallbackInfo ci) {
        if (NoUpdateMode.isSuppressing((ServerLevel) (Object) this)) {
            ci.cancel();
        }
    }
}
