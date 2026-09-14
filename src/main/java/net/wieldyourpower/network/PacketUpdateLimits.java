package net.wieldyourpower.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.wieldyourpower.capability.IPlayerLimits;
import net.wieldyourpower.capability.ModCapabilities;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketUpdateLimits {

    private final double walkSpeed;
    private final double flySpeedHorizontal;
    private final double flySpeedVertical;
    private final int mineSpeed;
    private final int mineInterval;
    private final double jumpLimit;
    private final double stepLimit;
    private final List<String> attributeLimits;
    private final List<String> allyProtection;

    public PacketUpdateLimits(double walkSpeed, double flySpeedHorizontal, double flySpeedVertical,
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

    public static void encode(PacketUpdateLimits msg, FriendlyByteBuf buf) {
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

    public static PacketUpdateLimits decode(FriendlyByteBuf buf) {
        double walk = buf.readDouble();
        double flyH = buf.readDouble();
        double flyV = buf.readDouble();
        int mineSpeed = buf.readInt();
        int mineInterval = buf.readInt();
        double jump = buf.readDouble();
        double step = buf.readDouble();
        List<String> attributes = readList(buf);
        List<String> allies = readList(buf);
        return new PacketUpdateLimits(walk, flyH, flyV, mineSpeed, mineInterval, jump, step, attributes, allies);
    }

    public static void handle(PacketUpdateLimits msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ServerPlayer sender = ctx.getSender();
        if (sender != null) {
            IPlayerLimits limits = ModCapabilities.resolve(sender);
            if (limits != null) {
                limits.setWalkSpeedLimit(msg.walkSpeed);
                limits.setFlySpeedHorizontalLimit(msg.flySpeedHorizontal);
                limits.setFlySpeedVerticalLimit(msg.flySpeedVertical);
                limits.setMineSpeedLimit(msg.mineSpeed);
                limits.setMineInterval(msg.mineInterval);
                limits.setJumpLimit(msg.jumpLimit);
                limits.setStepLimit(msg.stepLimit);
                limits.setAttributeLimits(msg.attributeLimits);
                limits.setAllyProtection(msg.allyProtection);
            }
            WYPNetwork.syncTo(sender);
            sender.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "commands.wieldyourpower.limits.applied",
                    msg.walkSpeed, msg.flySpeedHorizontal, msg.flySpeedVertical, msg.mineSpeed, msg.mineInterval));
        }
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
