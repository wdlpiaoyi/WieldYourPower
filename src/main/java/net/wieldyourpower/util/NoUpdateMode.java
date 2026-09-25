package net.wieldyourpower.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "No update" placement/breaking mode. While active for a player, the block changes caused by their
 * own placement/breaking/forced removal are written without the neighbour-notify and neighbour
 * shape-update bits, so observers, pistons and redstone do not react. The changed block itself is
 * still synced to clients.
 *
 * <p>The client suppresses continuously while its local mode is on: it must not locally derive the
 * neighbour reactions the server withheld. The server only suppresses inside the acting player's own
 * {@code destroyBlock}/{@code place}/force-break call, tracked by {@link #beginWindow()}, so other
 * players are never affected. The window is thread-local so the client and an integrated server never
 * bleed into each other.</p>
 */
public final class NoUpdateMode {

    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<int[]> WINDOW = ThreadLocal.withInitial(() -> new int[1]);
    private static volatile boolean clientActive;

    private NoUpdateMode() {
    }

    /** Server side: record whether a player has the mode on. */
    public static void setActive(UUID id, boolean active) {
        if (active) {
            ACTIVE.add(id);
        } else {
            ACTIVE.remove(id);
        }
    }

    /** Server side: forget a player (logout). */
    public static void clear(UUID id) {
        ACTIVE.remove(id);
    }

    /** Client side: the local computed state, applied every tick. */
    public static void setClientActive(boolean active) {
        clientActive = active;
    }

    public static boolean clientActive() {
        return clientActive;
    }

    /** Whether the given player has the mode on, on whichever side owns them. Creative only. */
    public static boolean isActiveFor(Player player) {
        if (player == null || !player.isCreative()) {
            return false;
        }
        return player.level().isClientSide ? clientActive : ACTIVE.contains(player.getUUID());
    }

    public static void beginWindow() {
        WINDOW.get()[0]++;
    }

    public static void endWindow() {
        int[] window = WINDOW.get();
        if (window[0] > 0) {
            window[0]--;
        }
    }

    /** Safety net: called at the start of each server tick so a window can never leak past its action. */
    public static void resetWindow() {
        WINDOW.get()[0] = 0;
    }

    /** Whether block changes should drop their neighbour updates right now. */
    public static boolean isSuppressing(Level level) {
        return level.isClientSide ? clientActive : WINDOW.get()[0] > 0;
    }
}
