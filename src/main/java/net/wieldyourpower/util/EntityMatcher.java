package net.wieldyourpower.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.wieldyourpower.WYPConfig;

import java.util.List;

/**
 * Matches configurable entity entries. Every entry declares its own match type, comma-separated (a colon
 * inside a value such as an entity id is kept as-is):
 * <pre>
 *   tag,&lt;scoreboard tag&gt;
 *   type,&lt;entity type id&gt;      e.g. type,minecraft:ender_dragon
 *   uuid,&lt;entity uuid&gt;
 * </pre>
 * A bare entry (no key) is tried as both a scoreboard tag and an entity type id. The legacy
 * {@code key:value} form is still accepted.
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
            String entry = raw == null ? "" : raw.trim();
            if (entry.isEmpty()) {
                continue;
            }
            String[] parts = FilterSyntax.entityKeyValue(entry);
            if (parts != null) {
                switch (parts[0]) {
                    case "tag":
                        if (entity.getTags().contains(parts[1])) {
                            return true;
                        }
                        break;
                    case "uuid":
                        if (entity.getStringUUID().equalsIgnoreCase(parts[1])) {
                            return true;
                        }
                        break;
                    case "type":
                        if (matchesType(entity, parts[1])) {
                            return true;
                        }
                        break;
                    default:
                        break;
                }
                continue;
            }
            // Bare entry: try as a scoreboard tag and as an entity type id.
            if (entity.getTags().contains(entry)) {
                return true;
            }
            if (matchesType(entity, entry)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesType(Entity entity, String value) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && id.toString().equals(value);
    }
}
