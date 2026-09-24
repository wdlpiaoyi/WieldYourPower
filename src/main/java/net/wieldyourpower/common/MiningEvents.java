package net.wieldyourpower.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wieldyourpower.WieldYourPower;
import net.wieldyourpower.capability.IPlayerLimits;
import net.wieldyourpower.capability.ModCapabilities;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = WieldYourPower.MODID)
public final class MiningEvents {

    private static final Map<UUID, Long> LAST_BREAK = new HashMap<>();

    private MiningEvents() {
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        IPlayerLimits limits = ModCapabilities.resolve(player);
        if (limits == null) {
            return;
        }

        int mineSpeed = limits.getMineSpeedLimit();
        if (mineSpeed < 0) {
            event.setNewSpeed(0.0F);
            return;
        }
        if (mineSpeed == 0) {
            return;
        }

        BlockPos pos = event.getPosition().orElse(player.blockPosition());
        float hardness = event.getState().getDestroySpeed(player.level(), pos);
        if (hardness <= 0.0F) {
            return;
        }

        float allowed = hardness * 30.0F / (float) mineSpeed;
        if (event.getNewSpeed() > allowed) {
            event.setNewSpeed(allowed);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        IPlayerLimits limits = ModCapabilities.resolve(player);
        if (limits == null) {
            return;
        }

        if (limits.getMineSpeedLimit() < 0) {
            event.setCanceled(true);
            return;
        }

        int interval = limits.getMineInterval();
        if (interval <= 0) {
            return;
        }

        long now = player.level().getGameTime();
        UUID id = player.getUUID();
        Long last = LAST_BREAK.get(id);
        if (last != null && now - last < interval) {
            event.setCanceled(true);
            return;
        }
        noteBreak(player);
    }

    /**
     * True while the player's own mining limits are in effect: a non-default {@code mineSpeed} (mining
     * forbidden when negative, a speed cap when positive) or a {@code mineInterval} window that has not
     * elapsed yet. Other features use this to defer to the player's self-imposed limits.
     */
    public static boolean miningLimitActive(Player player) {
        IPlayerLimits limits = ModCapabilities.resolve(player);
        if (limits == null) {
            return false;
        }
        if (limits.getMineSpeedLimit() != 0) {
            return true;
        }
        int interval = limits.getMineInterval();
        if (interval <= 0) {
            return false;
        }
        Long last = LAST_BREAK.get(player.getUUID());
        return last != null && player.level().getGameTime() - last < interval;
    }

    public static void noteBreak(Player player) {
        LAST_BREAK.put(player.getUUID(), player.level().getGameTime());
    }
}
