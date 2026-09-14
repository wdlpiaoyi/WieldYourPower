package net.wieldyourpower.common;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingUseTotemEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wieldyourpower.WieldYourPower;
import net.wieldyourpower.util.EntityMatcher;
import net.wieldyourpower.util.KillUtil;

/**
 * Supports the enhanced kill from the Forge event layer only (never edits other mods' internals):
 * <ul>
 *   <li>blocks totems of undying for a target we are force-killing;</li>
 *   <li>re-opens a cancelled {@link LivingDeathEvent} for non-honor targets, so protection that
 *       cancels death (death-protection items, scripts, etc.) cannot stop our kill.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = WieldYourPower.MODID)
public final class ForceKillEvents {

    private ForceKillEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseTotem(LivingUseTotemEvent event) {
        if (KillUtil.isForceKilling(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!KillUtil.isForceKilling(event.getEntity())) {
            return;
        }
        if (event.isCanceled() && !EntityMatcher.matchesHonor(event.getEntity())) {
            // Non-honor target: override the protection by making the death stand.
            event.setCanceled(false);
        }
    }
}
