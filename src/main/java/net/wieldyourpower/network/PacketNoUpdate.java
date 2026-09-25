package net.wieldyourpower.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.wieldyourpower.util.NoUpdateMode;

import java.util.function.Supplier;

public class PacketNoUpdate {

    private final boolean active;

    public PacketNoUpdate(boolean active) {
        this.active = active;
    }

    public static void encode(PacketNoUpdate msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
    }

    public static PacketNoUpdate decode(FriendlyByteBuf buf) {
        return new PacketNoUpdate(buf.readBoolean());
    }

    public static void handle(PacketNoUpdate msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ServerPlayer sender = ctx.getSender();
        if (sender != null && sender.isCreative()) {
            NoUpdateMode.setActive(sender.getUUID(), msg.active);
        }
        ctx.setPacketHandled(true);
    }
}
