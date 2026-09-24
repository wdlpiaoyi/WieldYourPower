package net.wieldyourpower.util;

import net.minecraft.world.entity.Entity;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Weak instance-keyed registry of entities that were force-removed. Used to refuse a later
 * {@code Level.addFreshEntity} (via {@code EntityJoinLevelEvent}) so a "remove then re-add" revival
 * cannot undo {@code /wyp kill}.
 *
 * <p>Deliberately keyed by the entity instance, never by UUID: {@code KillUtil.forceDeath} changes the
 * entity's UUID afterwards, while {@code Entity.hashCode()} is the entity's runtime id and
 * {@code Entity.equals} is id-based, so instance lookups stay valid across that change.</p>
 */
public final class RemovalGuard {

    private static final Set<Entity> REMOVED = Collections.newSetFromMap(new WeakHashMap<>());

    private RemovalGuard() {
    }

    public static void mark(Entity entity) {
        if (entity != null) {
            REMOVED.add(entity);
        }
    }

    public static boolean isGuarded(Entity entity) {
        return entity != null && !REMOVED.isEmpty() && REMOVED.contains(entity);
    }
}
