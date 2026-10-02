package net.wieldyourpower.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.wieldyourpower.util.NoUpdateMode;

import java.util.function.Supplier;

/** Tells the client whether the server granted it the "no update" mode (accessory etc.). */
public class PacketNoUpdatePermission {

    private final boolean allowed;

    public PacketNoUpdatePermission(boolean allowed) {
        this.allowed = allowed;
    }

    public static void encode(PacketNoUpdatePermission msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.allowed);
    }

    public static PacketNoUpdatePermission decode(FriendlyByteBuf buf) {
        return new PacketNoUpdatePermission(buf.readBoolean());
    }

    public static void handle(PacketNoUpdatePermission msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        NoUpdateMode.setClientPermitted(msg.allowed);
        ctx.setPacketHandled(true);
    }
}
