package net.wieldyourpower.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.wieldyourpower.WYPConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lets creative players place a block even when the target position is occupied by an entity (another
 * mob, a boat, ...). Vanilla refuses this inside {@code BlockItem.canPlace} through
 * {@code Level.isUnobstructed} (that check skips the placing player but counts every other
 * {@code blocksBuilding} entity), and it happens before any placement event, so the event-level
 * bypass in {@code BlockPlacementEvents} cannot cover it.
 *
 * <p>Only the entity-obstruction part is dropped; {@code mustSurvive}/{@code canSurvive} stay vanilla.
 * Gated by {@code creativePlacesThroughEntities}.</p>
 */
@Mixin(BlockItem.class)
public abstract class BlockItemCanPlaceMixin {

    @Redirect(
            method = "canPlace",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;isUnobstructed(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Z"))
    private boolean wieldyourpower$ignoreEntityObstruction(Level level, BlockState state, BlockPos pos,
                                                          CollisionContext context, BlockPlaceContext placeContext) {
        Player player = placeContext.getPlayer();
        if (WYPConfig.COMMON.creativePlacesThroughEntities.get() && player != null && player.isCreative()) {
            return true;
        }
        return level.isUnobstructed(state, pos, context);
    }
}
