package net.wieldyourpower.common;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wieldyourpower.WYPConfig;
import net.wieldyourpower.WieldYourPower;
import net.wieldyourpower.util.ForceBlockBreak;

/**
 * Lets creative players break blocks that other mods protect or forbid.
 *
 * <p>Respects this mod's own mining self-limits (any active {@code mineSpeed} value or an active
 * {@code mineInterval} window disables the bypass) but ignores every other protection.</p>
 *
 * <p>Creative mining is instant, so this does not wait for the vanilla break to succeed or for a
 * protection to cancel the click: on a creative left-click it simply checks that the targeted block is
 * still there and force-removes it. A protection that only cancels on the client - or blocks the block
 * change at the {@code Level} level - is handled either way.</p>
 */
@Mod.EventBusSubscriber(modid = WieldYourPower.MODID)
public final class BlockProtectionEvents {

    private BlockProtectionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide) {
            return;
        }
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }
        Player player = event.getEntity();
        if (!shouldBypass(player)) {
            return;
        }
        if (level.getBlockState(event.getPos()).isAir()) {
            return;
        }
        if (ForceBlockBreak.breakBlock(level, event.getPos(), player, false)) {
            MiningEvents.noteBreak(player);
        }
    }

    private static boolean shouldBypass(Player player) {
        return WYPConfig.COMMON.creativeBreaksProtectedBlocks.get()
                && player != null
                && player.isCreative()
                && !MiningEvents.miningLimitActive(player);
    }
}
