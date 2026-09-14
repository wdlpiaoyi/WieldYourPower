package net.wieldyourpower.common;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wieldyourpower.WieldYourPower;
import net.wieldyourpower.util.KillUtil;

/**
 * Drives {@link KillUtil#enforcePendingPlayerKills()} on the server.
 */
@Mod.EventBusSubscriber(modid = WieldYourPower.MODID)
public final class KillEnforceEvents {

    private KillEnforceEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            KillUtil.enforcePendingPlayerKills();
        }
    }
}
