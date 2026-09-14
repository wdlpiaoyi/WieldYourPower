package net.wieldyourpower.mixin;

import net.minecraft.world.entity.Entity;
import net.wieldyourpower.common.AuthorsFavorEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks the "-999,-999,-999" teleport MoreAvaritia's superAttack uses to dump a target into the void.
 */
@Mixin(Entity.class)
public abstract class EntitySetPosMixin {

    @Inject(method = "setPos(DDD)V", at = @At("HEAD"), cancellable = true)
    private void wieldyourpower$blockAbsurdPos(double x, double y, double z, CallbackInfo callback) {
        if (!AuthorsFavorEvents.hasFavored()) {
            return;
        }
        Entity self = (Entity) (Object) this;
        if (AuthorsFavorEvents.isFavored(self)
                && (y < -500.0D || Math.abs(x) > 1.0E6D || Math.abs(z) > 1.0E6D)) {
            callback.cancel();
        }
    }
}
