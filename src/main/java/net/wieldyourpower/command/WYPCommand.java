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
import net.wieldyourpower.common.NoUpdateEvents;
import net.wieldyourpower.network.PacketOpenPanel;
import net.wieldyourpower.network.WYPNetwork;
import net.wieldyourpower.util.FrozenEntities;

import java.util.Collection;

/**
 * The {@code /wyp} tree. It is deliberately small: every self-limit (walk/fly/mine speed, jump, step,
 * attributes, reset) is edited in the Cloth panel ({@code /wyp panel}) and has no command, and the
 * kill-honor variant is the trailing {@code honor} literal of {@code /wyp kill} instead of its own
 * command. {@code /wyp status} stays because it can also report <i>another</i> player's limits, which
 * the self-limits panel cannot.
 */
public final class WYPCommand {

    private WYPCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("wyp")
                .then(Commands.literal("panel")
                        .executes(ctx -> openPanel(ctx.getSource(), PacketOpenPanel.LIMITS)))
                .then(Commands.literal("config")
                        .executes(ctx -> openPanel(ctx.getSource(), PacketOpenPanel.CONFIG)))
                .then(Commands.literal("kill")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> KillCommand.execute(ctx.getSource(),
                                java.util.List.of(ctx.getSource().getEntityOrException())))
                        .then(Commands.literal("honor")
                                .executes(ctx -> KillCommand.executeHonor(ctx.getSource(),
                                        java.util.List.of(ctx.getSource().getEntityOrException())))
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .executes(ctx -> KillCommand.executeHonor(ctx.getSource(),
                                                EntityArgument.getEntities(ctx, "targets")))))
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(ctx -> KillCommand.execute(ctx.getSource(),
                                        EntityArgument.getEntities(ctx, "targets")))
                                .then(Commands.literal("honor")
                                        .executes(ctx -> KillCommand.executeHonor(ctx.getSource(),
                                                EntityArgument.getEntities(ctx, "targets"))))))
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
                .then(Commands.literal("access")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("grant")
                                .then(Commands.literal("noupdate")
                                        .executes(ctx -> noUpdateGrant(ctx.getSource(),
                                                java.util.List.of(ctx.getSource().getPlayerOrException()), true))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> noUpdateGrant(ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "targets"), true)))))
                        .then(Commands.literal("revoke")
                                .then(Commands.literal("noupdate")
                                        .executes(ctx -> noUpdateGrant(ctx.getSource(),
                                                java.util.List.of(ctx.getSource().getPlayerOrException()), false))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> noUpdateGrant(ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "targets"), false))))))
                .then(Commands.literal("status")
                        .executes(ctx -> status(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> status(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))));
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
        String tag = net.wieldyourpower.common.AuthorsFavorEvents.primaryTag();
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

    private static int noUpdateGrant(CommandSourceStack source, Collection<ServerPlayer> targets, boolean allowed) {
        int count = 0;
        for (ServerPlayer target : targets) {
            if (NoUpdateEvents.applyGrant(target, allowed)) {
                count++;
            }
        }
        if (count == 0) {
            source.sendFailure(Component.translatable(allowed
                    ? "commands.wieldyourpower.grant.noupdate.none"
                    : "commands.wieldyourpower.revoke.noupdate.none"));
            return 0;
        }
        final int changed = count;
        source.sendSuccess(() -> Component.translatable(allowed
                ? "commands.wieldyourpower.grant.noupdate.success"
                : "commands.wieldyourpower.revoke.noupdate.success", changed), true);
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
        source.sendSuccess(() -> Component.translatable("commands.wieldyourpower.status.noUpdate",
                Component.translatable(limits.isNoUpdateGranted()
                        ? "commands.wieldyourpower.status.noUpdate.granted"
                        : "commands.wieldyourpower.status.noUpdate.none")), false);
        return 1;
    }

    private static String fmt(double value) {
        if (value < 0) {
            return "off";
        }
        return String.format("%.3f", value);
    }
}
