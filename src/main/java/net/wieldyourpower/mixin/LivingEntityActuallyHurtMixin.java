package net.wieldyourpower.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.wieldyourpower.common.CreativeDefenseEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks damage application itself for a protected creative player. Some attacks never reach
 * {@code LivingAttackEvent}/{@code LivingHurtEvent} (a mod can call {@code actuallyHurt} directly, or tag
 * its damage to bypass invulnerability and the hurt cooldown), so cancelling the events alone is not enough.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityActuallyHurtMixin {

    @Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
    private void wieldyourpower$blockCreativeDamage(DamageSource source, float amount, CallbackInfo callback) {
        if (CreativeDefenseEvents.shouldBlockDamage((LivingEntity) (Object) this)) {
            callback.cancel();
        }
    }
}
