package net.wieldyourpower.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side mirror of the player's limits. Kept independent of the capability API so the panel and
 * the client-side enforcement always work, even if the local capability is unavailable.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientLimits {

    public static double walkSpeed = -1.0D;
    public static double flySpeedHorizontal = -1.0D;
    public static double flySpeedVertical = -1.0D;
    public static int mineSpeed = 0;
    public static int mineInterval = 0;
    public static double jumpLimit = -1.0D;
    public static double stepLimit = -1.0D;
    public static List<String> attributeLimits = new ArrayList<>();
    public static List<String> allyProtection = new ArrayList<>(List.of("type,touhou_little_maid:maid"));

    private ClientLimits() {
    }

    public static void setAll(double walk, double flyH, double flyV, int mine, int interval,
                              double jump, double step, List<String> attributes, List<String> allies) {
        walkSpeed = walk;
        flySpeedHorizontal = flyH;
        flySpeedVertical = flyV;
        mineSpeed = mine;
        mineInterval = interval;
        jumpLimit = jump;
        stepLimit = step;
        attributeLimits = attributes == null ? new ArrayList<>() : new ArrayList<>(attributes);
        allyProtection = allies == null ? new ArrayList<>() : new ArrayList<>(allies);
    }
}
