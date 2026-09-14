package net.wieldyourpower.util;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.entity.EntityLookup;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.wieldyourpower.WieldYourPower;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Entityeraser-style forced world removal. Even if a protection mod replaces {@code levelCallback}
 * with a no-op or swallows removal, this rips the entity out of the server's tick list, chunk
 * tracking, section storage and id/uuid lookups directly.
 *
 * <p>All access is reflective (Forge {@link ObfuscationReflectionHelper}, SRG names) and fully
 * defensive: any missing field simply skips that step. Only server-side removal is performed here
 * (the enhanced {@code /kill} always runs on the logical server); clients are notified through
 * {@code ClientboundRemoveEntitiesPacket}.</p>
 */
public final class ForcedRemoval {

    private ForcedRemoval() {
    }

    public static void forceRemove(Entity entity) {
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            // Never rip a player out of the level/entity manager: it breaks the death screen and respawn.
            return;
        }
        if (entity.level() instanceof ServerLevel serverLevel) {
            removeFromServer(serverLevel, entity);
        }
    }

    @SuppressWarnings("unchecked")
    private static void removeFromServer(ServerLevel level, Entity entity) {
        try {
            int id = entity.getId();
            UUID uuid = entity.getUUID();
            long sectionKey = SectionPos.asLong(entity.blockPosition());

            EntityTickList tickList = get(ServerLevel.class, level, "f_143243_");
            if (tickList != null) {
                removeFromTickList(tickList, id);
            }

            ServerChunkCache chunkSource = level.getChunkSource();
            ChunkMap chunkMap = get(ServerChunkCache.class, chunkSource, "f_8325_");
            if (chunkMap != null) {
                Object entityMapObj = get(ChunkMap.class, chunkMap, "f_140150_");
                if (entityMapObj instanceof it.unimi.dsi.fastutil.ints.Int2ObjectMap<?> entityMap) {
                    Object tracked = ((it.unimi.dsi.fastutil.ints.Int2ObjectMap<Object>) entityMap).remove(id);
                    if (tracked != null) {
                        notifyTracking(tracked, id);
                    }
                }
            }

            PersistentEntitySectionManager<Entity> manager = get(ServerLevel.class, level, "f_143244_");
            if (manager != null) {
                EntityLookup<Entity> visible = get(PersistentEntitySectionManager.class, manager, "f_157494_");
                if (visible != null) {
                    Object byIdObj = get(EntityLookup.class, visible, "f_156807_");
                    if (byIdObj instanceof it.unimi.dsi.fastutil.ints.Int2ObjectMap<?> byId) {
                        ((it.unimi.dsi.fastutil.ints.Int2ObjectMap<Object>) byId).remove(id);
                    }
                    Object byUuidObj = get(EntityLookup.class, visible, "f_156808_");
                    if (byUuidObj instanceof Map<?, ?> byUuid) {
                        ((Map<Object, Object>) byUuid).remove(uuid);
                    }
                }

                Object knownUuids = get(PersistentEntitySectionManager.class, manager, "f_157491_");
                if (knownUuids instanceof Set<?> set) {
                    ((Set<Object>) set).remove(uuid);
                }

                EntitySectionStorage<Entity> sectionStorage =
                        get(PersistentEntitySectionManager.class, manager, "f_157495_");
                if (sectionStorage != null) {
                    EntitySection<Entity> section = sectionStorage.getSection(sectionKey);
                    if (section != null) {
                        Object storage = get(EntitySection.class, section, "f_156827_");
                        if (storage instanceof Collection<?> collection) {
                            ((Collection<Object>) collection).remove(entity);
                        }
                    }
                }
            }

            if (entity instanceof Mob mob) {
                Object navigating = get(ServerLevel.class, level, "f_143246_");
                if (navigating instanceof Set<?> set) {
                    ((Set<Object>) set).remove(mob);
                }
            }
            if (entity.isMultipartEntity()) {
                Object dragonParts = get(ServerLevel.class, level, "f_143247_");
                if (dragonParts instanceof it.unimi.dsi.fastutil.ints.Int2ObjectMap<?> map) {
                    for (PartEntity<?> part : entity.getParts()) {
                        ((it.unimi.dsi.fastutil.ints.Int2ObjectMap<Object>) map).remove(part.getId());
                    }
                }
            }

            level.getScoreboard().entityRemoved(entity);
            entity.setLevelCallback(EntityInLevelCallback.NULL);
        } catch (Throwable throwable) {
            WieldYourPower.LOGGER.debug("[WieldYourPower] server forced removal partial failure", throwable);
        }
    }

    @SuppressWarnings("unchecked")
    private static void removeFromTickList(EntityTickList tickList, int id) {
        for (String name : new String[] { "f_156903_", "f_156904_", "f_156905_" }) {
            Object map = get(EntityTickList.class, tickList, name);
            if (map instanceof it.unimi.dsi.fastutil.ints.Int2ObjectMap<?> intMap) {
                ((it.unimi.dsi.fastutil.ints.Int2ObjectMap<Object>) intMap).remove(id);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void notifyTracking(Object tracked, int id) {
        Object seenBy = get(tracked.getClass(), tracked, "f_140475_");
        if (seenBy instanceof Iterable<?> iterable) {
            for (Object connection : iterable) {
                if (connection instanceof net.minecraft.server.network.ServerPlayerConnection playerConnection) {
                    playerConnection.getPlayer().connection.send(
                            new net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket(id));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T get(Class<?> owner, Object instance, String srgName) {
        try {
            Field field = ObfuscationReflectionHelper.findField(owner, srgName);
            return (T) field.get(instance);
        } catch (Throwable throwable) {
            return null;
        }
    }
}
