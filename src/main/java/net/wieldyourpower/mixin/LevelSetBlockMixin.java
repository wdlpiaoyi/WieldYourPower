package net.wieldyourpower.mixin;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.wieldyourpower.util.NoUpdateMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * While the "no update" mode is suppressing, drop the neighbour-notify bit and set the known-shape bit
 * on every {@code Level.setBlock}, so neither {@code neighborChanged} nor the neighbour shape updates
 * run. Observers react to shape updates ({@code ObserverBlock.updateShape}) and redstone/pistons to
 * {@code neighborChanged}, so both are silenced, while the block itself still goes to clients.
 */
@Mixin(Level.class)
public abstract class LevelSetBlockMixin {

    @ModifyVariable(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"),
            argsOnly = true,
            index = 3)
    private int wieldyourpower$noUpdateFlags(int flags) {
        Level level = (Level) (Object) this;
        if (NoUpdateMode.isSuppressing(level)) {
            return (flags & ~Block.UPDATE_NEIGHBORS) | Block.UPDATE_KNOWN_SHAPE;
        }
        return flags;
    }
}
