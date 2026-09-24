package net.wieldyourpower.common;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wieldyourpower.WYPConfig;
import net.wieldyourpower.WieldYourPower;
import net.wieldyourpower.util.ForceBlockBreak;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lets creative players break blocks that nothing else can break.
 *
 * <p>Respects this mod's own mining self-limits (any active {@code mineSpeed} value or an active
 * {@code mineInterval} window disables the bypass) but ignores every other protection.</p>
 *
 * <p>It uses a "confirm after one tick" check instead of looking at break progress: on a creative
 * left-click it remembers the target, and one tick later force-breaks only if the block is still there
 * and unchanged. Vanilla and multi-block breakers (vein miners) break the clicked block first, so they
 * are left alone; blocks that nothing can break are force-removed.</p>
 */
@Mod.EventBusSubscriber(modid = WieldYourPower.MODID)
public final class BlockProtectionEvents {

    private static final int CONFIRM_DELAY_TICKS = 1;
    private static final Map<BlockPos, Pending> PENDING = new ConcurrentHashMap<>();

    private BlockProtectionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }
        Level level = event.getLevel();
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Player player = event.getEntity();
        if (!shouldBypass(player)) {
            return;
        }
        BlockState state = level.getBlockState(event.getPos());
        if (state.isAir()) {
            return;
        }
        BlockPos pos = event.getPos().immutable();
        PENDING.put(pos, new Pending(serverLevel, pos, state.getBlock(), player, serverLevel.getGameTime() + CONFIRM_DELAY_TICKS));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<BlockPos, Pending>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Pending> entry = iterator.next();
            Pending pending = entry.getValue();
            if (pending.level.getGameTime() < pending.dueTick) {
                continue;
            }
            iterator.remove();
            if (pending.player.level() != pending.level || !shouldBypass(pending.player)) {
                continue;
            }
            BlockState current = pending.level.getBlockState(pending.pos);
            if (current.isAir() || current.getBlock() != pending.block) {
                // Vanilla or a multi-block breaker already handled it.
                continue;
            }
            if (ForceBlockBreak.breakBlock(pending.level, pending.pos, pending.player, false)) {
                MiningEvents.noteBreak(pending.player);
            }
        }
    }

    private static boolean shouldBypass(Player player) {
        return WYPConfig.COMMON.creativeBreaksProtectedBlocks.get()
                && player != null
                && player.isCreative()
                && !MiningEvents.miningLimitActive(player);
    }

    private record Pending(ServerLevel level, BlockPos pos, Block block, Player player, long dueTick) {
    }
}
