package net.wieldyourpower.client;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wieldyourpower.WieldYourPower;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = WieldYourPower.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSetup {

    public static final KeyMapping OPEN_PANEL = new KeyMapping(
            "key.wieldyourpower.open_panel",
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.wieldyourpower");

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_PANEL);
    }
}
