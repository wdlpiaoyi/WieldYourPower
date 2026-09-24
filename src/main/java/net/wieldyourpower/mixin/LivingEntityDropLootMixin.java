package net.wieldyourpower.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.wieldyourpower.common.AuthorsFavorEvents;
import net.wieldyourpower.common.CreativeDefenseEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops loot from being dropped for an entity that is not actually dying.
 *
 * <p>A mod that notices our blocked death often calls {@code dropAllDeathLoot} itself (creative
 * defence, author's-favor removal resistance), so the death guard alone is not enough. A favored
 * entity that is still fully alive must not drop loot either, otherwise a protected boss survives but
 * still spills its kill rewards.</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDropLootMixin {

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void wieldyourpower$blockUnwantedLoot(DamageSource source, CallbackInfo callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (CreativeDefenseEvents.shouldBlockDamage(self)) {
            callback.cancel();
            return;
        }
        if (!self.isDeadOrDying() && AuthorsFavorEvents.hasFavored() && AuthorsFavorEvents.isFavored(self)) {
            callback.cancel();
        }
    }
}
