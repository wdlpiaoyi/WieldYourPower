package net.wieldyourpower.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets the death screen open the command input (press {@code /} or {@code T}), so a player stuck on a
 * stuck/looping death screen can still run commands.
 */
@Mixin(Screen.class)
public abstract class ScreenDeathCommandMixin {

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void wieldyourpower$deathScreenCommand(int keyCode, int scanCode, int modifiers,
                                                   CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof DeathScreen
                && (keyCode == GLFW.GLFW_KEY_SLASH || keyCode == GLFW.GLFW_KEY_T)) {
            Minecraft.getInstance().setScreen(new ChatScreen("/"));
            callback.setReturnValue(true);
        }
    }
}
