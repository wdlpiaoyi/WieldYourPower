package net.wieldyourpower.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.wieldyourpower.util.NoUpdateMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Opens the "no update" window around a bucket use, so placing or picking up a fluid (any
 * {@code BucketItem}, including modded fluid buckets) is suppressed like a normal place/break. The
 * actual fluid write goes through {@code Level.setBlock}, which the window's flag rewrite covers.
 */
@Mixin(BucketItem.class)
public abstract class BucketItemMixin {

    @Inject(
            method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;",
            at = @At("HEAD"))
    private void wieldyourpower$beginNoUpdate(Level level, Player player, InteractionHand hand,
                                              CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (!level.isClientSide && NoUpdateMode.isActiveFor(player)) {
            NoUpdateMode.beginWindow();
        }
    }

    @Inject(
            method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;",
            at = @At("RETURN"))
    private void wieldyourpower$endNoUpdate(Level level, Player player, InteractionHand hand,
                                            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (!level.isClientSide && NoUpdateMode.isActiveFor(player)) {
            NoUpdateMode.endWindow();
        }
    }
}
