package net.wieldyourpower.mixin;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.item.context.UseOnContext;
import net.wieldyourpower.util.NoUpdateMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Same as {@link BucketItemMixin} for solid buckets (powder snow), which place through
 * {@code SolidBucketItem.useOn} instead of {@code BucketItem.use}.
 */
@Mixin(SolidBucketItem.class)
public abstract class SolidBucketItemMixin {

    @Inject(
            method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"))
    private void wieldyourpower$beginNoUpdate(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        if (player != null && !context.getLevel().isClientSide && NoUpdateMode.isActiveFor(player)) {
            NoUpdateMode.beginWindow();
        }
    }

    @Inject(
            method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At("RETURN"))
    private void wieldyourpower$endNoUpdate(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        if (player != null && !context.getLevel().isClientSide && NoUpdateMode.isActiveFor(player)) {
            NoUpdateMode.endWindow();
        }
    }
}
