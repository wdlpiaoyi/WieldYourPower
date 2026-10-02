package net.wieldyourpower.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.wieldyourpower.network.WYPNetwork;
import net.wieldyourpower.util.ForceBlockBreak;
import net.wieldyourpower.util.NoUpdateMode;

/**
 * Public API for KubeJS and other mods. Use it to build items (e.g. a survival tool) that reuse this
 * mod's block handling.
 *
 * <p>All methods are <b>server-side</b> (they return {@code false} on the client): call them from the
 * item's {@code useOn}/block handler and they perform the block change immediately. Everything returns
 * {@code true} when a block actually changed.</p>
 *
 * <ul>
 *   <li>{@link #breakBlock} — force-removes the block at {@code pos} the way the admin Block Breaker
 *       does: it runs the block's own destroy hook and bypasses other mods' block protection
 *       (keyword-driven, no mod is named). {@code drop} controls whether resources drop first.</li>
 *   <li>{@link #breakBlockNoUpdate} / {@link #placeBlockNoUpdate} — same write, but neighbour notify and
 *       neighbour shape updates are suppressed, so observers, pistons and redstone do not react while the
 *       block itself still syncs to clients.</li>
 * </ul>
 *
 * <p>KubeJS example (a right-click tool that breaks with no updates):</p>
 * <pre>{@code
 * const WYP = Java.loadClass('net.wieldyourpower.api.WieldYourPowerApi')
 * // in the item's useOn handler:
 * WYP.breakBlockNoUpdate(level, pos, player, true)
 * }</pre>
 */
public final class WieldYourPowerApi {

    private WieldYourPowerApi() {
    }

    /**
     * Grants/revokes the "no update" mode for a <b>survival</b> player, so they can use the no-update
     * keybind and the suppression API without being in creative. Intended for an equipped accessory: call
     * it on the server when the item is equipped ({@code true}) and unequipped ({@code false}), and only
     * for the correct slot. The server syncs the flag to that player's client (which also uses it for its
     * local prediction); calling it client-side sets only the local flag. Creative players are unaffected.
     */
    public static void setSurvivalNoUpdateAccess(Player player, boolean allowed) {
        if (player == null) {
            return;
        }
        if (player.level().isClientSide) {
            NoUpdateMode.setClientPermitted(allowed);
            return;
        }
        boolean changed = NoUpdateMode.permit(player.getUUID(), allowed);
        if (!allowed) {
            NoUpdateMode.setActive(player.getUUID(), false);
        }
        if (changed && player instanceof ServerPlayer serverPlayer) {
            WYPNetwork.sendNoUpdatePermission(serverPlayer, NoUpdateMode.isPermitted(player.getUUID()));
        }
    }

    /** Force-remove the block at {@code pos}; drops its resources. */
    public static boolean breakBlock(Level level, BlockPos pos, Player player) {
        return breakBlock(level, pos, player, true);
    }

    /**
     * Force-remove the block at {@code pos} (admin Block Breaker semantics: runs the block's own destroy
     * hook and bypasses other mods' block protection). Pass {@code drop = false} for a silent removal.
     */
    public static boolean breakBlock(Level level, BlockPos pos, Player player, boolean drop) {
        if (level == null || level.isClientSide || pos == null) {
            return false;
        }
        return ForceBlockBreak.breakBlock(level, pos, player, drop);
    }

    /** Force-remove the block at {@code pos} without triggering neighbour updates; drops its resources. */
    public static boolean breakBlockNoUpdate(Level level, BlockPos pos, Player player) {
        return breakBlockNoUpdate(level, pos, player, true);
    }

    /** Force-remove the block at {@code pos} without triggering neighbour updates. */
    public static boolean breakBlockNoUpdate(Level level, BlockPos pos, Player player, boolean drop) {
        if (level == null || level.isClientSide || pos == null) {
            return false;
        }
        NoUpdateMode.beginWindow();
        try {
            return ForceBlockBreak.breakBlock(level, pos, player, drop);
        } finally {
            NoUpdateMode.endWindow();
        }
    }

    /**
     * Place {@code state} at {@code pos} without triggering neighbour updates (the changed block still
     * syncs to clients). Mirrors {@code BlockItem.placeBlock}'s write. Returns {@code false} when the write
     * did not happen.
     */
    public static boolean placeBlockNoUpdate(Level level, BlockPos pos, BlockState state, Player player) {
        if (level == null || level.isClientSide || pos == null || state == null) {
            return false;
        }
        NoUpdateMode.beginWindow();
        try {
            return level.setBlock(pos, state, 3);
        } finally {
            NoUpdateMode.endWindow();
        }
    }
}
