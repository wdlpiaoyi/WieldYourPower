package net.wieldyourpower.common;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wieldyourpower.WieldYourPower;
import net.wieldyourpower.util.NoUpdateMode;

/** Forgets a player's "no update" state when they leave, so a stale flag never survives a reconnect. */
@Mod.EventBusSubscriber(modid = WieldYourPower.MODID)
public final class NoUpdateEvents {

    private NoUpdateEvents() {
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        NoUpdateMode.clear(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            NoUpdateMode.resetWindow();
        }
    }
}
