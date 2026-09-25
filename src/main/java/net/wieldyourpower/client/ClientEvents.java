package net.wieldyourpower.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wieldyourpower.WYPConfig;
import net.wieldyourpower.WieldYourPower;
import net.wieldyourpower.network.PacketNoUpdate;
import net.wieldyourpower.network.WYPNetwork;
import net.wieldyourpower.util.NoUpdateMode;

/**
 * Speed limits are now <b>absolute momentum caps</b> (blocks per tick, the length of the movement vector):
 * {@code -1} = off, {@code 0} = stop. Vanilla references: walk ~0.216, sprint ~0.281, creative flight
 * ~0.545 horizontal / ~0.5 vertical, jump ~0.42.
 */
@Mod.EventBusSubscriber(modid = WieldYourPower.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        while (ClientSetup.OPEN_PANEL.consumeClick()) {
            ClientPacketHandler.openScreen(net.wieldyourpower.network.PacketOpenPanel.LIMITS);
        }
        updateNoUpdateMode();
    }

    private static boolean noUpdateToggled;
    private static boolean noUpdateSent;

    /**
     * Computes the local "no update" state from the keybind and its configured mode, and tells the
     * server only when it changes. The client also suppresses its own neighbour derivations locally
     * while active, so its prediction matches the server's withheld updates.
     */
    private static void updateNoUpdateMode() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean active = false;
        if (minecraft.player != null && minecraft.player.isCreative() && !ClientSetup.NO_UPDATE.isUnbound()) {
            switch (WYPConfig.COMMON.noUpdatePlacementMode.get()) {
                case HOLD -> active = ClientSetup.NO_UPDATE.isDown();
                case INVERTED_HOLD -> active = !ClientSetup.NO_UPDATE.isDown();
                case TOGGLE -> {
                    while (ClientSetup.NO_UPDATE.consumeClick()) {
                        noUpdateToggled = !noUpdateToggled;
                    }
                    active = noUpdateToggled;
                }
            }
        } else {
            noUpdateToggled = false;
        }
        NoUpdateMode.setClientActive(active);
        if (active != noUpdateSent) {
            noUpdateSent = active;
            WYPNetwork.CHANNEL.sendToServer(new PacketNoUpdate(active));
        }
    }

    @SubscribeEvent
    public static void onLoggingIn(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
        // Start clean; the server (if it has this mod) will sync the player's own limits right after.
        ClientLimits.setAll(-1.0D, -1.0D, -1.0D, 0, 0, -1.0D, -1.0D, java.util.List.of(),
                java.util.List.of("type,touhou_little_maid:maid"));
        noUpdateToggled = false;
        noUpdateSent = false;
        NoUpdateMode.setClientActive(false);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || event.player != player) {
            return;
        }
        boolean flying = player.getAbilities().flying;

        double horizontal = flying ? ClientLimits.flySpeedHorizontal : ClientLimits.walkSpeed;
        Vec3 delta = player.getDeltaMovement();
        if (horizontal >= 0.0D) {
            double length = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            if (length > horizontal && length > 1.0E-4D) {
                double scale = horizontal / length;
                player.setDeltaMovement(delta.x * scale, delta.y, delta.z * scale);
                delta = player.getDeltaMovement();
            }
        }

        if (flying) {
            double vertical = ClientLimits.flySpeedVertical;
            if (vertical >= 0.0D && Math.abs(delta.y) > vertical) {
                player.setDeltaMovement(delta.x, Math.signum(delta.y) * vertical, delta.z);
            }
        }
    }
}
