package net.wieldyourpower.item;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wieldyourpower.WieldYourPower;

/**
 * Adds the Entity Viewer to the vanilla "Operator Utilities" creative tab instead of a custom tab.
 */
@Mod.EventBusSubscriber(modid = WieldYourPower.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CreativeTabEvents {

    private CreativeTabEvents() {
    }

    @SubscribeEvent
    public static void onBuildTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.OP_BLOCKS) {
            event.accept(ModItems.ENTITY_VIEWER.get());
        }
    }
}
