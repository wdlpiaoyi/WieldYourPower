package net.wieldyourpower.common;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wieldyourpower.WieldYourPower;
import net.wieldyourpower.util.FrozenEntities;

/**
 * True entity freeze, like Tweakerge's disableEntityTicking.
 *
 * <p>Forge fires {@link LivingEvent.LivingTickEvent} at the very start of {@code LivingEntity.tick()}
 * and returns early when it is cancelled
 * ({@code if (ForgeHooks.onLivingTick(this)) return;}), so cancelling it skips the whole tick:
 * no AI, no gravity, no movement, no spell casts.</p>
 *
 * <p>The zeroed velocity is still written first so the client is told to stop.</p>
 */
@Mod.EventBusSubscriber(modid = WieldYourPower.MODID)
public final class FreezeEvents {

    private FreezeEvents() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide || living instanceof Player) {
            return;
        }
        if (!FrozenEntities.isFrozen(living)) {
            return;
        }

        living.setDeltaMovement(Vec3.ZERO);
        living.hasImpulse = false;
        living.hurtMarked = true;

        // Skip the entire tick.
        event.setCanceled(true);
    }
}
