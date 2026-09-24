package net.wieldyourpower.common;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wieldyourpower.WYPConfig;
import net.wieldyourpower.WieldYourPower;
import net.wieldyourpower.util.ForceBlockBreak;

/**
 * Lets creative players break blocks that vanilla itself refuses to break (e.g. {@code strength(-1)}).
 *
 * <p>Respects this mod's own mining self-limits (any active {@code mineSpeed} value or an active
 * {@code mineInterval} window disables the bypass) but ignores every other protection.</p>
 *
 * <p>It only steps in when the break would fail, detected via {@code getDestroyProgress <= 0}. This
 * covers {@code strength(-1)} blocks and blocks whose destroy progress is overridden to a non-positive
 * value. Breaking every clicked block instead runs before multi-block breakers (vein miners and
 * similar) and swallows them.</p>
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
        BlockState state = level.getBlockState(event.getPos());
        if (state.isAir() || state.getDestroyProgress(player, level, event.getPos()) > 0.0F) {
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
