package net.wieldyourpower.common;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wieldyourpower.WYPConfig;
import net.wieldyourpower.WieldYourPower;
import net.wieldyourpower.util.KillUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hardened creative-mode defense. Damage is cancelled at every stage and each tick a protected creative
 * player is topped up: at least {@code creativeMinHealth} health, at least {@code creativeMinMaxHealth}
 * max health, full food/saturation, no harmful effects, no fire, full air, and death is undone in place
 * (the player is revived where they are, never sent to the world spawn).
 *
 * <p>It also pins the vanilla invulnerability-frame state ({@code invulnerableTime} / {@code lastHurt}),
 * so a hit that reaches the cooldown branch of {@code hurt()} is rejected there ("still in frames") and
 * never applies, instead of only being cancelled by the events.</p>
 *
 * <p>Only this mod's own kill bypasses it ({@link KillUtil#isForceKilling} during the kill and
 * {@link KillUtil#wasKilledByUs} afterwards). Every other source, including vanilla {@code /kill},
 * is blocked - so to be sacrificed in creative you must use {@code /wyp kill}.</p>
 *
 * <p>When a protected player leaves creative, the forced invulnerability and max-health boost are removed.</p>
 */
@Mod.EventBusSubscriber(modid = WieldYourPower.MODID)
public final class CreativeDefenseEvents {

    private static final UUID CREATIVE_MAX_HEALTH_ID =
            UUID.fromString("c7e1a5b9-4d2f-4a8b-9f31-6e0c2d4a0002");

    private static final Set<UUID> PROTECTED = ConcurrentHashMap.newKeySet();
    private static final java.util.Map<UUID, Double> BASE_MAX_HEALTH = new ConcurrentHashMap<>();

    private CreativeDefenseEvents() {
    }

    public static void forget(UUID uuid) {
        PROTECTED.remove(uuid);
        BASE_MAX_HEALTH.remove(uuid);
    }

    /**
     * Fast path for the mixins: true when at least one creative player is currently protected. Kept in
     * sync by {@code onPlayerTick} on both sides; when empty every hot mixin can bail out immediately.
     */
    public static boolean hasProtected() {
        return !PROTECTED.isEmpty();
    }

    /**
     * A creative player that should be treated as immortal right now. Safe to call on both sides, so the
     * {@code isDeadOrDying} mixin can also hide the death screen on the client.
     */
    public static boolean isProtectedCreative(Entity entity) {
        return entity instanceof Player player && hasProtected() && protectedCreative(player);
    }

    /**
     * Used by {@code ServerPlayerDieMixin} to stop a protected creative player from dying before the death
     * event is even fired, which is immune to weapons that make that event uncancellable.
     */
    public static boolean shouldBlockDeath(Player player) {
        return !player.level().isClientSide && isProtectedCreative(player);
    }

    /**
     * Used by the {@code actuallyHurt} and {@code dropAllDeathLoot} mixins. Some attacks bypass the damage
     * events entirely, so the protection also has to sit on the damage/loot application itself.
     */
    public static boolean shouldBlockDamage(LivingEntity entity) {
        return entity instanceof Player player && shouldBlockDeath(player);
    }

    /**
     * Used by the {@code Level.getEntities} mixins: should this protected creative player be hidden from
     * entity lookups (targeting / ray picks) right now, per {@code creativeHitboxMode}.
     */
    public static boolean shouldHideHitbox(Entity entity) {
        if (!(entity instanceof Player player) || !isProtectedCreative(player)) {
            return false;
        }
        boolean sneakGround = player.onGround() && player.isShiftKeyDown();
        switch (WYPConfig.COMMON.creativeHitboxMode.get()) {
            case REMOVE_UNLESS_SNEAK_GROUND:
                return !sneakGround;
            case HIDE_ONLY_SNEAK_GROUND:
                return sneakGround;
            case ALWAYS_HIDE:
                return true;
            case NEVER_HIDE:
            default:
                return false;
        }
    }

    private static boolean enabled() {
        return WYPConfig.COMMON.creativeDefense.get();
    }

    private static boolean protectedCreative(Player player) {
        // isCreative() first: it cheaply rejects almost every player tick before touching config / maps.
        return player.isCreative()
                && enabled()
                && !KillUtil.isForceKilling(player)
                && !KillUtil.wasKilledByUs(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide && protectedCreative(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide && protectedCreative(player)) {
            event.setCanceled(true);
            restore(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide && protectedCreative(player)) {
            event.setCanceled(true);
            restore(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player && protectedCreative(player)) {
            event.setCanceled(true);
            restore(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        boolean protectedNow = protectedCreative(player);
        if (protectedNow) {
            PROTECTED.add(player.getUUID());
            if (!player.level().isClientSide) {
                restore(player);
                undoForcedRemoval(player);
            }
        } else if (PROTECTED.remove(player.getUUID()) && !player.level().isClientSide) {
            // The player left creative (or defense was disabled): undo what we forced.
            undoDefense(player);
        }
    }

    private static void undoDefense(Player player) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        player.setInvulnerable(false);
        if (player.getAbilities().invulnerable) {
            player.getAbilities().invulnerable = false;
            player.onUpdateAbilities();
        }
        AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null) {
            attribute.removeModifier(CREATIVE_MAX_HEALTH_ID);
            Double original = BASE_MAX_HEALTH.remove(player.getUUID());
            if (original != null && attribute.getBaseValue() < original) {
                attribute.setBaseValue(original);
            }
        }
    }

    private static void restore(Player player) {
        ensureMinMaxHealth(player, WYPConfig.COMMON.creativeMinMaxHealth.get());

        float maxHealth = player.getMaxHealth();
        float minHealth = WYPConfig.COMMON.creativeMinHealth.get().floatValue();
        float targetHealth = Math.min(Math.max(1.0F, Math.max(player.getHealth(), minHealth)), maxHealth);

        // Undo death in place: never moves the player, so they revive where they are.
        player.dead = false;
        player.deathTime = 0;
        if (player.getHealth() != targetHealth) {
            // setHealth already writes DATA_HEALTH_ID; skipping when unchanged avoids a sync every tick.
            player.setHealth(targetHealth);
        }

        player.setInvulnerable(true);
        if (!player.getAbilities().invulnerable) {
            player.getAbilities().invulnerable = true;
            player.onUpdateAbilities();
        }

        // Invulnerability-frame deception: keep the vanilla hurt() cooldown branch in "still in frames"
        // state so a hit that reaches it (e.g. one that pierces invulnerability but not the cooldown)
        // returns false without ever applying damage.
        player.invulnerableTime = 20;
        player.lastHurt = Float.MAX_VALUE;

        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
        clearHarmfulEffects(player);
        player.clearFire();
        player.setAirSupply(player.getMaxAirSupply());
    }

    /**
     * Protects the max-health <b>base value</b> itself (not just an additive modifier): a mod lowering the
     * base is reverted, and the base is also kept at least {@code minimum}.
     */
    private static void ensureMinMaxHealth(Player player, double minimum) {
        AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) {
            return;
        }
        AttributeModifier existing = attribute.getModifier(CREATIVE_MAX_HEALTH_ID);
        if (existing != null) {
            attribute.removeModifier(existing);
        }
        double original = BASE_MAX_HEALTH.computeIfAbsent(player.getUUID(), uuid -> attribute.getBaseValue());
        double target = Math.max(original, minimum);
        if (attribute.getBaseValue() < target) {
            attribute.setBaseValue(target);
        }
    }

    private static void clearHarmfulEffects(Player player) {
        if (player.getActiveEffects().isEmpty()) {
            return;
        }
        List<MobEffect> harmful = null;
        for (MobEffectInstance instance : player.getActiveEffects()) {
            if (instance.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                if (harmful == null) {
                    harmful = new ArrayList<>();
                }
                harmful.add(instance.getEffect());
            }
        }
        if (harmful != null) {
            for (MobEffect effect : harmful) {
                player.removeEffect(effect);
            }
        }
    }

    private static void undoForcedRemoval(Player player) {
        Entity.RemovalReason reason = player.getRemovalReason();
        if (reason == null || reason == Entity.RemovalReason.CHANGED_DIMENSION) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.hasDisconnected()) {
            return;
        }

        player.unsetRemoved();
        player.reviveCaps();
        player.isAddedToWorld = true;

        if (player instanceof ServerPlayer serverPlayer) {
            ServerLevel level = serverPlayer.serverLevel();
            if (!level.players().contains(serverPlayer)) {
                level.players().add(serverPlayer);
                level.updateSleepingPlayerList();
            }
        }
    }
}
