package net.wieldyourpower.common;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.wieldyourpower.WYPConfig;
import net.wieldyourpower.WieldYourPower;
import net.wieldyourpower.capability.IPlayerLimits;
import net.wieldyourpower.capability.ModCapabilities;
import net.wieldyourpower.compat.CuriosNoUpdate;
import net.wieldyourpower.network.WYPNetwork;
import net.wieldyourpower.util.NoUpdateMode;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the "no update" state in sync: curio detection runs only when the equipment changes (equip/unequip
 * /login), plus the logout cleanup and the per-tick suppression-window reset.
 */
@Mod.EventBusSubscriber(modid = WieldYourPower.MODID)
public final class NoUpdateEvents {

    /** Players queued for a one-shot curio re-check on their next tick. */
    private static final Set<UUID> PENDING_CURIO_REFRESH = ConcurrentHashMap.newKeySet();

    private NoUpdateEvents() {
    }

    /** Ask for a curio re-check on the player's next tick (equip/unequip/login). */
    public static void requestCurioRefresh(UUID id) {
        if (WYPConfig.COMMON.noUpdateCurioEnabled.get()) {
            PENDING_CURIO_REFRESH.add(id);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        PENDING_CURIO_REFRESH.remove(id);
        NoUpdateMode.clear(id);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        UUID id = event.getEntity().getUUID();
        requestCurioRefresh(id);
        // The operator grant is persisted with the player's data, so restore it before the client needs it.
        if (event.getEntity() instanceof ServerPlayer serverPlayer
                && ModCapabilities.resolve(serverPlayer).isNoUpdateGranted()
                && NoUpdateMode.setGrantAccess(id, true)) {
            WYPNetwork.sendNoUpdatePermission(serverPlayer, NoUpdateMode.isPermitted(id));
        }
    }

    /**
     * Applies the persistent operator grant ({@code /wyp access grant|revoke noupdate}) for one player: stores it
     * in the player's capability and updates the effective permission. Returns whether the stored grant
     * changed (the effective permission can stay the same when an API grant or a curio also allows it).
     */
    public static boolean applyGrant(ServerPlayer player, boolean allowed) {
        IPlayerLimits limits = ModCapabilities.resolve(player);
        if (limits == null || limits.isNoUpdateGranted() == allowed) {
            return false;
        }
        limits.setNoUpdateGranted(allowed);
        if (!allowed) {
            NoUpdateMode.setActive(player.getUUID(), false);
        }
        if (NoUpdateMode.setGrantAccess(player.getUUID(), allowed)) {
            WYPNetwork.sendNoUpdatePermission(player, NoUpdateMode.isPermitted(player.getUUID()));
        }
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            NoUpdateMode.resetWindow();
        }
    }

    /** Handles only queued players (a set lookup otherwise), so there is no per-tick Curios scan. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        UUID id = event.player.getUUID();
        if (!PENDING_CURIO_REFRESH.remove(id) || !ModList.get().isLoaded("curios")) {
            return;
        }
        // CuriosNoUpdate references Curios classes, so it must only be touched past the isLoaded guard.
        boolean wears = CuriosNoUpdate.wearsQualifyingCurio(event.player);
        if (event.player instanceof ServerPlayer serverPlayer
                && NoUpdateMode.setCurioAccess(id, wears)) {
            WYPNetwork.sendNoUpdatePermission(serverPlayer, NoUpdateMode.isPermitted(id));
        }
    }
}
