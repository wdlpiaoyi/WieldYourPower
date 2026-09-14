package net.wieldyourpower.mixin;

import net.minecraft.world.entity.Entity;
import net.wieldyourpower.common.AuthorsFavorEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks a favored entity from being removed through {@code remove(KILLED/DISCARDED)} at the call site,
 * so forced-removal weapons (e.g. MoreAvaritia InfinityGodSword) cannot outrun the per-tick undo.
 */
@Mixin(Entity.class)
public abstract class EntityRemoveMixin {

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void wieldyourpower$blockFavoredRemove(Entity.RemovalReason reason, CallbackInfo callback) {
        if (!AuthorsFavorEvents.hasFavored()) {
            return;
        }
        if (AuthorsFavorEvents.shouldResistRemoval((Entity) (Object) this, reason)) {
            callback.cancel();
        }
    }
}
