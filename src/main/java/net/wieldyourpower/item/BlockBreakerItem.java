package net.wieldyourpower.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.wieldyourpower.WYPConfig;
import net.wieldyourpower.util.ForceBlockBreak;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Admin tool: right-click a block to force-remove it. It bypasses this mod's own self-limits as well as
 * every other protection, and it drops the block normally.
 */
public class BlockBreakerItem extends Item {

    public BlockBreakerItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!WYPConfig.COMMON.blockBreakerEnabled.get()) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (ForceBlockBreak.breakBlock(level, context.getClickedPos(), player, false)) {
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.wieldyourpower.block_breaker.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
