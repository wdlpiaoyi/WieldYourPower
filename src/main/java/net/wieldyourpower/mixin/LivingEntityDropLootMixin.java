package net.wieldyourpower.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.wieldyourpower.common.CreativeDefenseEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops a protected creative player's inventory/equipment from being dropped. A mod that notices our
 * blocked death often calls {@code dropAllDeathLoot} itself, so the death guard alone is not enough.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDropLootMixin {

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void wieldyourpower$blockCreativeLoot(DamageSource source, CallbackInfo callback) {
        if (CreativeDefenseEvents.shouldBlockDamage((LivingEntity) (Object) this)) {
            callback.cancel();
        }
    }
}
