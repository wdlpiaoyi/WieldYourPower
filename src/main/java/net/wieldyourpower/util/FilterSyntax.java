package net.wieldyourpower.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Shared entry syntax for the config filter lists.
 *
 * <p>New form: fields separated by {@code ,}. A colon inside a value (e.g. {@code minecraft:diamond})
 * is kept as-is. The legacy {@code key:value} form is still understood and can be rewritten with
 * {@link #normalize(String)}.</p>
 */
public final class FilterSyntax {

    /** Keys used by {@link EntityMatcher} (ally protection / kill honor). */
    public static final Set<String> ENTITY_KEYS = Set.of("tag", "type", "uuid");

    private FilterSyntax() {
    }

    /**
     * Splits an entity entry into {@code [key, value]}.
     *
     * <p>Comma is the new separator; if there is no comma the legacy {@code key:value} form is accepted
     * when the key is known. A bare entry (or an unknown key) returns {@code null} so the caller can fall
     * back to treating the whole entry as a tag / type id.</p>
     */
    public static String[] entityKeyValue(String raw) {
        return keyValue(raw, ENTITY_KEYS);
    }

    private static String[] keyValue(String raw, Set<String> knownKeys) {
        if (raw == null) {
            return null;
        }
        String entry = raw.trim();
        if (entry.isEmpty()) {
            return null;
        }
        int comma = entry.indexOf(',');
        if (comma > 0) {
            String key = entry.substring(0, comma).trim().toLowerCase(Locale.ROOT);
            String value = entry.substring(comma + 1).trim();
            if (knownKeys.contains(key) && !value.isEmpty()) {
                return new String[]{key, value};
            }
        }
        int colon = entry.indexOf(':');
        if (colon > 0) {
            String key = entry.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            if (knownKeys.contains(key)) {
                String value = entry.substring(colon + 1).trim();
                if (!value.isEmpty()) {
                    return new String[]{key, value};
                }
            }
        }
        return null;
    }

    /** Rewrites a legacy {@code key:value} entry into the new {@code key,value} form. */
    public static String normalize(String raw) {
        String entry = raw == null ? "" : raw.trim();
        String[] parts = keyValue(entry, ENTITY_KEYS);
        if (parts == null) {
            return entry;
        }
        return parts[0] + "," + parts[1];
    }

    public static List<String> normalizeAll(List<? extends String> entries) {
        List<String> out = new ArrayList<>();
        if (entries == null) {
            return out;
        }
        for (String entry : entries) {
            String normalized = normalize(entry);
            if (!normalized.isEmpty()) {
                out.add(normalized);
            }
        }
        return out;
    }

    public static boolean isKnownEntityKey(String key) {
        return ENTITY_KEYS.contains(key);
    }
}
