package net.wieldyourpower.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import org.jetbrains.annotations.Nullable;
import net.wieldyourpower.WYPConfig;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Escalating kill. Methods are tried from weakest to strongest, but the final forced removal is
 * always reached for non-honor targets.
 *
 * <p><b>Honor list (configurable, see {@link EntityMatcher}):</b> matching targets only get the normal
 * death attempt so other mods can take over (e.g. Goety's End Ritual) or so death animations/drops
 * (Ender Dragon egg, Wither) can run. Everything else is force-removed, even if a mod overrides
 * {@code tickDeath} or cancels the death.</p>
 */
public final class KillUtil {

    private static final Set<Entity> FORCE_KILLING = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<Entity> DEATH_BLOCKED = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<Entity, Long> PENDING_PLAYER_KILLS = new IdentityHashMap<>();
    private static final Map<UUID, Long> KILLED_PLAYERS = new ConcurrentHashMap<>();
    private static final long PLAYER_KILL_MEMORY_TICKS = 300L;
    private static final long PLAYER_KILL_REASSERT_TICKS = 20L;

    private KillUtil() {
    }

    public static boolean isForceKilling(@Nullable Entity entity) {
        return entity != null && FORCE_KILLING.contains(entity);
    }

    /**
     * Called from {@link net.wieldyourpower.common.ForceKillEvents} when another mod cancels a death
     * we caused. Non-honor targets are force-killed anyway.
     */
    public static void markDeathBlocked(LivingEntity entity) {
        DEATH_BLOCKED.add(entity);
    }

    private static boolean wasDeathBlocked(Entity entity) {
        return DEATH_BLOCKED.contains(entity);
    }

    public static boolean wasKilledByUs(Player player) {
        Long expiry = KILLED_PLAYERS.get(player.getUUID());
        if (expiry == null) {
            return false;
        }
        if (player.level().getGameTime() > expiry) {
            KILLED_PLAYERS.remove(player.getUUID());
            return false;
        }
        return true;
    }

    public static void clearKilled(UUID uuid) {
        KILLED_PLAYERS.remove(uuid);
    }

    /**
     * Keeps a player killed by this mod dead for a short window. Some resurrection mods revive the player
     * to health &gt; 0 without a real respawn; the vanilla respawn command is then ignored
     * ({@code getHealth() > 0} guard) while the client is still on the death screen ("respawn does nothing").
     * Re-asserting the death makes that command pass. Keyed by entity instance, so a real respawn (which
     * creates a new entity) is never fought.
     */
    public static void enforcePendingPlayerKills() {
        if (PENDING_PLAYER_KILLS.isEmpty()) {
            return;
        }
        java.util.Iterator<Map.Entry<Entity, Long>> iterator = PENDING_PLAYER_KILLS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Entity, Long> entry = iterator.next();
            Entity entity = entry.getKey();
            if (entity.isRemoved() || entity.level() == null) {
                iterator.remove();
                continue;
            }
            if (entity.level().getGameTime() > entry.getValue()) {
                iterator.remove();
                continue;
            }
            if (entity instanceof LivingEntity living && (living.getHealth() > 0.0F || !living.dead)) {
                living.getEntityData().set(LivingEntity.DATA_HEALTH_ID, 0.0F);
                living.setHealth(0.0F);
                living.dead = true;
            }
        }
    }

    public static boolean forceKill(@Nullable Entity entity) {
        if (entity == null || entity.isRemoved()) {
            return false;
        }
        FORCE_KILLING.add(entity);
        DEATH_BLOCKED.remove(entity);
        // Clear the creative-defense invulnerability-frame deception so our own hurt() attempts land.
        if (entity instanceof LivingEntity living) {
            living.invulnerableTime = 0;
            living.lastHurt = 0.0F;
        }
        try {
            // Vanilla kill is simply the first attack method in the escalation.
            entity.kill();

            DamageSource source = entity.damageSources().genericKill();

            if (entity instanceof Player player) {
                return killPlayer(player, source);
            }
            if (entity instanceof LivingEntity living) {
                return killLiving(living, source);
            }

            entity.setRemoved(Entity.RemovalReason.KILLED);
            if (WYPConfig.COMMON.killForceRemoval.get()) {
                ForcedRemoval.forceRemove(entity);
            }
            return entity.isRemoved();
        } finally {
            FORCE_KILLING.remove(entity);
            DEATH_BLOCKED.remove(entity);
        }
    }

    /**
     * "Honor" variant: only the normal death attempt, never a forced removal. Used by /wyp killhonor.
     */
    public static boolean honorKill(@Nullable Entity entity) {
        if (entity == null || entity.isRemoved()) {
            return false;
        }
        FORCE_KILLING.add(entity);
        DEATH_BLOCKED.remove(entity);
        if (entity instanceof LivingEntity living) {
            living.invulnerableTime = 0;
            living.lastHurt = 0.0F;
        }
        try {
            entity.kill();
            DamageSource source = entity.damageSources().genericKill();
            if (entity instanceof Player player) {
                player.hurt(source, Float.MAX_VALUE);
                if (!player.isAlive() && !wasDeathBlocked(player)) {
                    markPlayerKilled(player);
                    return true;
                }
                player.setInvulnerable(false);
                player.hurt(source, Float.MAX_VALUE);
                if (!player.isAlive() && !wasDeathBlocked(player)) {
                    markPlayerKilled(player);
                    return true;
                }
                player.die(source);
                markPlayerKilled(player);
                return true;
            }
            if (entity instanceof LivingEntity living) {
                living.hurt(source, Float.MAX_VALUE);
                if (!living.isRemoved() && living.isAlive()) {
                    living.setInvulnerable(false);
                    living.hurt(source, Float.MAX_VALUE);
                }
                if (!living.isRemoved() && living.isAlive()) {
                    living.die(source);
                }
                return true;
            }
            entity.setRemoved(Entity.RemovalReason.KILLED);
            return entity.isRemoved();
        } finally {
            FORCE_KILLING.remove(entity);
            DEATH_BLOCKED.remove(entity);
        }
    }

    private static boolean killPlayer(Player player, DamageSource source) {
        boolean honor = EntityMatcher.matchesHonor(player);

        player.hurt(source, Float.MAX_VALUE);
        if (!player.isAlive() && !wasDeathBlocked(player)) {
            markPlayerKilled(player);
            return true;
        }
        player.setInvulnerable(false);
        player.hurt(source, Float.MAX_VALUE);
        if (!player.isAlive() && !wasDeathBlocked(player)) {
            markPlayerKilled(player);
            return true;
        }
        player.die(source);
        if (!player.isAlive() && !wasDeathBlocked(player)) {
            markPlayerKilled(player);
            return true;
        }

        if (honor) {
            markPlayerKilled(player);
            return true;
        }

        // Force lethal health. Players are never world-removed, so their save is never touched.
        player.getEntityData().set(LivingEntity.DATA_HEALTH_ID, 0.0F);
        player.setHealth(0.0F);
        player.dead = true;
        markPlayerKilled(player);

        // A mod may have swallowed the setHealth and restored health (e.g. MoreAvaritia's Infinity set).
        // Force the vanilla respawn in that case, otherwise /wyp kill would not land at all.
        if (player instanceof ServerPlayer serverPlayer
                && serverPlayer.getServer() != null
                && (player.getHealth() > 0.0F || !player.dead)) {
            ServerPlayer respawned = serverPlayer.getServer().getPlayerList().respawn(serverPlayer, false);
            serverPlayer.connection.player = respawned;
        }
        return true;
    }

    private static boolean killLiving(LivingEntity living, DamageSource source) {
        boolean honor = EntityMatcher.matchesHonor(living);

        // Non-honor: try the normal kill first.
        living.hurt(source, Float.MAX_VALUE);
        if (!living.isRemoved() && living.isAlive()) {
            living.setInvulnerable(false);
            living.hurt(source, Float.MAX_VALUE);
        }
        if (!living.isRemoved() && living.isAlive()) {
            living.die(source);
        }

        if (living.isRemoved() || honor) {
            return true;
        }

        // Ask the target's own mod to despawn it (keyword-driven, no save edits) before forcing.
        if (WYPConfig.COMMON.bossDespawnCompat.get()
                && net.wieldyourpower.compat.BossDespawnCompat.tryDespawn(living)
                && living.isRemoved()) {
            return true;
        }

        // Non-honor and still present: force it out.
        forceDeath(living);
        return true;
    }

    private static void forceDeath(LivingEntity living) {
        // Detach from vehicles / riders and stop it acting (learned from TianshaExecution).
        try {
            living.stopRiding();
            Entity vehicle = living.getVehicle();
            if (vehicle != null) {
                vehicle.ejectPassengers();
            }
            living.ejectPassengers();
            if (living instanceof Mob mob) {
                mob.setTarget(null);
                mob.setAggressive(false);
            }
        } catch (Throwable ignored) {
        }

        // Ender Dragon: make the dragon fight record the kill so the egg/portal still happen.
        if (living instanceof EnderDragon dragon
                && living.level() instanceof ServerLevel serverLevel
                && serverLevel.dimension() == Level.END) {
            try {
                EndDragonFight fight = serverLevel.getDragonFight();
                if (fight != null) {
                    fight.setDragonKilled(dragon);
                }
            } catch (Throwable ignored) {
            }
        }

        living.getEntityData().set(LivingEntity.DATA_HEALTH_ID, 0.0F);
        living.setHealth(0.0F);
        living.dead = true;
        // Boss bars are owned by the entity and only removed by its own die()/remove(); since we bypass
        // those, clear any ServerBossEvent field ourselves or the boss bar lingers after the kill.
        if (WYPConfig.COMMON.killClearBossBars.get()) {
            clearBossBars(living);
        }
        // Drop it into the void so it dies even if a mod resurrects/re-adds it.
        living.setPos(living.getX(), -200.0D, living.getZ());
        living.setRemoved(Entity.RemovalReason.KILLED);
        if (WYPConfig.COMMON.killForceRemoval.get()) {
            ForcedRemoval.forceRemove(living);
        }
        // Change the UUID after the manager cleanup so revival-by-UUID cannot find it.
        try {
            living.setUUID(UUID.randomUUID());
        } catch (Throwable ignored) {
        }
    }

    /**
     * Removes any {@link net.minecraft.server.level.ServerBossEvent} owned by the entity (found generically
     * by field type) so a forced kill does not leave a lingering boss bar. This only calls the vanilla
     * {@code removeAllPlayers()} / {@code setVisible(false)} on that one bar - it is not a "wipe" hook.
     */
    private static void clearBossBars(LivingEntity entity) {
        try {
            for (Class<?> type = entity.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
                for (java.lang.reflect.Field field : type.getDeclaredFields()) {
                    if (field.getType() != net.minecraft.server.level.ServerBossEvent.class) {
                        continue;
                    }
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    if (value instanceof net.minecraft.server.level.ServerBossEvent boss) {
                        boss.removeAllPlayers();
                        boss.setVisible(false);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void markPlayerKilled(Player player) {
        KILLED_PLAYERS.put(player.getUUID(), player.level().getGameTime() + PLAYER_KILL_MEMORY_TICKS);
        PENDING_PLAYER_KILLS.put(player, player.level().getGameTime() + PLAYER_KILL_REASSERT_TICKS);
    }
}
