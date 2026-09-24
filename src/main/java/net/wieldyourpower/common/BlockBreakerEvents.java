package net.wieldyourpower.common;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wieldyourpower.WYPConfig;
import net.wieldyourpower.WieldYourPower;
import net.wieldyourpower.item.ModItems;
import net.wieldyourpower.util.ForceBlockBreak;

/**
 * Makes the admin Block Breaker win the right-click: it force-removes the targeted block and cancels the
 * interaction, so aiming at a protected block never turns into the block's own use (altar interaction,
 * container GUI, placement, ...).
 */
@Mod.EventBusSubscriber(modid = WieldYourPower.MODID)
public final class BlockBreakerEvents {

    private BlockBreakerEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || !WYPConfig.COMMON.blockBreakerEnabled.get()) {
            return;
        }
        Player player = event.getEntity();
        if (player == null || !holdingBreaker(player, event.getHand())) {
            return;
        }
        if (ForceBlockBreak.breakBlock(event.getLevel(), event.getPos(), player, false)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static boolean holdingBreaker(Player player, InteractionHand hand) {
        ItemStack used = player.getItemInHand(hand);
        if (used.is(ModItems.BLOCK_BREAKER.get())) {
            return true;
        }
        if (used.isEmpty()) {
            InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            return player.getItemInHand(other).is(ModItems.BLOCK_BREAKER.get());
        }
        return false;
    }
}
