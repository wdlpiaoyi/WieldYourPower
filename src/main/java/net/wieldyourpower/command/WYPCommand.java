package net.wieldyourpower.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.wieldyourpower.WYPConfig;
import net.wieldyourpower.capability.IPlayerLimits;
import net.wieldyourpower.capability.ModCapabilities;
import net.wieldyourpower.network.PacketOpenPanel;
import net.wieldyourpower.network.WYPNetwork;
import net.wieldyourpower.util.FrozenEntities;

import java.util.Collection;

public final class WYPCommand {

    private WYPCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("wyp")
                .then(Commands.literal("panel")
                        .executes(ctx -> openPanel(ctx.getSource(), net.wieldyourpower.network.PacketOpenPanel.LIMITS)))
                .then(Commands.literal("config")
                        .executes(ctx -> openPanel(ctx.getSource(), net.wieldyourpower.network.PacketOpenPanel.CONFIG)))
                .then(Commands.literal("kill")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> KillCommand.execute(ctx.getSource(),
                                java.util.List.of(ctx.getSource().getEntityOrException())))
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(ctx -> KillCommand.execute(ctx.getSource(),
                                        EntityArgument.getEntities(ctx, "targets")))))
                .then(Commands.literal("killhonor")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> KillCommand.executeHonor(ctx.getSource(),
                                java.util.List.of(ctx.getSource().getEntityOrException())))
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(ctx -> KillCommand.executeHonor(ctx.getSource(),
                                        EntityArgument.getEntities(ctx, "targets")))))
                .then(Commands.literal("freeze")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(ctx -> freeze(ctx.getSource(), EntityArgument.getEntities(ctx, "targets"),
                                        WYPConfig.COMMON.freezeDuration.get(), WYPConfig.COMMON.freezeRadius.get()))
                                .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 72000))
                                        .executes(ctx -> freeze(ctx.getSource(), EntityArgument.getEntities(ctx, "targets"),
                                                IntegerArgumentType.getInteger(ctx, "ticks"),
                                                WYPConfig.COMMON.freezeRadius.get()))
                                        .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0.0D, 64.0D))
                                                .executes(ctx -> freeze(ctx.getSource(), EntityArgument.getEntities(ctx, "targets"),
                                                        IntegerArgumentType.getInteger(ctx, "ticks"),
                                                        DoubleArgumentType.getDouble(ctx, "radius")))))))
                .then(Commands.literal("favor")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("add")
                                .executes(ctx -> favor(ctx.getSource(),
                                        java.util.List.of(ctx.getSource().getEntityOrException()), true))
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .executes(ctx -> favor(ctx.getSource(), EntityArgument.getEntities(ctx, "targets"), true))))
                        .then(Commands.literal("remove")
                                .executes(ctx -> favor(ctx.getSource(),
                                        java.util.List.of(ctx.getSource().getEntityOrException()), false))
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .executes(ctx -> favor(ctx.getSource(), EntityArgument.getEntities(ctx, "targets"), false)))))
                .then(Commands.literal("jump")
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                .executes(ctx -> setJump(ctx.getSource(), ctx.getSource().getPlayerOrException(), DoubleArgumentType.getDouble(ctx, "value")))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .requires(source -> source.hasPermission(2))
                                        .executes(ctx -> setJump(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), DoubleArgumentType.getDouble(ctx, "value"))))))
                .then(Commands.literal("step")
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                .executes(ctx -> setStep(ctx.getSource(), ctx.getSource().getPlayerOrException(), DoubleArgumentType.getDouble(ctx, "value")))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .requires(source -> source.hasPermission(2))
                                        .executes(ctx -> setStep(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), DoubleArgumentType.getDouble(ctx, "value"))))))
                .then(Commands.literal("attribute")
                        .then(Commands.argument("id", com.mojang.brigadier.arguments.StringArgumentType.string())
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                        .executes(ctx -> setAttribute(ctx.getSource(), ctx.getSource().getPlayerOrException(),
                                                com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "id"),
                                                DoubleArgumentType.getDouble(ctx, "value")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> source.hasPermission(2))
                                                .executes(ctx -> setAttribute(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                                        com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "id"),
                                                        DoubleArgumentType.getDouble(ctx, "value")))))))
                .then(Commands.literal("status")
                        .executes(ctx -> status(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> status(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("reset")
                        .executes(ctx -> reset(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> reset(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("set")
                        .then(Commands.literal("walkSpeed")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                        .executes(ctx -> setWalk(ctx.getSource(), ctx.getSource().getPlayerOrException(), DoubleArgumentType.getDouble(ctx, "value")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> source.hasPermission(2))
                                                .executes(ctx -> setWalk(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), DoubleArgumentType.getDouble(ctx, "value"))))))
                        .then(Commands.literal("flyH")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                        .executes(ctx -> setFlyH(ctx.getSource(), ctx.getSource().getPlayerOrException(), DoubleArgumentType.getDouble(ctx, "value")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> source.hasPermission(2))
                                                .executes(ctx -> setFlyH(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), DoubleArgumentType.getDouble(ctx, "value"))))))
                        .then(Commands.literal("flyV")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                        .executes(ctx -> setFlyV(ctx.getSource(), ctx.getSource().getPlayerOrException(), DoubleArgumentType.getDouble(ctx, "value")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> source.hasPermission(2))
                                                .executes(ctx -> setFlyV(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), DoubleArgumentType.getDouble(ctx, "value"))))))
                        .then(Commands.literal("mineSpeed")
                                .then(Commands.argument("value", IntegerArgumentType.integer(-1))
                                        .executes(ctx -> setMineSpeed(ctx.getSource(), ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "value")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> source.hasPermission(2))
                                                .executes(ctx -> setMineSpeed(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "value"))))))
                        .then(Commands.literal("mineInterval")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(ctx -> setMineInterval(ctx.getSource(), ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "value")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> source.hasPermission(2))
                                                .executes(ctx -> setMineInterval(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "value"))))))));
    }

    private static int openPanel(CommandSourceStack source, int screen) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        WYPNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new PacketOpenPanel(screen));
        return 1;
    }

    private static int freeze(CommandSourceStack source, Collection<? extends Entity> targets, int ticks, double radius) {
        if (!WYPConfig.COMMON.freezeEnabled.get()) {
            source.sendFailure(Component.translatable("commands.wieldyourpower.freeze.disabled"));
            return 0;
        }
        int count = 0;
        for (Entity entity : targets) {
            if (!(entity instanceof LivingEntity living) || living instanceof net.minecraft.world.entity.player.Player) {
                continue;
            }
            FrozenEntities.freeze(living, ticks);
            count++;
            if (radius > 0.0D) {
                for (LivingEntity other : living.level().getEntitiesOfClass(
                        LivingEntity.class, living.getBoundingBox().inflate(radius),
                        other -> !(other instanceof net.minecraft.world.entity.player.Player))) {
                    FrozenEntities.freeze(other, ticks);
                }
            }
        }
        final int frozen = count;
        if (frozen == 0) {
            source.sendFailure(Component.translatable("commands.wieldyourpower.freeze.none"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.wieldyourpower.freeze.success", frozen, ticks), true);
        return frozen;
    }

    private static int favor(CommandSourceStack source, Collection<? extends Entity> targets, boolean add) {
        String tag = WYPConfig.COMMON.authorsFavorTag.get();
        if (tag.isEmpty()) {
            source.sendFailure(Component.translatable("commands.wieldyourpower.favor.notag"));
            return 0;
        }
        int count = 0;
        for (Entity entity : targets) {
            if (entity instanceof net.minecraft.world.entity.player.Player) {
                continue;
            }
            boolean changed = add ? entity.addTag(tag) : entity.removeTag(tag);
            if (changed) {
                count++;
            }
        }
        if (count == 0) {
            source.sendFailure(Component.translatable(add
                    ? "commands.wieldyourpower.favor.add.none"
                    : "commands.wieldyourpower.favor.remove.none"));
            return 0;
        }
        final int changed = count;
        source.sendSuccess(() -> Component.translatable(add
                ? "commands.wieldyourpower.favor.add.success"
                : "commands.wieldyourpower.favor.remove.success", changed, tag), true);
        return changed;
    }

    private static int status(CommandSourceStack source, ServerPlayer target) {
        IPlayerLimits limits = ModCapabilities.resolve(target);
        if (limits == null) {
            source.sendFailure(Component.literal("No limit data for " + target.getGameProfile().getName()));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.wieldyourpower.status.header", target.getGameProfile().getName()), false);
        source.sendSuccess(() -> Component.translatable("commands.wieldyourpower.status.walk", fmt(limits.getWalkSpeedLimit())), false);
        source.sendSuccess(() -> Component.translatable("commands.wieldyourpower.status.flyH", fmt(limits.getFlySpeedHorizontalLimit())), false);
        source.sendSuccess(() -> Component.translatable("commands.wieldyourpower.status.flyV", fmt(limits.getFlySpeedVerticalLimit())), false);
        source.sendSuccess(() -> Component.translatable("commands.wieldyourpower.status.mineSpeed", limits.getMineSpeedLimit()), false);
        source.sendSuccess(() -> Component.translatable("commands.wieldyourpower.status.mineInterval", limits.getMineInterval()), false);
        source.sendSuccess(() -> Component.translatable("commands.wieldyourpower.status.jump", fmt(limits.getJumpLimit())), false);
        source.sendSuccess(() -> Component.translatable("commands.wieldyourpower.status.step", fmt(limits.getStepLimit())), false);
        source.sendSuccess(() -> Component.translatable("commands.wieldyourpower.status.attribute",
                limits.getAttributeLimits().isEmpty() ? "-" : String.join(", ", limits.getAttributeLimits())), false);
        return 1;
    }

    private static int reset(CommandSourceStack source, ServerPlayer target) {
        IPlayerLimits limits = ModCapabilities.resolve(target);
        if (limits == null) {
            return 0;
        }
        limits.setWalkSpeedLimit(-1);
        limits.setFlySpeedHorizontalLimit(-1);
        limits.setFlySpeedVerticalLimit(-1);
        limits.setMineSpeedLimit(0);
        limits.setMineInterval(0);
        limits.setJumpLimit(-1);
        limits.setStepLimit(-1);
        limits.setAttributeLimits(java.util.List.of());
        WYPNetwork.syncTo(target);
        source.sendSuccess(() -> Component.translatable("commands.wieldyourpower.reset", target.getGameProfile().getName()), true);
        return 1;
    }

    private static int setWalk(CommandSourceStack source, ServerPlayer target, double value) {
        IPlayerLimits limits = ModCapabilities.resolve(target);
        if (limits == null) {
            return 0;
        }
        limits.setWalkSpeedLimit(value);
        WYPNetwork.syncTo(target);
        source.sendSuccess(() -> Component.translatable("commands.wieldyourpower.set.walk", fmt(value)), false);
        return 1;
    }

    private static int setFlyH(CommandSourceStack source, ServerPlayer target, double value) {
        IPlayerLimits limits = ModCapabilities.resolve(target);
        if (limits == null) {
            return 0;
        }
        limits.setFlySpeedHorizontalLimit(value);
        WYPNetwork.syncTo(target);
        source.sendSuccess(() -> Component.translatable("commands.wieldyourpower.set.flyH", fmt(value)), false);
        return 1;
    }

    private static int setFlyV(CommandSourceStack source, ServerPlayer target, double value) {
        IPlayerLimits limits = ModCapabilities.resolve(target);
        if (limits == null) {
            return 0;
        }
        limits.setFlySpeedVerticalLimit(value);
        WYPNetwork.syncTo(target);
        source.sendSuccess(() -> Component.translatable("commands.wieldyourpower.set.flyV", fmt(value)), false);
        return 1;
    }

    private static int setMineSpeed(CommandSourceStack source, ServerPlayer target, int value) {
        IPlayerLimits limits = ModCapabilities.resolve(target);
        if (limits == null) {
            return 0;
        }
        limits.setMineSpeedLimit(value);
        WYPNetwork.syncTo(target);
        source.sendSuccess(() -> Component.translatable("commands.wieldyourpower.set.mineSpeed", value), false);
        return 1;
    }

    private static int setJump(CommandSourceStack source, ServerPlayer target, double value) {
        IPlayerLimits limits = ModCapabilities.resolve(target);
        if (limits == null) {
            return 0;
        }
        limits.setJumpLimit(value);
        WYPNetwork.syncTo(target);
        source.sendSuccess(() -> Component.translatable("commands.wieldyourpower.set.jump", fmt(value)), false);
        return 1;
    }

    private static int setStep(CommandSourceStack source, ServerPlayer target, double value) {
        IPlayerLimits limits = ModCapabilities.resolve(target);
        if (limits == null) {
            return 0;
        }
        limits.setStepLimit(value);
        WYPNetwork.syncTo(target);
        source.sendSuccess(() -> Component.translatable("commands.wieldyourpower.set.step", fmt(value)), false);
        return 1;
    }

    private static int setAttribute(CommandSourceStack source, ServerPlayer target, String id, double value) {
        IPlayerLimits limits = ModCapabilities.resolve(target);
        if (limits == null) {
            return 0;
        }
        java.util.List<String> updated = new java.util.ArrayList<>();
        String prefix = "attribute:" + id + ":";
        for (String entry : limits.getAttributeLimits()) {
            if (entry == null || !entry.startsWith(prefix)) {
                updated.add(entry);
            }
        }
        if (value >= 0.0D) {
            updated.add(prefix + value);
        }
        limits.setAttributeLimits(updated);
        WYPNetwork.syncTo(target);
        source.sendSuccess(() -> Component.translatable("commands.wieldyourpower.set.attribute", id, fmt(value)), false);
        return 1;
    }

    private static int setMineInterval(CommandSourceStack source, ServerPlayer target, int value) {
        IPlayerLimits limits = ModCapabilities.resolve(target);
        if (limits == null) {
            return 0;
        }
        limits.setMineInterval(value);
        WYPNetwork.syncTo(target);
        source.sendSuccess(() -> Component.translatable("commands.wieldyourpower.set.mineInterval", value), false);
        return 1;
    }

    private static String fmt(double value) {
        if (value < 0) {
            return "off";
        }
        return String.format("%.3f", value);
    }
}
