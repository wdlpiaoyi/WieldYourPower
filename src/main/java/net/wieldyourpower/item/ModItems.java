package net.wieldyourpower.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.wieldyourpower.WieldYourPower;

public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, WieldYourPower.MODID);

    public static final RegistryObject<Item> ENTITY_VIEWER =
            ITEMS.register("entity_viewer", EntityViewerItem::new);

    public static final RegistryObject<Item> BLOCK_BREAKER =
            ITEMS.register("block_breaker", BlockBreakerItem::new);

    private ModItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
