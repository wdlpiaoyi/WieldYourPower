package net.wieldyourpower.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.wieldyourpower.client.cloth.ClothScreens;

import java.util.List;

/**
 * Client-only packet handling. Kept in a dedicated class so that packets loaded on a dedicated server
 * never reference client classes (which the runtime dist cleaner would reject).
 */
@OnlyIn(Dist.CLIENT)
public final class ClientPacketHandler {

    private ClientPacketHandler() {
    }

    public static void handleLimitsSync(double walkSpeed, double flyHorizontal, double flyVertical,
                                        int mineSpeed, int mineInterval, double jumpLimit, double stepLimit,
                                        List<String> attributeLimits, List<String> allyProtection) {
        ClientLimits.setAll(walkSpeed, flyHorizontal, flyVertical, mineSpeed, mineInterval,
                jumpLimit, stepLimit, attributeLimits, allyProtection);
    }

    public static void openScreen(int screen) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen parent = minecraft.screen;
        Screen target = screen == net.wieldyourpower.network.PacketOpenPanel.CONFIG
                ? ClothScreens.config(parent)
                : ClothScreens.limits(parent);
        minecraft.setScreen(target);
    }
}
