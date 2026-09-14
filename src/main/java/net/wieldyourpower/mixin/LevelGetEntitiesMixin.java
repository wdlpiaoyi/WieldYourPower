package net.wieldyourpower.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.wieldyourpower.common.CreativeDefenseEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Removes a protected creative player from {@code Level.getEntities} results (Mekanism-style entity
 * lookup filtering) so targeting and ray picks cannot see them.
 */
@Mixin(Level.class)
public abstract class LevelGetEntitiesMixin {

    @Inject(method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
            at = @At("RETURN"))
    private void wieldyourpower$hideHitbox(Entity except, AABB area, Predicate<? super Entity> filter,
                                           CallbackInfoReturnable<List<Entity>> callback) {
        List<Entity> list = callback.getReturnValue();
        if (list != null && !list.isEmpty() && CreativeDefenseEvents.hasProtected()) {
            list.removeIf(CreativeDefenseEvents::shouldHideHitbox);
        }
    }
}
