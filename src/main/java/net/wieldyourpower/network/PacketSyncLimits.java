package net.wieldyourpower.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.wieldyourpower.capability.IPlayerLimits;
import net.wieldyourpower.capability.ModCapabilities;
import net.wieldyourpower.client.ClientPacketHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketSyncLimits {

    private final double walkSpeed;
    private final double flySpeedHorizontal;
    private final double flySpeedVertical;
    private final int mineSpeed;
    private final int mineInterval;
    private final double jumpLimit;
    private final double stepLimit;
    private final List<String> attributeLimits;
    private final List<String> allyProtection;

    public PacketSyncLimits(double walkSpeed, double flySpeedHorizontal, double flySpeedVertical,
                            int mineSpeed, int mineInterval, double jumpLimit, double stepLimit,
                            List<String> attributeLimits, List<String> allyProtection) {
        this.walkSpeed = walkSpeed;
        this.flySpeedHorizontal = flySpeedHorizontal;
        this.flySpeedVertical = flySpeedVertical;
        this.mineSpeed = mineSpeed;
        this.mineInterval = mineInterval;
        this.jumpLimit = jumpLimit;
        this.stepLimit = stepLimit;
        this.attributeLimits = List.copyOf(attributeLimits);
        this.allyProtection = List.copyOf(allyProtection);
    }

    public static PacketSyncLimits from(ServerPlayer player) {
        IPlayerLimits limits = ModCapabilities.resolve(player);
        if (limits == null) {
            return new PacketSyncLimits(-1, -1, -1, 0, 0, -1, -1, List.of(), List.of());
        }
        return new PacketSyncLimits(limits.getWalkSpeedLimit(), limits.getFlySpeedHorizontalLimit(),
                limits.getFlySpeedVerticalLimit(), limits.getMineSpeedLimit(), limits.getMineInterval(),
                limits.getJumpLimit(), limits.getStepLimit(), limits.getAttributeLimits(),
                limits.getAllyProtection());
    }

    public static void encode(PacketSyncLimits msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.walkSpeed);
        buf.writeDouble(msg.flySpeedHorizontal);
        buf.writeDouble(msg.flySpeedVertical);
        buf.writeInt(msg.mineSpeed);
        buf.writeInt(msg.mineInterval);
        buf.writeDouble(msg.jumpLimit);
        buf.writeDouble(msg.stepLimit);
        writeList(buf, msg.attributeLimits);
        writeList(buf, msg.allyProtection);
    }

    public static PacketSyncLimits decode(FriendlyByteBuf buf) {
        double walk = buf.readDouble();
        double flyH = buf.readDouble();
        double flyV = buf.readDouble();
        int mineSpeed = buf.readInt();
        int mineInterval = buf.readInt();
        double jump = buf.readDouble();
        double step = buf.readDouble();
        List<String> attributes = readList(buf);
        List<String> allies = readList(buf);
        return new PacketSyncLimits(walk, flyH, flyV, mineSpeed, mineInterval, jump, step, attributes, allies);
    }

    public static void handle(PacketSyncLimits msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleLimitsSync(
                msg.walkSpeed, msg.flySpeedHorizontal, msg.flySpeedVertical, msg.mineSpeed, msg.mineInterval,
                msg.jumpLimit, msg.stepLimit, msg.attributeLimits, msg.allyProtection));
        ctx.setPacketHandled(true);
    }

    private static void writeList(FriendlyByteBuf buf, List<String> list) {
        buf.writeInt(list.size());
        for (String entry : list) {
            buf.writeUtf(entry);
        }
    }

    private static List<String> readList(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(buf.readUtf());
        }
        return list;
    }
}
