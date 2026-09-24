package net.wieldyourpower.common;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wieldyourpower.WYPConfig;
import net.wieldyourpower.WieldYourPower;

/**
 * Lets creative players place blocks that other mods forbid placing, mirroring the creative break
 * bypass.
 *
 * <p>Only the placement intent is affected (a {@link BlockItem} in hand), so container/use
 * interactions are left alone. It works on both sides: a mod that cancels the client-side event would
 * otherwise stop the packet before the server ever sees it. Mods that deny placement without firing
 * either event (region checks, custom items) are not covered.</p>
 */
@Mod.EventBusSubscriber(modid = WieldYourPower.MODID)
public final class BlockPlacementEvents {

    private BlockPlacementEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!WYPConfig.COMMON.creativePlacesBlocks.get() || !isCreativeWithBlock(event.getEntity())) {
            return;
        }
        event.setCanceled(false);
        if (event.getUseBlock() == Event.Result.DENY) {
            event.setUseBlock(Event.Result.DEFAULT);
        }
        if (event.getUseItem() == Event.Result.DENY) {
            event.setUseItem(Event.Result.DEFAULT);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        if (!WYPConfig.COMMON.creativePlacesBlocks.get() || !event.isCanceled()) {
            return;
        }
        if (event.getEntity() instanceof Player player && player.isCreative()) {
            event.setCanceled(false);
        }
    }

    private static boolean isCreativeWithBlock(Player player) {
        return player != null && player.isCreative()
                && (player.getMainHandItem().getItem() instanceof BlockItem
                || player.getOffhandItem().getItem() instanceof BlockItem);
    }
}
