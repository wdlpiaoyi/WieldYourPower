package net.wieldyourpower.mixin;

import net.minecraft.world.entity.Entity;
import net.wieldyourpower.common.AuthorsFavorEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks the forge removal hook for a favored entity that is being removed through KILLED/DISCARDED
 * (MoreAvaritia's superAttack calls it directly).
 */
@Mixin(Entity.class)
public abstract class EntityOnRemovedFromWorldMixin {

    @Inject(method = "onRemovedFromWorld", at = @At("HEAD"), cancellable = true, remap = false)
    private void wieldyourpower$blockFavoredOnRemoved(CallbackInfo callback) {
        if (!AuthorsFavorEvents.hasFavored()) {
            return;
        }
        Entity self = (Entity) (Object) this;
        if (AuthorsFavorEvents.shouldResistRemoval(self, self.getRemovalReason())) {
            callback.cancel();
        }
    }
}
