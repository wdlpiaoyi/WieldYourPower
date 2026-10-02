package net.wieldyourpower.compat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.wieldyourpower.WYPConfig;
import net.wieldyourpower.common.NoUpdateEvents;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioEquipEvent;
import top.theillusivec4.curios.api.event.CurioUnequipEvent;

/**
 * Curios integration: the equip/unequip hooks and the check for whether a player wears an accessory that
 * grants the "no update" mode.
 *
 * <p>Curios is a <b>soft</b> dependency (compileOnly). This class references Curios types, so it is only
 * loaded and registered when Curios is loaded ({@code ModList.get().isLoaded("curios")}). The qualifying
 * item is chosen by the item tag {@code noUpdateCurioTag}; the check only runs when the equipment changes
 * (or on login), never every tick.</p>
 */
public final class CuriosNoUpdate {

    private CuriosNoUpdate() {
    }

    /** Register the equip/unequip refresh hooks. Only call when Curios is loaded. */
    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(CuriosNoUpdate::onEquip);
        MinecraftForge.EVENT_BUS.addListener(CuriosNoUpdate::onUnequip);
    }

    private static void onEquip(CurioEquipEvent event) {
        refresh(event.getEntity());
    }

    private static void onUnequip(CurioUnequipEvent event) {
        refresh(event.getEntity());
    }

    private static void refresh(LivingEntity entity) {
        if (entity != null) {
            // The event fires around the inventory change, so re-check on the player's next tick.
            NoUpdateEvents.requestCurioRefresh(entity.getUUID());
        }
    }

    public static boolean wearsQualifyingCurio(Player player) {
        ResourceLocation tagId = ResourceLocation.tryParse(WYPConfig.COMMON.noUpdateCurioTag.get());
        if (tagId == null) {
            return false;
        }
        TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
        return CuriosApi.getCuriosInventory(player)
                .resolve()
                .map(handler -> handler.isEquipped(stack -> stack.is(tag)))
                .orElse(false);
    }
}
