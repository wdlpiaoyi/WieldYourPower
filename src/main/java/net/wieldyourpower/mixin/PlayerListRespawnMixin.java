package net.wieldyourpower.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.wieldyourpower.common.CreativeDefenseEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Blocks a forced respawn of a protected creative player. Some mods call
 * {@code PlayerList.respawn(player, false)} to "kill" a player without any damage: it discards the old
 * player entity (and with it the inventory unless {@code keepInventory}) and drops a fresh one on the
 * respawn point. Returning the same player leaves the connection untouched - no teleport, no wipe.
 *
 * <p>A protected creative player can never legitimately need this (its death is blocked), and after
 * {@code /wyp kill} {@code wasKilledByUs} makes the check pass, so real respawns still work.</p>
 */
@Mixin(PlayerList.class)
public abstract class PlayerListRespawnMixin {

    @Inject(method = "respawn", at = @At("HEAD"), cancellable = true)
    private void wieldyourpower$blockCreativeRespawn(ServerPlayer player, boolean keepEverything,
                                                     CallbackInfoReturnable<ServerPlayer> callback) {
        if (CreativeDefenseEvents.shouldBlockDeath(player)) {
            callback.setReturnValue(player);
        }
    }
}
