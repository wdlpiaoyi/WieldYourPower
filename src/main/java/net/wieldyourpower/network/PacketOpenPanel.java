package net.wieldyourpower.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.wieldyourpower.client.ClientPacketHandler;

import java.util.function.Supplier;

public class PacketOpenPanel {

    public static final int LIMITS = 0;
    public static final int CONFIG = 1;

    private final int screen;

    public PacketOpenPanel(int screen) {
        this.screen = screen;
    }

    public static void encode(PacketOpenPanel msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.screen);
    }

    public static PacketOpenPanel decode(FriendlyByteBuf buf) {
        return new PacketOpenPanel(buf.readInt());
    }

    public static void handle(PacketOpenPanel msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.openScreen(msg.screen));
        ctx.setPacketHandled(true);
    }
}
