package net.wieldyourpower.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.wieldyourpower.util.NoUpdateMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Opens the "no update" window around a server player's own block break, so only that player's action
 * is suppressed (never another player's).
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

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
}
