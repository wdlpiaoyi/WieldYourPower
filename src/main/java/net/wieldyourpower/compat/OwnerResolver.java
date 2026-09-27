package net.wieldyourpower.compat;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.wieldyourpower.WieldYourPower;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves the player an entity ultimately belongs to: its owner (pets/summons), or the shooter/owner of
 * a projectile or effect cloud, recursively. Vanilla interfaces ({@link OwnableEntity}, {@link Projectile},
 * {@link AreaEffectCloud}) are used where possible; a keyword-driven reflective fallback finds a summon's
 * own owner method ({@code getSummoner}/{@code getOwner}/...) for summons that do not implement
 * {@code OwnableEntity}, without naming any mod.
 *
 * <p>The reflective lookup is cached per class (the expensive part is the class scan, not the call) and
 * the found method's bytecode is safety-scanned via {@link ClassSafety}.</p>
 */
public final class OwnerResolver {

    private static final int MAX_DEPTH = 4;
    private static final String[] OWNER_KEYWORDS = {
            "getsummoner", "getowner", "getmaster", "gettrueowner", "getowningplayer"
    };
    private static final Map<Class<?>, Optional<Method>> REFLECTIVE = new ConcurrentHashMap<>();

    private OwnerResolver() {
    }

    /** The UUID of the player an entity ultimately belongs to, or {@code null}. */
    public static UUID ownerUUID(Entity entity) {
        return ownerUUID(entity, 0);
    }

    /** The online {@link ServerPlayer} an entity ultimately belongs to, or {@code null}. */
    public static ServerPlayer responsible(Entity entity) {
        UUID uuid = ownerUUID(entity);
        if (uuid == null || entity == null) {
            return null;
        }
        MinecraftServer server = entity.getServer();
        return server == null ? null : server.getPlayerList().getPlayer(uuid);
    }

    private static UUID ownerUUID(Entity entity, int depth) {
        if (entity == null || depth > MAX_DEPTH) {
            return null;
        }
        if (entity instanceof ServerPlayer player) {
            return player.getUUID();
        }
        if (entity instanceof OwnableEntity ownable) {
            UUID owner = ownable.getOwnerUUID();
            if (owner != null) {
                return owner;
            }
        }
        if (entity instanceof Projectile projectile) {
            UUID owner = ownerUUID(projectile.getOwner(), depth + 1);
            if (owner != null) {
                return owner;
            }
        }
        if (entity instanceof AreaEffectCloud cloud) {
            UUID owner = ownerUUID(cloud.getOwner(), depth + 1);
            if (owner != null) {
                return owner;
            }
        }
        Object summoner = invokeOwnerGetter(entity);
        if (summoner instanceof Entity ownerEntity) {
            return ownerUUID(ownerEntity, depth + 1);
        }
        if (summoner instanceof UUID uuid) {
            return uuid;
        }
        return null;
    }

    private static Object invokeOwnerGetter(Entity entity) {
        Method method = REFLECTIVE
                .computeIfAbsent(entity.getClass(), OwnerResolver::findOwnerGetter)
                .orElse(null);
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(entity);
        } catch (Throwable throwable) {
            REFLECTIVE.put(entity.getClass(), Optional.empty());
            WieldYourPower.LOGGER.debug("[WieldYourPower] owner lookup failed for {}",
                    entity.getClass().getName(), throwable);
            return null;
        }
    }

    private static Optional<Method> findOwnerGetter(Class<?> clazz) {
        try {
            for (Class<?> type = clazz; type != null && type != Object.class; type = type.getSuperclass()) {
                for (Method method : type.getDeclaredMethods()) {
                    if (method.getParameterCount() != 0) {
                        continue;
                    }
                    Class<?> returnType = method.getReturnType();
                    if (!Entity.class.isAssignableFrom(returnType) && returnType != UUID.class) {
                        continue;
                    }
                    String name = method.getName().toLowerCase(Locale.ROOT);
                    boolean named = false;
                    for (String keyword : OWNER_KEYWORDS) {
                        if (name.contains(keyword)) {
                            named = true;
                            break;
                        }
                    }
                    if (!named || !ClassSafety.isMethodSafe(method)) {
                        continue;
                    }
                    method.setAccessible(true);
                    return Optional.of(method);
                }
            }
        } catch (Throwable throwable) {
            WieldYourPower.LOGGER.debug("[WieldYourPower] owner-method scan failed for {}",
                    clazz.getName(), throwable);
        }
        return Optional.empty();
    }
}
