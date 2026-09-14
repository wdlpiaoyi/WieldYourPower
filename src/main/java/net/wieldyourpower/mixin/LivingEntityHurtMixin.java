package net.wieldyourpower.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.wieldyourpower.common.CreativeDefenseEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hard-blocks {@code hurt} for a protected creative player before any damage-source tag
 * (TrialMonolith tags its attacks {@code bypasses_invulnerability}/{@code bypasses_cooldown}) or event can
 * matter. This is the same layer MoreAvaritia's Infinity armor uses.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHurtMixin {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void wieldyourpower$blockCreativeHurt(DamageSource source, float amount,
                                                  CallbackInfoReturnable<Boolean> callback) {
        if (CreativeDefenseEvents.shouldBlockDamage((LivingEntity) (Object) this)) {
            callback.setReturnValue(false);
        }
    }
}
