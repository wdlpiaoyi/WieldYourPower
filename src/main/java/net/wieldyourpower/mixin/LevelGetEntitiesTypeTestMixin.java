package net.wieldyourpower.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.wieldyourpower.common.CreativeDefenseEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Same as {@code LevelGetEntitiesMixin} for the {@code EntityTypeTest} overload, which is what
 * {@code getEntitiesOfClass} / {@code getNearestEntity} (used by AI) funnel through. Kept as its own
 * class so the refmap can map this overload's SRG name separately.
 */
@Mixin(Level.class)
public abstract class LevelGetEntitiesTypeTestMixin {

    @Inject(method = "getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
            at = @At("RETURN"))
    private void wieldyourpower$hideHitbox(EntityTypeTest<Entity, ?> test, AABB area, Predicate<? super Object> filter,
                                           CallbackInfoReturnable<List<?>> callback) {
        List<?> list = callback.getReturnValue();
        if (list != null && !list.isEmpty() && CreativeDefenseEvents.hasProtected()) {
            list.removeIf(value -> value instanceof Entity entity && CreativeDefenseEvents.shouldHideHitbox(entity));
        }
    }
}
