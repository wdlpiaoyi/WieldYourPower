package net.wieldyourpower.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wieldyourpower.WieldYourPower;
import net.wieldyourpower.capability.IPlayerLimits;
import net.wieldyourpower.capability.ModCapabilities;
import net.wieldyourpower.capability.PlayerLimitsProvider;
import net.wieldyourpower.command.WYPCommand;

@Mod.EventBusSubscriber(modid = WieldYourPower.MODID)
public final class CommonEvents {

    private CommonEvents() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            PlayerLimitsProvider provider = new PlayerLimitsProvider();
            event.addCapability(new ResourceLocation(WieldYourPower.MODID, "limits"), provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        IPlayerLimits oldLimits = ModCapabilities.resolve(event.getOriginal());
        IPlayerLimits newLimits = ModCapabilities.resolve(event.getEntity());
        if (oldLimits != null && newLimits != null) {
            newLimits.copyFrom(oldLimits);
        }
        event.getOriginal().invalidateCaps();

        net.wieldyourpower.util.KillUtil.clearKilled(event.getEntity().getUUID());
        CreativeDefenseEvents.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            net.wieldyourpower.network.WYPNetwork.syncTo(player);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        WYPCommand.register(event.getDispatcher());
    }
}
