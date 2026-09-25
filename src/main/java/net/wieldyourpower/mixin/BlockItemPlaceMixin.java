package net.wieldyourpower.mixin;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.wieldyourpower.util.NoUpdateMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Opens the "no update" window around a server-side block placement, so only the placing player's own
 * action is suppressed.
 */
@Mixin(BlockItem.class)
public abstract class BlockItemPlaceMixin {

    @Inject(
            method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"))
    private void wieldyourpower$beginNoUpdate(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        if (player != null && !player.level().isClientSide && NoUpdateMode.isActiveFor(player)) {
            NoUpdateMode.beginWindow();
        }
    }

    @Inject(
            method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At("RETURN"))
    private void wieldyourpower$endNoUpdate(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        if (player != null && !player.level().isClientSide && NoUpdateMode.isActiveFor(player)) {
            NoUpdateMode.endWindow();
        }
    }
}
