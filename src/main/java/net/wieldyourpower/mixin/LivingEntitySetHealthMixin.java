package net.wieldyourpower.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.wieldyourpower.common.AuthorsFavorEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Intercepts every {@code LivingEntity.setHealth(float)} call so the author's favor mitigation is computed
 * at the call site instead of being reverse-engineered from health deltas on the entity tick.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntitySetHealthMixin {

    @ModifyVariable(method = "setHealth", at = @At("HEAD"), argsOnly = true)
    private float wieldyourpower$mitigateSetHealth(float health) {
        return AuthorsFavorEvents.mitigateSetHealth((LivingEntity) (Object) this, health);
    }
}
