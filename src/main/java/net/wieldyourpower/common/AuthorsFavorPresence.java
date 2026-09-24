package net.wieldyourpower.common;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wieldyourpower.WieldYourPower;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Presence guard for favored entities.
 *
 * <p>A "deep" removal can bypass {@code remove}/{@code setRemoved} entirely by reflecting straight into
 * the level's entity manager (sections, chunk tracking, tick list). The mixin-level resistance in
 * {@link AuthorsFavorEvents} never sees that, and the per-tick undo never runs because the entity is
 * no longer ticking. This keeps a strong reference to each currently favored entity and re-adds it
 * when it disappears from its level - the same idea a "revive" mod uses, but mod-agnostic.</p>
 *
 * <p>Favored entities are opt-in (tag / filter) and few, so a strong reference is acceptable. It only
 * re-adds while the chunk is already loaded, so it never forces chunk loading, and it never fights
 * {@code /wyp kill}: {@link AuthorsFavorEvents#isFavored} is false while force-killing. A genuinely
 * dying entity is left alone so a custom death animation can finish instead of looping.</p>
 */
@Mod.EventBusSubscriber(modid = WieldYourPower.MODID)
public final class AuthorsFavorPresence {

    private static final Map<UUID, Entity> PRESENT = new ConcurrentHashMap<>();

    private AuthorsFavorPresence() {
    }

    public static void track(LivingEntity entity) {
        if (!entity.level().isClientSide) {
            PRESENT.put(entity.getUUID(), entity);
        }
    }

    public static void forget(UUID uuid) {
        PRESENT.remove(uuid);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PRESENT.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Entity>> iterator = PRESENT.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Entity> entry = iterator.next();
            Entity entity = entry.getValue();
            if (entity == null || entity.level() == null || !AuthorsFavorEvents.isFavored(entity)) {
                iterator.remove();
                continue;
            }
            if (entity instanceof LivingEntity living && living.isDeadOrDying()) {
                // A real death: let it finish instead of resurrecting a corpse / looping an animation.
                iterator.remove();
                continue;
            }
            if (!(entity.level() instanceof ServerLevel serverLevel)) {
                continue;
            }
            if (serverLevel.getEntities().get(entry.getKey()) != null) {
                continue;
            }
            if (!serverLevel.hasChunkAt(entity.blockPosition())) {
                continue;
            }
            try {
                entity.unsetRemoved();
                entity.reviveCaps();
                entity.isAddedToWorld = true;
                if (serverLevel.addFreshEntity(entity)) {
                    restoreBossBars(entity, serverLevel);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * A "deep removal" mod may clear the entity's {@link ServerBossEvent}s before removing it. Re-show
     * them to the players in the same level after the entity is put back.
     */
    private static void restoreBossBars(Entity entity, ServerLevel serverLevel) {
        try {
            for (Class<?> type = entity.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
                for (Field field : type.getDeclaredFields()) {
                    if (field.getType() != ServerBossEvent.class) {
                        continue;
                    }
                    field.setAccessible(true);
                    if (field.get(entity) instanceof ServerBossEvent boss) {
                        boss.setVisible(true);
                        for (ServerPlayer player : serverLevel.players()) {
                            boss.addPlayer(player);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
