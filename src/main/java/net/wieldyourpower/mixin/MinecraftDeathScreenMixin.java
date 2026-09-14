package net.wieldyourpower.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.wieldyourpower.common.CreativeDefenseEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Refuses to show the death screen to a protected creative player. Some mods set the client screen to
 * {@code DeathScreen} while the player is still alive (e.g. after forcing the synced health to a dead
 * value), which the server-side guards cannot undo.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftDeathScreenMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void wieldyourpower$blockCreativeDeathScreen(Screen screen, CallbackInfo callback) {
        Minecraft self = (Minecraft) (Object) this;
        if (screen instanceof DeathScreen && self.player != null
                && CreativeDefenseEvents.isProtectedCreative(self.player)) {
            callback.cancel();
        }
    }
}
