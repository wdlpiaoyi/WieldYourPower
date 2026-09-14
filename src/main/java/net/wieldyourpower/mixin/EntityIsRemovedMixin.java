package net.wieldyourpower.mixin;

import net.minecraft.world.entity.Entity;
import net.wieldyourpower.common.AuthorsFavorEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A favored entity whose {@code removalReason} was written <b>directly</b> (MoreAvaritia's superAttack does
 * {@code target.removalReason = DISCARDED} without calling {@code setRemoved}) would be treated as removed
 * and stop ticking, so the per-tick undo would never run. Report it as present while the reason is
 * KILLED/DISCARDED; the next tick clears the reason for good.
 */
@Mixin(Entity.class)
public abstract class EntityIsRemovedMixin {

    @Inject(method = "isRemoved", at = @At("HEAD"), cancellable = true)
    private void wieldyourpower$hideFavoredRemoval(CallbackInfoReturnable<Boolean> callback) {
        if (!AuthorsFavorEvents.hasFavored()) {
            return;
        }
        Entity self = (Entity) (Object) this;
        if (AuthorsFavorEvents.shouldResistRemoval(self, self.getRemovalReason())) {
            callback.setReturnValue(false);
        }
    }
}
