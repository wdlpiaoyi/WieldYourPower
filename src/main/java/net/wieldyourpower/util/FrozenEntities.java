package net.wieldyourpower.util;

import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks entities that are temporarily frozen by the Entity Viewer.
 */
public final class FrozenEntities {

    private static final Map<UUID, Long> FROZEN = new ConcurrentHashMap<>();

    private FrozenEntities() {
    }

    public static void freeze(LivingEntity entity, int ticks) {
        FROZEN.put(entity.getUUID(), entity.level().getGameTime() + ticks);
    }

    public static boolean isFrozen(LivingEntity entity) {
        if (FROZEN.isEmpty()) {
            return false;
        }
        Long expiry = FROZEN.get(entity.getUUID());
        if (expiry == null) {
            return false;
        }
        if (entity.level().getGameTime() > expiry) {
            FROZEN.remove(entity.getUUID());
            return false;
        }
        return true;
    }
}
