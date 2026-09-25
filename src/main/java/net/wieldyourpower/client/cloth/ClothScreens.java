package net.wieldyourpower.client.cloth;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.StringListListEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.wieldyourpower.WYPConfig;
import net.wieldyourpower.client.ClientLimits;
import net.wieldyourpower.network.PacketUpdateLimits;
import net.wieldyourpower.network.WYPNetwork;

/**
 * Cloth Config screens: the per-player self-limits panel and the mod settings screen.
 */
@OnlyIn(Dist.CLIENT)
public final class ClothScreens {

    private ClothScreens() {
    }

    public static Screen limits(Screen parent) {
        final double[] speeds = {
                ClientLimits.walkSpeed, ClientLimits.flySpeedHorizontal, ClientLimits.flySpeedVertical
        };
        final int[] mines = { ClientLimits.mineSpeed, ClientLimits.mineInterval };
        final double[] extra = { ClientLimits.jumpLimit, ClientLimits.stepLimit };
        final java.util.List<String> attributes = new java.util.ArrayList<>(ClientLimits.attributeLimits);
        final java.util.List<String> allies = new java.util.ArrayList<>(ClientLimits.allyProtection);

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("wieldyourpower.screen.title"))
                .setSavingRunnable(() -> {
                    WYPNetwork.CHANNEL.sendToServer(
                            new PacketUpdateLimits(speeds[0], speeds[1], speeds[2], mines[0], mines[1],
                                    extra[0], extra[1], attributes, allies));
                    ClientLimits.setAll(speeds[0], speeds[1], speeds[2], mines[0], mines[1],
                            extra[0], extra[1], attributes, allies);
                });

        ConfigEntryBuilder entry = builder.entryBuilder();
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("wieldyourpower.category.limits"));

        category.addEntry(entry.startStrField(Component.translatable("wieldyourpower.field.walk"), format(ClientLimits.walkSpeed))
                .setDefaultValue("-1")
                .setTooltip(Component.translatable("wieldyourpower.tip.speed"))
                .setSaveConsumer(text -> speeds[0] = parseDouble(text, -1.0D))
                .build());
        category.addEntry(entry.startStrField(Component.translatable("wieldyourpower.field.fly_h"), format(ClientLimits.flySpeedHorizontal))
                .setDefaultValue("-1")
                .setTooltip(Component.translatable("wieldyourpower.tip.speed"))
                .setSaveConsumer(text -> speeds[1] = parseDouble(text, -1.0D))
                .build());
        category.addEntry(entry.startStrField(Component.translatable("wieldyourpower.field.fly_v"), format(ClientLimits.flySpeedVertical))
                .setDefaultValue("-1")
                .setTooltip(Component.translatable("wieldyourpower.tip.fly_v"))
                .setSaveConsumer(text -> speeds[2] = parseDouble(text, -1.0D))
                .build());
        category.addEntry(entry.startStrField(Component.translatable("wieldyourpower.field.mine_speed"), String.valueOf(ClientLimits.mineSpeed))
                .setDefaultValue("0")
                .setTooltip(Component.translatable("wieldyourpower.tip.mine_speed"))
                .setSaveConsumer(text -> mines[0] = Math.max(-1, parseInt(text, 0)))
                .build());
        category.addEntry(entry.startStrField(Component.translatable("wieldyourpower.field.mine_interval"), String.valueOf(ClientLimits.mineInterval))
                .setDefaultValue("0")
                .setTooltip(Component.translatable("wieldyourpower.tip.mine_interval"))
                .setSaveConsumer(text -> mines[1] = Math.max(0, parseInt(text, 0)))
                .build());
        category.addEntry(entry.startStrField(Component.translatable("wieldyourpower.field.jump"), format(ClientLimits.jumpLimit))
                .setDefaultValue("-1")
                .setTooltip(Component.translatable("wieldyourpower.tip.jump"))
                .setSaveConsumer(text -> extra[0] = parseDouble(text, -1.0D))
                .build());
        category.addEntry(entry.startStrField(Component.translatable("wieldyourpower.field.step"), format(ClientLimits.stepLimit))
                .setDefaultValue("-1")
                .setTooltip(Component.translatable("wieldyourpower.tip.step"))
                .setSaveConsumer(text -> extra[1] = parseDouble(text, -1.0D))
                .build());
        StringListListEntry attributeEntry = entry.startStrList(Component.translatable("wieldyourpower.field.attributes"),
                        new java.util.ArrayList<>(ClientLimits.attributeLimits))
                .setDefaultValue(java.util.List.of())
                .setTooltip(Component.translatable("wieldyourpower.tip.attribute"))
                .setSaveConsumer(list -> {
                    attributes.clear();
                    attributes.addAll(list);
                })
                .build();
        category.addEntry(attributeEntry);
        category.addEntry(new QuickAddEntry(Component.translatable("wieldyourpower.field.attribute_add"),
                attributeEntry.getValue(), new String[0], new String[]{"attribute"}));
        StringListListEntry allyEntry = entry.startStrList(Component.translatable("wieldyourpower.field.ally_protection"),
                        new java.util.ArrayList<>(ClientLimits.allyProtection))
                .setDefaultValue(java.util.List.of("type,touhou_little_maid:maid"))
                .setExpanded(true)
                .setTooltip(Component.translatable("wieldyourpower.tip.ally_protection"))
                .setSaveConsumer(list -> {
                    allies.clear();
                    allies.addAll(list);
                })
                .build();
        category.addEntry(allyEntry);
        category.addEntry(new QuickAddEntry(Component.translatable("wieldyourpower.field.ally_add"),
                allyEntry.getValue(), new String[0], new String[]{"tag", "type", "uuid"}));

        return builder.build();
    }

    public static Screen config(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("wieldyourpower.screen.config"))
                .setSavingRunnable(ClothScreens::saveConfig);

        ConfigEntryBuilder entry = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("wieldyourpower.category.general"));
        general.addEntry(entry.startBooleanToggle(Component.translatable("wieldyourpower.config.creativeDefense"),
                        WYPConfig.COMMON.creativeDefense.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("wieldyourpower.tip.creativeDefense"))
                .setSaveConsumer(value -> WYPConfig.COMMON.creativeDefense.set(value))
                .build());
        general.addEntry(entry.startStrField(Component.translatable("wieldyourpower.config.creativeMinHealth"),
                        String.valueOf(WYPConfig.COMMON.creativeMinHealth.get()))
                .setDefaultValue("20")
                .setTooltip(Component.translatable("wieldyourpower.tip.creativeMinHealth"))
                .setSaveConsumer(text -> WYPConfig.COMMON.creativeMinHealth.set(
                        Math.max(0.0D, parseDouble(text, 20.0D))))
                .build());
        general.addEntry(entry.startStrField(Component.translatable("wieldyourpower.config.creativeMinMaxHealth"),
                        String.valueOf(WYPConfig.COMMON.creativeMinMaxHealth.get()))
                .setDefaultValue("20")
                .setTooltip(Component.translatable("wieldyourpower.tip.creativeMinMaxHealth"))
                .setSaveConsumer(text -> WYPConfig.COMMON.creativeMinMaxHealth.set(
                        Math.max(1.0D, parseDouble(text, 20.0D))))
                .build());
        general.addEntry(entry.startEnumSelector(Component.translatable("wieldyourpower.config.creativeHitboxMode"),
                        WYPConfig.HitboxMode.class, WYPConfig.COMMON.creativeHitboxMode.get())
                .setDefaultValue(WYPConfig.HitboxMode.REMOVE_UNLESS_SNEAK_GROUND)
                .setTooltip(Component.translatable("wieldyourpower.tip.creativeHitboxMode"))
                .setSaveConsumer(value -> WYPConfig.COMMON.creativeHitboxMode.set(value))
                .build());
        general.addEntry(entry.startBooleanToggle(Component.translatable("wieldyourpower.config.creativeBreaksProtectedBlocks"),
                        WYPConfig.COMMON.creativeBreaksProtectedBlocks.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("wieldyourpower.tip.creativeBreaksProtectedBlocks"))
                .setSaveConsumer(value -> WYPConfig.COMMON.creativeBreaksProtectedBlocks.set(value))
                .build());
        general.addEntry(entry.startBooleanToggle(Component.translatable("wieldyourpower.config.creativePlacesBlocks"),
                        WYPConfig.COMMON.creativePlacesBlocks.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("wieldyourpower.tip.creativePlacesBlocks"))
                .setSaveConsumer(value -> WYPConfig.COMMON.creativePlacesBlocks.set(value))
                .build());
        general.addEntry(entry.startBooleanToggle(Component.translatable("wieldyourpower.config.creativePlacesThroughEntities"),
                        WYPConfig.COMMON.creativePlacesThroughEntities.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("wieldyourpower.tip.creativePlacesThroughEntities"))
                .setSaveConsumer(value -> WYPConfig.COMMON.creativePlacesThroughEntities.set(value))
                .build());
        general.addEntry(entry.startEnumSelector(Component.translatable("wieldyourpower.config.noUpdatePlacementMode"),
                        WYPConfig.PlacementUpdateMode.class, WYPConfig.COMMON.noUpdatePlacementMode.get())
                .setDefaultValue(WYPConfig.PlacementUpdateMode.HOLD)
                .setTooltip(Component.translatable("wieldyourpower.tip.noUpdatePlacementMode"))
                .setSaveConsumer(value -> WYPConfig.COMMON.noUpdatePlacementMode.set(value))
                .build());
        general.addEntry(entry.startBooleanToggle(Component.translatable("wieldyourpower.config.blockBreakerEnabled"),
                        WYPConfig.COMMON.blockBreakerEnabled.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("wieldyourpower.tip.blockBreakerEnabled"))
                .setSaveConsumer(value -> WYPConfig.COMMON.blockBreakerEnabled.set(value))
                .build());
        general.addEntry(entry.startBooleanToggle(Component.translatable("wieldyourpower.config.blockProtectionBypass"),
                        WYPConfig.COMMON.blockProtectionBypass.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("wieldyourpower.tip.blockProtectionBypass"))
                .setSaveConsumer(value -> WYPConfig.COMMON.blockProtectionBypass.set(value))
                .build());
        general.addEntry(entry.startBooleanToggle(Component.translatable("wieldyourpower.config.killPierces"),
                        WYPConfig.COMMON.killPiercesProtection.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("wieldyourpower.tip.killPierces"))
                .setSaveConsumer(value -> WYPConfig.COMMON.killPiercesProtection.set(value))
                .build());
        general.addEntry(entry.startBooleanToggle(Component.translatable("wieldyourpower.config.killForceRemoval"),
                        WYPConfig.COMMON.killForceRemoval.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("wieldyourpower.tip.killForceRemoval"))
                .setSaveConsumer(value -> WYPConfig.COMMON.killForceRemoval.set(value))
                .build());
        general.addEntry(entry.startBooleanToggle(Component.translatable("wieldyourpower.config.killClearBossBars"),
                        WYPConfig.COMMON.killClearBossBars.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("wieldyourpower.tip.killClearBossBars"))
                .setSaveConsumer(value -> WYPConfig.COMMON.killClearBossBars.set(value))
                .build());
        general.addEntry(entry.startBooleanToggle(Component.translatable("wieldyourpower.config.bossDespawnCompat"),
                        WYPConfig.COMMON.bossDespawnCompat.get())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("wieldyourpower.tip.bossDespawnCompat"))
                .setSaveConsumer(value -> WYPConfig.COMMON.bossDespawnCompat.set(value))
                .build());
        StringListListEntry killHonorEntry = entry.startStrList(Component.translatable("wieldyourpower.config.killHonor"),
                        new java.util.ArrayList<>(WYPConfig.COMMON.killHonor.get()))
                .setDefaultValue(java.util.List.of("tag,odamaneFinalDeath", "type,minecraft:ender_dragon",
                        "type,minecraft:wither"))
                .setExpanded(false)
                .setTooltip(Component.translatable("wieldyourpower.tip.killHonor"))
                .setSaveConsumer(list -> WYPConfig.COMMON.killHonor.set(list))
                .build();
        general.addEntry(killHonorEntry);
        general.addEntry(new QuickAddEntry(Component.translatable("wieldyourpower.field.killHonor_add"),
                killHonorEntry.getValue(), new String[0], new String[]{"tag", "type", "uuid"}));

        ConfigCategory freeze = builder.getOrCreateCategory(Component.translatable("wieldyourpower.category.freeze"));
        freeze.addEntry(entry.startBooleanToggle(Component.translatable("wieldyourpower.config.freezeEnabled"),
                        WYPConfig.COMMON.freezeEnabled.get())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("wieldyourpower.tip.freezeEnabled"))
                .setSaveConsumer(value -> WYPConfig.COMMON.freezeEnabled.set(value))
                .build());
        freeze.addEntry(entry.startIntField(Component.translatable("wieldyourpower.config.freezeDuration"),
                        WYPConfig.COMMON.freezeDuration.get())
                .setDefaultValue(100)
                .setMin(1)
                .setTooltip(Component.translatable("wieldyourpower.tip.freezeDuration"))
                .setSaveConsumer(value -> WYPConfig.COMMON.freezeDuration.set(value))
                .build());
        freeze.addEntry(entry.startStrField(Component.translatable("wieldyourpower.config.freezeRadius"),
                        String.valueOf(WYPConfig.COMMON.freezeRadius.get()))
                .setDefaultValue("0")
                .setTooltip(Component.translatable("wieldyourpower.tip.freezeRadius"))
                .setSaveConsumer(text -> WYPConfig.COMMON.freezeRadius.set(parseDouble(text, 0.0D)))
                .build());

        ConfigCategory viewer = builder.getOrCreateCategory(Component.translatable("wieldyourpower.category.viewer"));
        viewer.addEntry(entry.startBooleanToggle(Component.translatable("wieldyourpower.config.viewerEnabled"),
                        WYPConfig.COMMON.entityViewerEnabled.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("wieldyourpower.tip.viewerEnabled"))
                .setSaveConsumer(value -> WYPConfig.COMMON.entityViewerEnabled.set(value))
                .build());
        viewer.addEntry(entry.startStrField(Component.translatable("wieldyourpower.config.viewerReach"),
                        String.valueOf(WYPConfig.COMMON.entityViewerReach.get()))
                .setDefaultValue("0")
                .setTooltip(Component.translatable("wieldyourpower.tip.viewerReach"))
                .setSaveConsumer(text -> WYPConfig.COMMON.entityViewerReach.set(parseDouble(text, 0.0D)))
                .build());
        viewer.addEntry(entry.startStrField(Component.translatable("wieldyourpower.config.viewerHitbox"),
                        String.valueOf(WYPConfig.COMMON.entityViewerHitbox.get()))
                .setDefaultValue("0")
                .setTooltip(Component.translatable("wieldyourpower.tip.viewerHitbox"))
                .setSaveConsumer(text -> WYPConfig.COMMON.entityViewerHitbox.set(parseDouble(text, 0.0D)))
                .build());
        viewer.addEntry(entry.startBooleanToggle(Component.translatable("wieldyourpower.config.viewerSelectDrops"),
                        WYPConfig.COMMON.entityViewerSelectDrops.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("wieldyourpower.tip.viewerSelectDrops"))
                .setSaveConsumer(value -> WYPConfig.COMMON.entityViewerSelectDrops.set(value))
                .build());
        viewer.addEntry(entry.startIntField(Component.translatable("wieldyourpower.config.viewerCooldown"),
                        WYPConfig.COMMON.entityViewerCooldown.get())
                .setDefaultValue(10)
                .setMin(0)
                .setMax(200)
                .setTooltip(Component.translatable("wieldyourpower.tip.viewerCooldown"))
                .setSaveConsumer(value -> WYPConfig.COMMON.entityViewerCooldown.set(value))
                .build());

        ConfigCategory favor = builder.getOrCreateCategory(Component.translatable("wieldyourpower.category.authorsFavor"));
        favor.addEntry(entry.startBooleanToggle(Component.translatable("wieldyourpower.config.authorsFavorEnabled"),
                        WYPConfig.COMMON.authorsFavorEnabled.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("wieldyourpower.tip.authorsFavorEnabled"))
                .setSaveConsumer(value -> WYPConfig.COMMON.authorsFavorEnabled.set(value))
                .build());
        StringListListEntry favorFilterEntry = entry.startStrList(Component.translatable("wieldyourpower.config.authorsFavorFilter"),
                        new java.util.ArrayList<>(WYPConfig.COMMON.authorsFavorFilter.get()))
                .setDefaultValue(java.util.List.of("tag,authorsfavor"))
                .setExpanded(true)
                .setTooltip(Component.translatable("wieldyourpower.tip.authorsFavorFilter"))
                .setSaveConsumer(list -> WYPConfig.COMMON.authorsFavorFilter.set(list))
                .build();
        favor.addEntry(favorFilterEntry);
        favor.addEntry(new QuickAddEntry(Component.translatable("wieldyourpower.field.authorsFavor_add"),
                favorFilterEntry.getValue(), new String[0], new String[]{"tag", "type", "uuid"}));
        favor.addEntry(entry.startStrField(Component.translatable("wieldyourpower.config.authorsFavorDamage"),
                        String.valueOf(WYPConfig.COMMON.authorsFavorDamageCoefficient.get()))
                .setDefaultValue("0")
                .setTooltip(Component.translatable("wieldyourpower.tip.authorsFavorDamage"))
                .setSaveConsumer(text -> WYPConfig.COMMON.authorsFavorDamageCoefficient.set(
                        clamp01(parseDouble(text, 0.0D))))
                .build());
        favor.addEntry(entry.startStrField(Component.translatable("wieldyourpower.config.authorsFavorMaxHealth"),
                        String.valueOf(WYPConfig.COMMON.authorsFavorMaxHealthCoefficient.get()))
                .setDefaultValue("0")
                .setTooltip(Component.translatable("wieldyourpower.tip.authorsFavorMaxHealth"))
                .setSaveConsumer(text -> WYPConfig.COMMON.authorsFavorMaxHealthCoefficient.set(
                        clamp01(parseDouble(text, 0.0D))))
                .build());
        favor.addEntry(entry.startStrField(Component.translatable("wieldyourpower.config.authorsFavorMaxHealthChange"),
                        String.valueOf(WYPConfig.COMMON.authorsFavorMaxHealthChangeCoefficient.get()))
                .setDefaultValue("1")
                .setTooltip(Component.translatable("wieldyourpower.tip.authorsFavorMaxHealthChange"))
                .setSaveConsumer(text -> WYPConfig.COMMON.authorsFavorMaxHealthChangeCoefficient.set(
                        clamp01(parseDouble(text, 1.0D))))
                .build());

        return builder.build();
    }

    private static void saveConfig() {
        WYPConfig.COMMON.creativeDefense.save();
        WYPConfig.COMMON.creativeMinHealth.save();
        WYPConfig.COMMON.creativeMinMaxHealth.save();
        WYPConfig.COMMON.creativeHitboxMode.save();
        WYPConfig.COMMON.creativeBreaksProtectedBlocks.save();
        WYPConfig.COMMON.creativePlacesBlocks.save();
        WYPConfig.COMMON.creativePlacesThroughEntities.save();
        WYPConfig.COMMON.noUpdatePlacementMode.save();
        WYPConfig.COMMON.blockBreakerEnabled.save();
        WYPConfig.COMMON.blockProtectionBypass.save();
        WYPConfig.COMMON.killPiercesProtection.save();
        WYPConfig.COMMON.killForceRemoval.save();
        WYPConfig.COMMON.killClearBossBars.save();
        WYPConfig.COMMON.bossDespawnCompat.save();
        WYPConfig.COMMON.killHonor.save();
        WYPConfig.COMMON.freezeEnabled.save();
        WYPConfig.COMMON.freezeDuration.save();
        WYPConfig.COMMON.freezeRadius.save();
        WYPConfig.COMMON.entityViewerEnabled.save();
        WYPConfig.COMMON.entityViewerReach.save();
        WYPConfig.COMMON.entityViewerHitbox.save();
        WYPConfig.COMMON.entityViewerSelectDrops.save();
        WYPConfig.COMMON.entityViewerCooldown.save();
        WYPConfig.COMMON.authorsFavorEnabled.save();
        WYPConfig.COMMON.authorsFavorFilter.save();
        WYPConfig.COMMON.authorsFavorDamageCoefficient.save();
        WYPConfig.COMMON.authorsFavorMaxHealthCoefficient.save();
        WYPConfig.COMMON.authorsFavorMaxHealthChangeCoefficient.save();
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static String format(double value) {
        if (value < 0.0D) {
            return "-1";
        }
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.format("%.3f", value);
    }

    private static double parseDouble(String text, double fallback) {
        try {
            return Double.parseDouble(text.trim());
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (RuntimeException exception) {
            return fallback;
        }
    }
}
