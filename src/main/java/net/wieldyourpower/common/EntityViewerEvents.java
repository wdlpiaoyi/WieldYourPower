package net.wieldyourpower.common;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.wieldyourpower.WYPConfig;
import net.wieldyourpower.WieldYourPower;
import net.wieldyourpower.item.ModItems;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = WieldYourPower.MODID)
public final class EntityViewerEvents {

    private static final Map<UUID, Long> LAST_USE = new ConcurrentHashMap<>();

    private EntityViewerEvents() {
    }

    private static boolean enabled() {
        return WYPConfig.COMMON.entityViewerEnabled.get();
    }

    private static boolean holdingViewer(Player player) {
        return player.getMainHandItem().is(ModItems.ENTITY_VIEWER.get())
                || player.getOffhandItem().is(ModItems.ENTITY_VIEWER.get());
    }

    /**
     * @return true if the player is still on the viewer cooldown (blocks chat spam).
     */
    private static boolean onCooldown(Player player) {
        int cooldown = WYPConfig.COMMON.entityViewerCooldown.get();
        long now = player.level().getGameTime();
        Long last = LAST_USE.get(player.getUUID());
        if (cooldown > 0 && last != null && now - last < cooldown) {
            return true;
        }
        LAST_USE.put(player.getUUID(), now);
        return false;
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!enabled()) {
            return;
        }
        Player player = event.getEntity();
        Entity target = normalize(event.getTarget());
        if (!holdingViewer(player)) {
            return;
        }
        if (target instanceof ItemEntity && !WYPConfig.COMMON.entityViewerSelectDrops.get()) {
            return;
        }
        if (player.level().isClientSide) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!onCooldown(player)) {
            sendInfo(player, target);
        }
    }

    /**
     * The viewer casts its own ray instead of touching the player's reach attribute. This fires for
     * right-clicks that did not hit an entity directly, letting the viewer reach further and select
     * entities with an inflated hitbox.
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!enabled()) {
            return;
        }
        Player player = event.getEntity();
        if (player.level().isClientSide || !holdingViewer(player)) {
            return;
        }
        Entity target = rayPick(player);
        if (target != null) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (!onCooldown(player)) {
                sendInfo(player, target);
            }
        }
    }

    /**
     * Multi-part entities (Ender Dragon etc.) are picked as their parts; commands need the parent UUID.
     */
    private static Entity normalize(Entity entity) {
        return entity instanceof PartEntity<?> part ? part.getParent() : entity;
    }

    private static Entity rayPick(Player player) {
        double bonus = WYPConfig.COMMON.entityViewerReach.get();
        double inflate = WYPConfig.COMMON.entityViewerHitbox.get();
        double reach = player.getAttributeValue(ForgeMod.ENTITY_REACH.get()) + bonus;

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 lookNorm = look.normalize();
        Vec3 end = eye.add(look.scale(reach));
        AABB search = player.getBoundingBox().inflate(reach + inflate).expandTowards(look.scale(reach));

        Entity best = null;
        // Prefer the entity whose hit point is closest to the crosshair line (best alignment),
        // using distance along the ray only as a tie-breaker.
        double bestPerpendicular = Double.MAX_VALUE;
        double bestAlong = Double.MAX_VALUE;
        for (Entity entity : player.level().getEntities(player, search,
                e -> e != player && !e.isSpectator() && (e instanceof ItemEntity || e.isPickable()))) {
            if (entity instanceof ItemEntity && !WYPConfig.COMMON.entityViewerSelectDrops.get()) {
                continue;
            }
            Optional<Vec3> hit = entity.getBoundingBox().inflate(inflate).clip(eye, end);
            if (hit.isEmpty()) {
                continue;
            }
            Vec3 toHit = hit.get().subtract(eye);
            double along = toHit.dot(lookNorm);
            if (along < 0.0D) {
                continue;
            }
            Vec3 perpendicular = toHit.subtract(lookNorm.scale(along));
            double perpendicularSqr = perpendicular.lengthSqr();
            if (perpendicularSqr < bestPerpendicular - 1.0E-6D
                    || (Math.abs(perpendicularSqr - bestPerpendicular) <= 1.0E-6D && along < bestAlong)) {
                bestPerpendicular = perpendicularSqr;
                bestAlong = along;
                best = entity;
            }
        }
        return best == null ? null : normalize(best);
    }

    private static void sendInfo(Player player, Entity target) {
        UUID uuid = target.getUUID();
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        String type = typeId == null ? "unknown" : typeId.toString();
        Vec3 pos = target.position();

        player.sendSystemMessage(Component.literal("===== ").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable("item.wieldyourpower.entity_viewer").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" =====").withStyle(ChatFormatting.GOLD)));

        player.sendSystemMessage(label("entity_viewer.name", target.getDisplayName().getString()));
        player.sendSystemMessage(label("entity_viewer.type", type));
        player.sendSystemMessage(label("entity_viewer.uuid", uuid.toString()));
        player.sendSystemMessage(label("entity_viewer.pos",
                String.format("%.2f %.2f %.2f", pos.x, pos.y, pos.z)));

        if (target instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getItem();
            player.sendSystemMessage(label("entity_viewer.item",
                    stack.getCount() + "x " + stack.getHoverName().getString()));
        }
        if (target instanceof LivingEntity living) {
            player.sendSystemMessage(label("entity_viewer.health",
                    String.format("%.2f / %.2f", living.getHealth(), living.getMaxHealth())));
            if (living.isInvulnerable()) {
                player.sendSystemMessage(Component.translatable("entity_viewer.invulnerable").withStyle(ChatFormatting.RED));
            }
        }

        MutableComponent killOne = button("entity_viewer.button.kill_one", ChatFormatting.RED, "/wyp kill " + uuid);
        MutableComponent killType = button("entity_viewer.button.kill_type", ChatFormatting.GOLD,
                "/wyp kill @e[type=" + type + "]");
        MutableComponent killNearType = button("entity_viewer.button.kill_near_type", ChatFormatting.YELLOW,
                "/wyp kill @e[type=" + type + ",distance=..16]");

        MutableComponent actions = Component.translatable("entity_viewer.actions").withStyle(ChatFormatting.GRAY)
                .append(killOne).append(Component.literal(" ")).append(killType)
                .append(Component.literal(" ")).append(killNearType);
        if (!(target instanceof Player)) {
            actions.append(Component.literal(" "))
                    .append(button("entity_viewer.button.favor_add", ChatFormatting.GREEN, "/wyp favor add " + uuid))
                    .append(Component.literal(" "))
                    .append(button("entity_viewer.button.favor_remove", ChatFormatting.DARK_GREEN, "/wyp favor remove " + uuid));
        }
        player.sendSystemMessage(actions);
    }

    private static Component label(String key, Object value) {
        return Component.translatable(key).withStyle(ChatFormatting.AQUA)
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(value)).withStyle(ChatFormatting.WHITE));
    }

    private static MutableComponent button(String key, ChatFormatting color, String command) {
        return Component.translatable(key)
                .withStyle(Style.EMPTY
                        .withColor(color)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("entity_viewer.button.tooltip", command))))
                .append(Component.literal(" ").withStyle(ChatFormatting.RESET));
    }
}
