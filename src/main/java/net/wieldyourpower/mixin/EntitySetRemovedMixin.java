package net.wieldyourpower.mixin;

import net.minecraft.world.entity.Entity;
import net.wieldyourpower.common.AuthorsFavorEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Same as {@code EntityRemoveMixin} but for code that calls the final {@code setRemoved} directly
 * (also used by MoreAvaritia's forced removal).
 */
@Mixin(Entity.class)
public abstract class EntitySetRemovedMixin {

    @Inject(method = "setRemoved", at = @At("HEAD"), cancellable = true)
    private void wieldyourpower$blockFavoredSetRemoved(Entity.RemovalReason reason, CallbackInfo callback) {
        if (!AuthorsFavorEvents.hasFavored()) {
            return;
        }
        if (AuthorsFavorEvents.shouldResistRemoval((Entity) (Object) this, reason)) {
            callback.cancel();
        }
    }
}
