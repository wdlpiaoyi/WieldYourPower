package net.wieldyourpower.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.wieldyourpower.WYPConfig;

import java.util.List;
import java.util.Locale;

/**
 * Matches configurable entity entries. Every entry declares its own match type:
 * <pre>
 *   tag:&lt;scoreboard tag&gt;
 *   type:&lt;entity type id&gt;
 *   uuid:&lt;entity uuid&gt;
 * </pre>
 * A bare entry (no prefix) is tried as both a scoreboard tag and an entity type id.
 */
public final class EntityMatcher {

    private EntityMatcher() {
    }

    public static boolean matchesHonor(Entity entity) {
        return matches(WYPConfig.COMMON.killHonor.get(), entity);
    }

    public static boolean matches(List<? extends String> entries, Entity entity) {
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        for (String raw : entries) {
            if (raw == null) {
                continue;
            }
            String entry = raw.trim();
            if (entry.isEmpty()) {
                continue;
            }
            int separator = entry.indexOf(':');
            String key = separator < 0 ? "" : entry.substring(0, separator).trim().toLowerCase(Locale.ROOT);
            String value = separator < 0 ? entry : entry.substring(separator + 1).trim();
            if (value.isEmpty()) {
                continue;
            }

            switch (key) {
                case "tag":
                    if (entity instanceof Player player && player.getTags().contains(value)) {
                        return true;
                    }
                    break;
                case "uuid":
                    if (entity.getStringUUID().equalsIgnoreCase(value)) {
                        return true;
                    }
                    break;
                case "type":
                    if (matchesType(entity, value)) {
                        return true;
                    }
                    break;
                default:
                    if (entity instanceof Player player && player.getTags().contains(entry)) {
                        return true;
                    }
                    if (matchesType(entity, entry)) {
                        return true;
                    }
                    break;
            }
        }
        return false;
    }

    private static boolean matchesType(Entity entity, String value) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && id.toString().equals(value);
    }
}
