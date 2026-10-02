package net.wieldyourpower.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.phys.Vec3;
import net.wieldyourpower.util.NoUpdateMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Third layer of the "no update" suppression: vibrations, which is what sculk sensors, sculk
 * shriekers, sculk catalysts and wardens listen to.
 *
 * <p>{@code SculkSensorBlockEntity.VibrationUser} only ever learns about the world through
 * {@code GameEvent}s, so a suppressing player's own block change has to stop one step earlier than
 * the neighbour notify firewall in {@link LevelNotifyMixin}. Breaking a block fires
 * {@code GameEvent.BLOCK_DESTROY} from {@code Block.playerWillDestroy} (and from
 * {@code Level.destroyBlock}), placing one fires {@code GameEvent.BLOCK_PLACE} from
 * {@code BlockItem.place}, and right-click interactions fire {@code BLOCK_OPEN},
 * {@code BLOCK_ACTIVATE} and friends - all of them dispatched by
 * {@code ServerLevel.gameEvent(GameEvent, Vec3, GameEvent.Context)} into
 * {@code GameEventDispatcher.post}.</p>
 *
 * <p>This mixin drops the dispatch at that single choke point while the mode is suppressing, rather
 * than cancelling {@code ServerLevel.gameEvent}: the Forge {@code VanillaGameEvent} hook that runs
 * in front of it therefore still fires, so other mods keep seeing the events and nothing about the
 * suppression is invisible to them. The dispatcher is reached from nowhere else - {@code post} is
 * its only entry point and {@code ServerLevel.gameEvent} is its only vanilla caller - so sculk
 * sensors see no vibration at all and never power their redstone output. The client is not patched:
 * vibrations are decided on the server, and the block states it changes still sync normally.</p>
 */
@Mixin(GameEventDispatcher.class)
public abstract class GameEventDispatcherMixin {

    @Shadow
    @Final
    private ServerLevel level;

    @Inject(
            method = "post(Lnet/minecraft/world/level/gameevent/GameEvent;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void wieldyourpower$noUpdateVibration(GameEvent event, Vec3 pos, GameEvent.Context context, CallbackInfo ci) {
        if (NoUpdateMode.isSuppressing(this.level)) {
            ci.cancel();
        }
    }
}
