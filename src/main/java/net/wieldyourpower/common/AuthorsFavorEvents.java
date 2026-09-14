package net.wieldyourpower.common;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wieldyourpower.WYPConfig;
import net.wieldyourpower.WieldYourPower;
import net.wieldyourpower.effect.ModEffects;
import net.wieldyourpower.util.KillUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Author's favor" tag protection. Pack authors put the configured scoreboard tag (default
 * {@code authorsfavor}) on a single non-player entity (e.g. via KubeJS or {@code /wyp favor add})
 * so endgame players cannot delete it with overpowered code.
 *
 * <p>Only calls that bypass the vanilla damage chain are touched. Everything that goes through
 * {@code hurt}/{@code actuallyHurt} (ordinary weapons, mob attacks, OP weapon damage) is left alone.
 * A direct code call such as {@code setHealth(X)} with {@code X} below the current health is rewritten to
 * {@code floor((current - X) * coefficient)} by {@code LivingEntitySetHealthMixin}; a direct {@code die()}
 * with no damage event is cancelled and restored the same way. The coefficient is clamped to 0 - 1:
 * {@code 0} means no protection, {@code 1} absorbs the drop entirely.</p>
 *
 * <p>While the tag is present on a non-player entity it also:</p>
 * <ul>
 *   <li>undoes a direct removal ({@code discard}) each tick;</li>
 *   <li>softens max-health cuts against the entity's historical max health, scaled by
 *       {@code maxHealthChangeCoefficient} and floored at {@code maxHealthCoefficient};</li>
 *   <li>keeps a no-op effect applied as a visual hint.</li>
 * </ul>
 *
 * <p>All coefficients default to "no effect"; this feature is opt-in per entity for pack authors.
 * This mod's own {@code /wyp kill} always bypasses it (the force-killing flag is the backdoor).</p>
 */
@Mod.EventBusSubscriber(modid = WieldYourPower.MODID)
public final class AuthorsFavorEvents {

    private static final UUID MAX_HEALTH_MODIFIER_ID =
            UUID.fromString("a17f0c4e-2b7d-4f1a-9c6e-3d5b8a0e0001");
    private static final int EFFECT_DURATION = 60;

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private AuthorsFavorEvents() {
    }

    private static float coefficient() {
        return WYPConfig.COMMON.authorsFavorDamageCoefficient.get().floatValue();
    }

    public static boolean isFavored(Entity entity) {
        if (!(entity instanceof LivingEntity) || entity instanceof Player) {
            return false;
        }
        // Cheapest discriminator first: the scoreboard tag. Almost every entity fails here.
        String tag = WYPConfig.COMMON.authorsFavorTag.get();
        if (tag.isEmpty() || !entity.getTags().contains(tag)) {
            return false;
        }
        return WYPConfig.COMMON.authorsFavorEnabled.get() && !KillUtil.isForceKilling(entity);
    }

    /**
     * Fast path for the mixins: true while at least one favored entity is being tracked.
     */
    public static boolean hasFavored() {
        return !STATES.isEmpty();
    }

    private static State state(LivingEntity entity) {
        return STATES.computeIfAbsent(entity.getUUID(), uuid -> new State(entity.getHealth(), entity.getMaxHealth()));
    }

    /**
     * Used by the removal-resistance mixins: a favored entity cannot be removed through KILLED/DISCARDED
     * (e.g. MoreAvaritia's InfinityGodSword forced removal). The tick undo stays as a fallback.
     */
    public static boolean shouldResistRemoval(Entity entity, Entity.RemovalReason reason) {
        return (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED)
                && isFavored(entity);
    }

    /**
     * Called from {@code LivingEntitySetHealthMixin} for every {@code setHealth} call. Returns the value
     * that should actually be written to the entity.
     */
    public static float mitigateSetHealth(LivingEntity entity, float requested) {
        try {
            if (entity == null || entity.level() == null || entity.level().isClientSide) {
                return requested;
            }
            // Protected creative players must never be written below 1, whatever shoves -Infinity in
            // (e.g. TrialMonolith's onSoulDeath). Their per-tick restore then tops the value back up.
            if (CreativeDefenseEvents.isProtectedCreative(entity)) {
                float maximum = entity.getMaxHealth();
                float value = Float.isFinite(requested) ? requested : 1.0F;
                return Math.max(1.0F, Math.min(value, maximum));
            }
            if (!isFavored(entity)) {
                return requested;
            }
            if (!Float.isFinite(requested) || requested < 0.0F) {
                // A negative setHealth is the same as setting 0 (the drop can never exceed the current
                // health), so treat it as 0 for the change calculation. -Infinity included.
                requested = 0.0F;
            }
            // Skip constructor/load-time writes (before the entity is part of the level) so saved health
            // is never rewritten on load.
            if (!entity.isAddedToWorld) {
                return requested;
            }
            float coefficient = coefficient();
            if (coefficient <= 0.0F) {
                return requested;
            }
            float before = entity.getHealth();
            if (requested >= before) {
                // Healing or raising the cap.
                return requested;
            }
            if (requested >= entity.getMaxHealth()) {
                // A clamp to a recently cut max health: leave it untouched.
                return requested;
            }
            State state = state(entity);
            if (state.damagePending) {
                // This write comes from the vanilla damage chain: leave it untouched.
                state.damagePending = false;
                return requested;
            }
            return mitigate(before, requested, coefficient);
        } catch (Throwable throwable) {
            return requested;
        }
    }

    /**
     * The drop amount ({@code before - requested}) is multiplied by the coefficient and floored.
     */
    private static float mitigate(float before, float requested, float coefficient) {
        float change = before - requested;
        if (change <= 0.0F) {
            return requested;
        }
        return (float) Math.floor(change * coefficient);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !isFavored(entity)) {
            return;
        }
        state(entity).recentHurt = true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !isFavored(entity)) {
            return;
        }
        // The following setHealth call is the vanilla chain applying this damage.
        state(entity).damagePending = true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !isFavored(entity)) {
            return;
        }
        float coefficient = coefficient();
        if (coefficient <= 0.0F) {
            return;
        }
        State state = state(entity);
        if (state.recentHurt) {
            // A death from the vanilla damage chain: leave it alone.
            state.deathAllowed = true;
            return;
        }
        // A direct die() with no damage event: treat it as a code kill.
        float maxHealth = entity.getMaxHealth();
        float before = state.lastHealth > 0.0F ? state.lastHealth : maxHealth;
        float newHealth = mitigate(before, 0.0F, coefficient);
        if (newHealth <= 0.0F) {
            state.deathAllowed = true;
            return;
        }
        event.setCanceled(true);
        entity.dead = false;
        entity.deathTime = 0;
        entity.setHealth(Math.min(newHealth, maxHealth));
        state.recentHurt = false;
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        if (!isFavored(entity)) {
            if (!STATES.isEmpty()) {
                STATES.remove(entity.getUUID());
            }
            return;
        }

        State state = state(entity);

        if (state.deathAllowed) {
            if (entity.dead || entity.getHealth() <= 0.0F) {
                STATES.remove(entity.getUUID());
                return;
            }
            state.deathAllowed = false;
        }

        // Undo a forced world removal (resist discard-style kills).
        Entity.RemovalReason reason = entity.getRemovalReason();
        if (reason != null && reason != Entity.RemovalReason.CHANGED_DIMENSION) {
            entity.unsetRemoved();
            entity.reviveCaps();
            entity.isAddedToWorld = true;
        }

        entity.addEffect(new MobEffectInstance(ModEffects.AUTHORS_FAVOR.get(), EFFECT_DURATION, 0, false, false, true));

        softenMaxHealthCut(entity, state);

        // Undo a direct health write that bypassed setHealth (InfinityUtils.forceSetHealth writes the
        // synced DATA_HEALTH_ID itself, then drops loot). Only while protection is actually enabled.
        if (coefficient() > 0.0F && !state.deathAllowed && entity.getHealth() <= 0.0F) {
            float maxHealth = entity.getMaxHealth();
            float before = state.lastHealth > 0.0F ? state.lastHealth : maxHealth;
            float restored = Math.max(1.0F, mitigate(before, 0.0F, coefficient()));
            entity.dead = false;
            entity.deathTime = 0;
            entity.setHealth(Math.min(restored, maxHealth));
        }

        // Undo MoreAvaritia-style forced deletion: it teleports the target to (-999,-999,-999) and
        // disables gravity; snap the entity back and re-enable gravity.
        if (state.hasSnapshot) {
            boolean absurd = entity.getY() < -500.0D
                    || Math.abs(entity.getX()) > 1.0E6D || Math.abs(entity.getZ()) > 1.0E6D;
            if (absurd) {
                entity.moveTo(state.lastX, state.lastY, state.lastZ, entity.getYRot(), entity.getXRot());
            }
            if (!state.lastNoGravity && entity.isNoGravity()) {
                entity.setNoGravity(false);
            }
        }
        state.lastX = entity.getX();
        state.lastY = entity.getY();
        state.lastZ = entity.getZ();
        state.lastNoGravity = entity.isNoGravity();
        state.hasSnapshot = true;

        state.lastHealth = entity.getHealth();
        state.recentHurt = false;
        state.damagePending = false;
    }

    /**
     * Softens max-health cuts against the entity's historical maximum. The change amount is scaled by
     * {@code maxHealthChangeCoefficient} and can never push the maximum below
     * {@code historicalMax * maxHealthCoefficient}.
     */
    private static void softenMaxHealthCut(LivingEntity entity, State state) {
        AttributeInstance attribute = entity.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) {
            return;
        }
        float floorCoefficient = WYPConfig.COMMON.authorsFavorMaxHealthCoefficient.get().floatValue();
        float changeCoefficient = WYPConfig.COMMON.authorsFavorMaxHealthChangeCoefficient.get().floatValue();
        AttributeModifier existing = attribute.getModifier(MAX_HEALTH_MODIFIER_ID);
        float ourAmount = existing != null ? (float) existing.getAmount() : 0.0F;
        float underlying = entity.getMaxHealth() - ourAmount;
        state.baselineMax = Math.max(state.baselineMax, underlying);
        float cutFromHistorical = state.baselineMax - underlying;
        float targetFromChange = state.baselineMax - cutFromHistorical * changeCoefficient;
        float floor = state.baselineMax * floorCoefficient;
        float target = Math.max(targetFromChange, floor);
        float desired = Math.max(0.0F, target - underlying);
        if (Math.abs(ourAmount - desired) > 0.01F) {
            if (existing != null) {
                attribute.removeModifier(existing);
            }
            if (desired > 0.0F) {
                attribute.addTransientModifier(new AttributeModifier(
                        MAX_HEALTH_MODIFIER_ID, "wieldyourpower_authorsfavor", desired,
                        AttributeModifier.Operation.ADDITION));
            }
        }
    }

    private static final class State {
        private float lastHealth;
        private float baselineMax;
        private boolean recentHurt;
        private boolean deathAllowed;
        private boolean damagePending;
        private boolean hasSnapshot;
        private double lastX;
        private double lastY;
        private double lastZ;
        private boolean lastNoGravity;

        private State(float lastHealth, float baselineMax) {
            this.lastHealth = lastHealth;
            this.baselineMax = baselineMax;
        }
    }
}
