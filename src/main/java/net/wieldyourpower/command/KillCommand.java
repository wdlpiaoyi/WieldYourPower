package net.wieldyourpower.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.wieldyourpower.util.KillUtil;

import java.util.Collection;

/**
 * The enhanced (escalating, protection-piercing) kill. Registered as {@code /wyp kill}; vanilla
 * {@code /kill} is intentionally left untouched.
 */
public final class KillCommand {

    private KillCommand() {
    }

    public static int execute(CommandSourceStack source, Collection<? extends Entity> targets) {
        return run(source, targets, false);
    }

    /**
     * Honor variant: matching targets only get the normal death attempt, never a forced removal.
     */
    public static int executeHonor(CommandSourceStack source, Collection<? extends Entity> targets) {
        return run(source, targets, true);
    }

    private static int run(CommandSourceStack source, Collection<? extends Entity> targets, boolean honor) {
        int count = 0;
        for (Entity entity : targets) {
            boolean killed = honor ? KillUtil.honorKill(entity) : KillUtil.forceKill(entity);
            if (killed) {
                count++;
            }
        }

        final int killed = count;
        if (killed == 0) {
            source.sendFailure(Component.translatable("commands.wieldyourpower.kill.failure"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.wieldyourpower.kill.success", killed), true);
        return killed;
    }
}
