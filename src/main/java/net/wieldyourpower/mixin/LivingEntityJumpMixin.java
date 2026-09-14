package net.wieldyourpower.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.wieldyourpower.client.ClientLimits;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Caps the local player's jump velocity to the absolute {@code jumpLimit} (blocks/tick). Vanilla is ~0.42.
 * Client only (see the {@code client} list in the mixin config), so referencing client classes is safe.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityJumpMixin {

    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void wieldyourpower$limitJump(CallbackInfo callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (Minecraft.getInstance().player != self) {
            return;
        }
        double limit = ClientLimits.jumpLimit;
        if (limit < 0.0D) {
            return;
        }
        Vec3 delta = self.getDeltaMovement();
        if (delta.y > limit) {
            self.setDeltaMovement(delta.x, limit, delta.z);
        }
    }
}
