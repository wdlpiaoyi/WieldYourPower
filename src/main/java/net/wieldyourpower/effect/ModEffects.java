package net.wieldyourpower.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.wieldyourpower.WieldYourPower;

public final class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, WieldYourPower.MODID);

    /**
     * A purely cosmetic effect: it does nothing. While an entity carries the author's favor tag it is
     * kept applied so the HUD shows a matching icon as a hint.
     */
    public static final RegistryObject<MobEffect> AUTHORS_FAVOR =
            EFFECTS.register("authorsfavor", AuthorsFavorEffect::new);

    private ModEffects() {
    }

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }
}
