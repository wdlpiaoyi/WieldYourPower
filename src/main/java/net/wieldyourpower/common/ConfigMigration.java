package net.wieldyourpower.common;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.wieldyourpower.WYPConfig;
import net.wieldyourpower.WieldYourPower;
import net.wieldyourpower.util.FilterSyntax;

import java.util.ArrayList;
import java.util.List;

/**
 * Rewrites legacy {@code key:value} entries in the filter lists to the new {@code key,value} form when the
 * config loads, so old configs keep working without hand editing.
 */
@Mod.EventBusSubscriber(modid = WieldYourPower.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ConfigMigration {

    private ConfigMigration() {
    }

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getType() != ModConfig.Type.COMMON) {
            return;
        }
        List<? extends String> honor = WYPConfig.COMMON.killHonor.get();
        List<String> normalized = FilterSyntax.normalizeAll(honor);
        if (!normalized.equals(new ArrayList<>(honor))) {
            WYPConfig.COMMON.killHonor.set(normalized);
            WYPConfig.COMMON.killHonor.save();
        }
    }
}
