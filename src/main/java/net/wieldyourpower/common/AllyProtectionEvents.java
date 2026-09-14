package net.wieldyourpower.common;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wieldyourpower.WieldYourPower;
import net.wieldyourpower.capability.IPlayerLimits;
import net.wieldyourpower.capability.ModCapabilities;
import net.wieldyourpower.util.EntityMatcher;
import net.wieldyourpower.util.KillUtil;

/**
 * Per-player ally protection: entities listed in any online player's ally list are immune to damage
 * caused by players - but only to player damage. Mobs, the environment and this mod's own
 * {@code /wyp kill} can still affect them.
 */
@Mod.EventBusSubscriber(modid = WieldYourPower.MODID)
public final class AllyProtectionEvents {

    private AllyProtectionEvents() {
    }

    private static boolean isProtectedAlly(Entity entity) {
        MinecraftServer server = entity.getServer();
        if (server == null) {
            return false;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            IPlayerLimits limits = ModCapabilities.resolve(player);
            if (limits != null && EntityMatcher.matches(limits.getAllyProtection(), entity)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A damage should be blocked only when it comes from a player and the victim is a protected ally.
     * Our own kill sets the force-killing flag, which bypasses this.
     */
    private static boolean shouldBlock(Entity victim, net.minecraft.world.damagesource.DamageSource source) {
        if (KillUtil.isForceKilling(victim)) {
            return false;
        }
        if (!(source.getEntity() instanceof Player)) {
            return false;
        }
        return isProtectedAlly(victim);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(LivingAttackEvent event) {
        if (!event.getEntity().level().isClientSide && shouldBlock(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onHurt(LivingHurtEvent event) {
        if (!event.getEntity().level().isClientSide && shouldBlock(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onDamage(LivingDamageEvent event) {
        if (!event.getEntity().level().isClientSide && shouldBlock(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide && shouldBlock(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
        }
    }
}
