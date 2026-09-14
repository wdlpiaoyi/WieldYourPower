package net.wieldyourpower.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * A no-op effect used only as a hint: while an entity carries the author's favor tag this effect is kept
 * applied so its icon shows up in the HUD. It has no gameplay behaviour.
 */
public class AuthorsFavorEffect extends MobEffect {

    public AuthorsFavorEffect() {
        super(MobEffectCategory.NEUTRAL, 0xFFC83C);
    }
}
