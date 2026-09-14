package net.wieldyourpower.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerLimitsProvider implements ICapabilityProvider, ICapabilitySerializable<CompoundTag> {

    private final PlayerLimits limits = new PlayerLimits();
    private final LazyOptional<IPlayerLimits> optional = LazyOptional.of(() -> this.limits);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == ModCapabilities.PLAYER_LIMITS ? this.optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        this.limits.saveNBT(tag);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.limits.loadNBT(nbt);
    }

    public void invalidate() {
        this.optional.invalidate();
    }
}
