package net.wieldyourpower.common;

import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wieldyourpower.WieldYourPower;
import net.wieldyourpower.util.RemovalGuard;

/**
 * Blocks a force-removed entity from being added back to a level. {@code ServerLevel.addFreshEntity}
 * fires this cancelable event, which is exactly the path a "guard tick / revival" mod uses, so
 * cancelling it makes the removal stick without any mod-specific code.
 */
@Mod.EventBusSubscriber(modid = WieldYourPower.MODID)
public final class RemovalGuardEvents {

    private RemovalGuardEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (RemovalGuard.isGuarded(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
