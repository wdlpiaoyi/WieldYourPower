package net.wieldyourpower.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.wieldyourpower.WieldYourPower;

public final class WYPNetwork {

    private static final String VERSION = "3";
    private static int nextId = 0;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(WieldYourPower.MODID, "main"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals);

    private WYPNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(PacketUpdateLimits.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PacketUpdateLimits::encode)
                .decoder(PacketUpdateLimits::decode)
                .consumerMainThread((msg, ctx) -> PacketUpdateLimits.handle(msg, ctx))
                .add();

        CHANNEL.messageBuilder(PacketSyncLimits.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketSyncLimits::encode)
                .decoder(PacketSyncLimits::decode)
                .consumerMainThread((msg, ctx) -> PacketSyncLimits.handle(msg, ctx))
                .add();

        CHANNEL.messageBuilder(PacketOpenPanel.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketOpenPanel::encode)
                .decoder(PacketOpenPanel::decode)
                .consumerMainThread((msg, ctx) -> PacketOpenPanel.handle(msg, ctx))
                .add();

        CHANNEL.messageBuilder(PacketNoUpdate.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PacketNoUpdate::encode)
                .decoder(PacketNoUpdate::decode)
                .consumerMainThread((msg, ctx) -> PacketNoUpdate.handle(msg, ctx))
                .add();
    }

    public static void syncTo(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), PacketSyncLimits.from(player));
    }
}
