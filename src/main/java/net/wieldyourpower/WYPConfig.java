package net.wieldyourpower;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public final class WYPConfig {

    public static final ForgeConfigSpec SPEC;
    public static final Common COMMON;

    static {
        Pair<Common, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = pair.getLeft();
        SPEC = pair.getRight();
    }

    private WYPConfig() {
    }

    public enum HitboxMode {
        REMOVE_UNLESS_SNEAK_GROUND,
        HIDE_ONLY_SNEAK_GROUND,
        NEVER_HIDE,
        ALWAYS_HIDE
    }

    public static final class Common {

        public final ForgeConfigSpec.BooleanValue creativeDefense;
        public final ForgeConfigSpec.DoubleValue creativeMinHealth;
        public final ForgeConfigSpec.DoubleValue creativeMinMaxHealth;
        public final ForgeConfigSpec.EnumValue<HitboxMode> creativeHitboxMode;
        public final ForgeConfigSpec.BooleanValue creativeBreaksProtectedBlocks;
        public final ForgeConfigSpec.BooleanValue blockBreakerEnabled;
        public final ForgeConfigSpec.BooleanValue blockProtectionBypass;
        public final ForgeConfigSpec.BooleanValue killPiercesProtection;
        public final ForgeConfigSpec.BooleanValue killForceRemoval;
        public final ForgeConfigSpec.BooleanValue killClearBossBars;
        public final ForgeConfigSpec.BooleanValue bossDespawnCompat;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> killHonor;
        public final ForgeConfigSpec.BooleanValue authorsFavorEnabled;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> authorsFavorFilter;
        public final ForgeConfigSpec.DoubleValue authorsFavorDamageCoefficient;
        public final ForgeConfigSpec.DoubleValue authorsFavorMaxHealthCoefficient;
        public final ForgeConfigSpec.DoubleValue authorsFavorMaxHealthChangeCoefficient;

        public final ForgeConfigSpec.BooleanValue freezeEnabled;
        public final ForgeConfigSpec.IntValue freezeDuration;
        public final ForgeConfigSpec.DoubleValue freezeRadius;

        public final ForgeConfigSpec.BooleanValue entityViewerEnabled;
        public final ForgeConfigSpec.DoubleValue entityViewerReach;
        public final ForgeConfigSpec.DoubleValue entityViewerHitbox;
        public final ForgeConfigSpec.BooleanValue entityViewerSelectDrops;
        public final ForgeConfigSpec.IntValue entityViewerCooldown;

        Common(ForgeConfigSpec.Builder builder) {
            builder.comment("WieldYourPower general settings").push("general");

            this.creativeDefense = builder
                    .comment("Strengthen creative-mode defense so other mods cannot kill creative players.",
                            "Only this mod's /wyp kill bypasses it.")
                    .define("creativeDefense", true);

            this.creativeMinHealth = builder
                    .comment("Every tick keep a protected creative player's health at least this high.")
                    .defineInRange("creativeMinHealth", 20.0D, 0.0D, 1024.0D);

            this.creativeMinMaxHealth = builder
                    .comment("Every tick keep a protected creative player's max health at least this high.")
                    .defineInRange("creativeMinMaxHealth", 20.0D, 1.0D, 1024.0D);

            this.creativeHitboxMode = builder
                    .comment("Experimental: hide a protected creative player from entity lookups (absolute dodge).",
                            "REMOVE_UNLESS_SNEAK_GROUND: hidden, restored while sneaking on the ground (default).",
                            "HIDE_ONLY_SNEAK_GROUND: inverse of the above.",
                            "NEVER_HIDE / ALWAYS_HIDE: unconditional.")
                    .defineEnum("creativeHitboxMode", HitboxMode.REMOVE_UNLESS_SNEAK_GROUND);

            this.creativeBreaksProtectedBlocks = builder
                    .comment("Let creative players break blocks other mods protect or forbid.",
                            "Respects this mod's own mining self-limits; ignores every other protection.")
                    .define("creativeBreaksProtectedBlocks", true);

            this.blockBreakerEnabled = builder
                    .comment("Enable the admin Block Breaker item. Right-click a block to force-remove it,",
                            "bypassing this mod's self-limits and every other protection.")
                    .define("blockBreakerEnabled", true);

            this.blockProtectionBypass = builder
                    .comment("Keyword-driven bypass for third-party block-protection switches.",
                            "Scans every loaded mod for a static *bypass*(boolean) method on a protection/guard",
                            "class (method bytecode safety-scanned) and toggles it around a forced removal.",
                            "No-op when no such switch is present.")
                    .define("blockProtectionBypass", true);

            this.killPiercesProtection = builder
                    .comment("Make the enhanced kill command pierce invulnerability, totems and event cancellation.")
                    .define("killPiercesProtection", true);

            this.killForceRemoval = builder
                    .comment("Last-resort entity-manager teardown for non-honor entities (mobs) that survive setRemoved.",
                            "Players are never world-removed, so player saves are never touched.",
                            "Disable only if a mob's manual removal ever causes issues.")
                    .define("killForceRemoval", true);

            this.killClearBossBars = builder
                    .comment("On a forced kill, clear the target's ServerBossEvent fields so its boss bar",
                            "does not linger (vanilla removeAllPlayers + setVisible(false) only).")
                    .define("killClearBossBars", true);

            this.bossDespawnCompat = builder
                    .comment("LAST RESORT, OFF BY DEFAULT. Before forcing a kill, ask the target's own mod to clean up",
                            "via keyword-driven reflection (despawn/kill/remove/delete/destroy) guarded by a bytecode",
                            "safety scan. It never edits save data or files, but reflective calls into unknown mods are",
                            "inherently risky - only enable it if you trust the mods in your pack.")
                    .define("bossDespawnCompat", false);

            this.killHonor = builder
                    .comment("Honor list; each entry declares its own match type with a prefix:",
                            "  tag,<scoreboard tag>      e.g. tag,odamaneFinalDeath",
                            "  type,<entity type id>     e.g. type,minecraft:ender_dragon",
                            "  uuid,<entity uuid>        e.g. uuid,123e4567-e89b-12d3-a456-426614174000",
                            "Matching targets only get a normal death attempt (no forced kill), so other mods",
                            "(e.g. Goety's End Ritual) can take over. Everything else is force-killed.")
                    .defineListAllowEmpty("killHonor",
                            List.of("tag,odamaneFinalDeath", "type,minecraft:ender_dragon", "type,minecraft:wither"),
                            value -> value instanceof String);

            builder.pop();

            builder.comment("Author's favor: put the tag on a single non-player entity (e.g. via KubeJS) to",
                    "blunt non-vanilla setHealth/die calls, resist discard, soften max-health cuts and show a",
                    "matching hint effect. Vanilla damage is never touched. /wyp kill always bypasses it.",
                    "Layout: tag / enabled + coefficients.").push("authorsFavor");

            this.authorsFavorEnabled = builder
                    .comment("Enable the author's favor protection.")
                    .define("enabled", true);

            this.authorsFavorFilter = builder
                    .comment("Which non-player entities get the protection, comma-separated matchers",
                            "(a ':' inside an id is kept as-is):",
                            "  tag,<scoreboard tag>   e.g. tag,authorsfavor",
                            "  type,<entity type id>  e.g. type,minecraft:ender_dragon",
                            "  uuid,<entity uuid>",
                            "Default keeps the old behaviour: any entity carrying the 'authorsfavor' tag.")
                    .defineListAllowEmpty("filter", List.of("tag,authorsfavor"), value -> value instanceof String);

            this.authorsFavorDamageCoefficient = builder
                    .comment("Only affects a setHealth call that does NOT come from the vanilla damage chain:",
                            "when code sets the health below its current value, the new value becomes",
                            "(current - requested) * coefficient, floored. Vanilla damage passes untouched.",
                            "Range 0 - 1. Default 0 = no protection; 1 = the drop is fully absorbed.",
                            "E.g. setHealth(0) at full health with 0.5 leaves half, with 1 leaves full health.")
                    .defineInRange("damageCoefficient", 0.0D, 0.0D, 1.0D);

            this.authorsFavorMaxHealthCoefficient = builder
                    .comment("Hard floor for a max-health cut, relative to the entity's historical max health.",
                            "Default 0 = no floor. 0.5 = max health can never be cut below half; 1 = cuts are fully reversed.")
                    .defineInRange("maxHealthCoefficient", 0.0D, 0.0D, 1.0D);

            this.authorsFavorMaxHealthChangeCoefficient = builder
                    .comment("How much of a max-health cut actually takes effect.",
                            "Default 1 = full effect (no protection). 0.5 = a cut has half its effect; 0 = cuts are ignored.")
                    .defineInRange("maxHealthChangeCoefficient", 1.0D, 0.0D, 1.0D);

            builder.pop();

            builder.comment("Entity freeze, grouped with the kill/control features (not the viewer).",
                    "Use /wyp freeze <targets> [ticks] [radius]; the config values are the defaults.").push("freeze");

            this.freezeEnabled = builder
                    .comment("Allow the /wyp freeze command to freeze entities.")
                    .define("enabled", false);

            this.freezeDuration = builder
                    .comment("Default freeze duration in ticks when the command omits it.")
                    .defineInRange("durationTicks", 100, 1, 72000);

            this.freezeRadius = builder
                    .comment("Default radius (blocks) to also freeze other non-player entities around each",
                            "target when the command omits it. 0 freezes only the targets.")
                    .defineInRange("radius", 0.0D, 0.0D, 64.0D);

            builder.pop();

            builder.comment("Entity Viewer settings").push("entityViewer");

            this.entityViewerEnabled = builder
                    .comment("Enable the Entity Viewer item.")
                    .define("enabled", true);

            this.entityViewerReach = builder
                    .comment("Extra reach (in blocks) for the viewer's own ray trace. 0 disables.")
                    .defineInRange("reachBonus", 0.0D, 0.0D, 256.0D);

            this.entityViewerHitbox = builder
                    .comment("Extra hitbox size used by the viewer's own ray trace.",
                            "Lets you select entities even when not perfectly aimed at them. 0 disables.")
                    .defineInRange("hitboxBonus", 0.0D, 0.0D, 16.0D);

            this.entityViewerSelectDrops = builder
                    .comment("Allow the Entity Viewer to select dropped item entities.")
                    .define("selectDroppedItems", true);

            this.entityViewerCooldown = builder
                    .comment("Cooldown in ticks between viewer inspections (prevents chat spam). 0 = off.")
                    .defineInRange("cooldownTicks", 10, 0, 200);

            builder.pop();

        }
    }
}
