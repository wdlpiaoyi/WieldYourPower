package net.wieldyourpower.capability;

import net.minecraft.nbt.CompoundTag;

import java.util.List;

/**
 * Per-player limit settings controlled by the player themselves.
 *
 * <p>Speed limits are expressed in blocks per tick. A value of {@code <= 0} means "no limit".</p>
 * <p>Mining speed is expressed in ticks and {@code -1} forbids mining entirely. {@code 0} means no limit.</p>
 */
public interface IPlayerLimits {

    double getWalkSpeedLimit();

    void setWalkSpeedLimit(double value);

    double getFlySpeedHorizontalLimit();

    void setFlySpeedHorizontalLimit(double value);

    double getFlySpeedVerticalLimit();

    void setFlySpeedVerticalLimit(double value);

    int getMineSpeedLimit();

    void setMineSpeedLimit(int value);

    int getMineInterval();

    void setMineInterval(int value);

    boolean isMiningForbidden();

    /** Jump velocity coefficient: -1 off, 1 vanilla, 0 = cannot jump. */
    double getJumpLimit();

    void setJumpLimit(double value);

    /** Step height coefficient: -1 off, 1 = vanilla 0.6, 0 = no auto-step. */
    double getStepLimit();

    void setStepLimit(double value);

    /** Custom attribute caps, one entry per line as {@code attribute,<attribute id>,<max>}. */
    List<String> getAttributeLimits();

    void setAttributeLimits(List<String> entries);

    /** Ally protection entries (same syntax as the kill honor list). Protected from all means except /wyp kill. */
    List<String> getAllyProtection();

    void setAllyProtection(List<String> entries);

    void copyFrom(IPlayerLimits other);

    void saveNBT(CompoundTag tag);

    void loadNBT(CompoundTag tag);
}
