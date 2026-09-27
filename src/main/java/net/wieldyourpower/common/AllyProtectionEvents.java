package net.wieldyourpower.common;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wieldyourpower.WYPConfig;
import net.wieldyourpower.WieldYourPower;
import net.wieldyourpower.capability.IPlayerLimits;
import net.wieldyourpower.capability.ModCapabilities;
import net.wieldyourpower.compat.OwnerResolver;
import net.wieldyourpower.util.EntityMatcher;
import net.wieldyourpower.util.KillUtil;

import java.util.UUID;

/**
 * Per-player ally protection. Damage caused by a player (or by a pet/summon/projectile that player owns)
 * to an entity on <b>that player's own</b> ally list is blocked, and a player also cannot damage their own
 * pets/summons (ownership also covers pets/summons damaging each other). Only player-caused damage is
 * touched: mobs, the environment and this mod's own {@code /wyp kill} can still affect them.
 */
@Mod.EventBusSubscriber(modid = WieldYourPower.MODID)
public final class AllyProtectionEvents {

    private AllyProtectionEvents() {
    }

    /**
     * Blocks when the responsible player (the attacker, or the owner behind a pet/summon/projectile)
     * either lists the victim or owns it. Only that player's own list applies. Self-harm is allowed unless
     * {@code allyBlocksSelfHarm} is on; our own kill bypasses everything.
     */
    private static boolean shouldBlock(Entity victim, DamageSource source) {
        if (KillUtil.isForceKilling(victim)) {
            return false;
        }
        Entity attacker = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
        if (attacker == null) {
            return false;
        }
        if (attacker == victim && !WYPConfig.COMMON.allyBlocksSelfHarm.get()) {
            return false;
        }
        UUID attackerOwner = OwnerResolver.ownerUUID(attacker);
        if (attackerOwner == null) {
            return false;
        }
        MinecraftServer server = victim.getServer();
        if (server != null) {
            ServerPlayer owner = server.getPlayerList().getPlayer(attackerOwner);
            if (owner != null) {
                IPlayerLimits limits = ModCapabilities.resolve(owner);
                if (limits != null && EntityMatcher.matches(limits.getAllyProtection(), victim)) {
                    return true;
                }
            }
        }
        if (WYPConfig.COMMON.allyProtectsOwned.get()) {
            UUID victimOwner = OwnerResolver.ownerUUID(victim);
            if (victimOwner != null && victimOwner.equals(attackerOwner)) {
                return true;
            }
        }
        return false;
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
