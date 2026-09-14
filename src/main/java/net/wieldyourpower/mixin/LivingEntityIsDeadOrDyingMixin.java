package net.wieldyourpower.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.wieldyourpower.common.CreativeDefenseEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hides death from a protected creative player on both sides. {@code DeathScreen} is shown purely because
 * the client thinks it is dead, so reporting {@code false} here prevents the screen even when some mod
 * shoved the synced health to {@code -Infinity} (this is exactly how MoreAvaritia's Infinity armor does it).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityIsDeadOrDyingMixin {

    @Inject(method = "isDeadOrDying", at = @At("HEAD"), cancellable = true)
    private void wieldyourpower$hideCreativeDeath(CallbackInfoReturnable<Boolean> callback) {
        if (!CreativeDefenseEvents.hasProtected()) {
            return;
        }
        if (CreativeDefenseEvents.isProtectedCreative((LivingEntity) (Object) this)) {
            callback.setReturnValue(false);
        }
    }
}
