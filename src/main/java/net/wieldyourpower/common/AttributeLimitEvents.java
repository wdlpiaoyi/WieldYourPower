package net.wieldyourpower.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.wieldyourpower.WieldYourPower;
import net.wieldyourpower.capability.IPlayerLimits;
import net.wieldyourpower.capability.ModCapabilities;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side attribute caps. {@code stepLimit} caps {@code forge:step_height_addition} as a coefficient
 * of the vanilla 0.6, and custom entries {@code attribute,<id>,<max>} cap any attribute. The cap is applied
 * with a transient (never saved) modifier, so the underlying value is never edited.
 */
@Mod.EventBusSubscriber(modid = WieldYourPower.MODID)
public final class AttributeLimitEvents {

    private static final UUID STEP_MODIFIER_ID =
            UUID.fromString("b41d9e77-2c53-4a10-8f6d-7a2e5c9b0003");
    private static final double VANILLA_STEP = 0.6D;

    private static final Map<UUID, Map<UUID, Attribute>> APPLIED = new ConcurrentHashMap<>();

    private AttributeLimitEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide) {
            return;
        }
        IPlayerLimits limits = ModCapabilities.resolve(player);
        if (limits == null) {
            return;
        }

        Map<UUID, Attribute> applied = APPLIED.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>());
        // Fast path: nothing configured and nothing left over.
        if (limits.getStepLimit() < 0.0D && limits.getAttributeLimits().isEmpty() && applied.isEmpty()) {
            return;
        }
        Set<UUID> active = new HashSet<>();

        double step = limits.getStepLimit();
        if (step >= 0.0D) {
            Attribute stepHeight = ForgeMod.STEP_HEIGHT_ADDITION.get();
            clampStep(player, stepHeight, step * VANILLA_STEP, STEP_MODIFIER_ID);
            applied.put(STEP_MODIFIER_ID, stepHeight);
            active.add(STEP_MODIFIER_ID);
        }

        for (String entry : limits.getAttributeLimits()) {
            double[] maxHolder = new double[1];
            Attribute attribute = parse(entry, maxHolder);
            if (attribute == null) {
                continue;
            }
            UUID id = modifierId(attribute);
            clamp(player, attribute, maxHolder[0], id);
            applied.put(id, attribute);
            active.add(id);
        }

        // Remove caps that are no longer configured.
        for (UUID id : new HashSet<>(applied.keySet())) {
            if (!active.contains(id)) {
                Attribute attribute = applied.remove(id);
                AttributeInstance instance = attribute == null ? null : player.getAttribute(attribute);
                if (instance != null) {
                    instance.removeModifier(id);
                }
            }
        }
    }

    /**
     * {@code forge:step_height_addition} defaults to 0; the vanilla 0.6 lives in {@code maxUpStep}, so the
     * cap has to be computed against {@code 0.6 + attribute} (that is the actual step height).
     */
    private static void clampStep(Player player, Attribute attribute, double maxTotal, UUID id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        double ourAmount = existing != null ? existing.getAmount() : 0.0D;
        double underlying = instance.getValue() - ourAmount;
        double total = VANILLA_STEP + underlying;
        double desired = -Math.max(0.0D, total - maxTotal);
        if (Math.abs(ourAmount - desired) > 1.0E-4D) {
            if (existing != null) {
                instance.removeModifier(existing);
            }
            if (desired < 0.0D) {
                instance.addTransientModifier(new AttributeModifier(
                        id, "wieldyourpower_limit", desired, AttributeModifier.Operation.ADDITION));
            }
        }
    }

    private static void clamp(Player player, Attribute attribute, double max, UUID id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        double ourAmount = existing != null ? existing.getAmount() : 0.0D;
        double underlying = instance.getValue() - ourAmount;
        double desired = -Math.max(0.0D, underlying - max);
        if (Math.abs(ourAmount - desired) > 1.0E-4D) {
            if (existing != null) {
                instance.removeModifier(existing);
            }
            if (desired < 0.0D) {
                instance.addTransientModifier(new AttributeModifier(
                        id, "wieldyourpower_limit", desired, AttributeModifier.Operation.ADDITION));
            }
        }
    }

    private static Attribute parse(String entry, double[] maxHolder) {
        if (entry == null) {
            return null;
        }
        // attribute,<attribute id>,<max>  (a ':' inside the id is kept)
        String[] parts = entry.trim().split(",", 3);
        if (parts.length < 3 || !parts[0].trim().equalsIgnoreCase("attribute")) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(parts[1].trim());
        if (id == null) {
            return null;
        }
        try {
            maxHolder[0] = Double.parseDouble(parts[2].trim());
        } catch (RuntimeException exception) {
            return null;
        }
        return ForgeRegistries.ATTRIBUTES.getValue(id);
    }

    private static UUID modifierId(Attribute attribute) {
        ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        return UUID.nameUUIDFromBytes(("wieldyourpower:" + id).getBytes(StandardCharsets.UTF_8));
    }
}
