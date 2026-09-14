package net.wieldyourpower.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.wieldyourpower.common.CreativeDefenseEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops a protected creative player from dying before the death event even fires. This defeats weapons
 * that make {@code LivingDeathEvent} uncancellable (e.g. RevelationFix's Valettein), because
 * {@code ServerPlayer.die} calls {@code ForgeHooks.onLivingDeath} as its very first action; cancelling at
 * HEAD means that event - and any mod hook riding on it - is never reached.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDieMixin {

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void wieldyourpower$blockCreativeDeath(DamageSource source, CallbackInfo callback) {
        if (CreativeDefenseEvents.shouldBlockDeath((ServerPlayer) (Object) this)) {
            callback.cancel();
        }
    }
}
