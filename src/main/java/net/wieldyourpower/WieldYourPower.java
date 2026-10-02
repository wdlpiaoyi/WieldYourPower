package net.wieldyourpower;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.wieldyourpower.capability.IPlayerLimits;
import net.wieldyourpower.compat.CuriosNoUpdate;
import net.wieldyourpower.effect.ModEffects;
import net.wieldyourpower.item.ModItems;
import net.wieldyourpower.network.WYPNetwork;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import org.slf4j.Logger;

@Mod(WieldYourPower.MODID)
public class WieldYourPower {

    public static final String MODID = "wieldyourpower";
    public static final String VERSION = "1.4.0";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WieldYourPower() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        modBus.addListener(this::registerCapabilities);
        modBus.addListener(this::commonSetup);
        ModItems.register(modBus);
        ModEffects.register(modBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, WYPConfig.SPEC);

        WYPNetwork.register();

        LOGGER.info("[WieldYourPower] WieldYourPower {} initialised", VERSION);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IPlayerLimits.class);
    }

    /** Optional integrations that must only be touched when their mod is present. */
    private void commonSetup(FMLCommonSetupEvent event) {
        if (ModList.get().isLoaded("curios")) {
            CuriosNoUpdate.register();
        }
    }
}
