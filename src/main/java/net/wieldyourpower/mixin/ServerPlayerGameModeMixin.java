package net.wieldyourpower.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.wieldyourpower.util.NoUpdateMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Opens the "no update" window around a server player's own block break and block interaction, so only
 * that player's action is suppressed (never another player's).
 *
 * <p>The break window covers the vanilla removal (plus this mod's force-break paths, which open their
 * own window). The interaction window covers every right-click that can change blocks - bone meal,
 * flint and steel, axes, hoes, shovels, doors, trapdoors, buttons, levers, candles, campfires, TNT,
 * respawn anchors - because all of them run through {@code useItemOn}.</p>
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

    private static final String USE_ITEM_ON =
            "useItemOn(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;"
                    + "Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;"
                    + "Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;";

    @Shadow
    @Final
    protected ServerPlayer player;

    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"))
    private void wieldyourpower$beginNoUpdate(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (NoUpdateMode.isActiveFor(this.player)) {
            NoUpdateMode.beginWindow();
        }
    }

    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", at = @At("RETURN"))
    private void wieldyourpower$endNoUpdate(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (NoUpdateMode.isActiveFor(this.player)) {
            NoUpdateMode.endWindow();
        }
    }

    @Inject(method = USE_ITEM_ON, at = @At("HEAD"))
    private void wieldyourpower$beginNoUpdateInteraction(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand,
                                                         BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (NoUpdateMode.isActiveFor(player)) {
            NoUpdateMode.beginWindow();
        }
    }

    @Inject(method = USE_ITEM_ON, at = @At("RETURN"))
    private void wieldyourpower$endNoUpdateInteraction(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand,
                                                       BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (NoUpdateMode.isActiveFor(player)) {
            NoUpdateMode.endWindow();
        }
    }
}
