package net.wieldyourpower.capability;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ModCapabilities {

    public static final Capability<IPlayerLimits> PLAYER_LIMITS = CapabilityManager.get(new CapabilityToken<>() {
    });

    /**
     * Fallback storage used only if the capability is unavailable (kept so the limiter still works).
     */
    private static final Map<UUID, IPlayerLimits> FALLBACK = new ConcurrentHashMap<>();

    private ModCapabilities() {
    }

    public static LazyOptional<IPlayerLimits> get(Player player) {
        return player.getCapability(PLAYER_LIMITS);
    }

    @Nullable
    public static IPlayerLimits resolve(Player player) {
        IPlayerLimits viaCapability = player.getCapability(PLAYER_LIMITS).resolve().orElse(null);
        if (viaCapability != null) {
            return viaCapability;
        }
        return FALLBACK.computeIfAbsent(player.getUUID(), uuid -> new PlayerLimits());
    }
}

