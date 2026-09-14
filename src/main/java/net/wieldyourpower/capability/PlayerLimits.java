package net.wieldyourpower.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.ArrayList;
import java.util.List;

public class PlayerLimits implements IPlayerLimits {

    public static final double DEFAULT_WALK = -1.0D;
    public static final double DEFAULT_FLY_H = -1.0D;
    public static final double DEFAULT_FLY_V = -1.0D;
    public static final int DEFAULT_MINE_SPEED = 0;
    public static final int DEFAULT_MINE_INTERVAL = 0;
    public static final double DEFAULT_JUMP = -1.0D;
    public static final double DEFAULT_STEP = -1.0D;

    private double walkSpeed = DEFAULT_WALK;
    private double flySpeedHorizontal = DEFAULT_FLY_H;
    private double flySpeedVertical = DEFAULT_FLY_V;
    private int mineSpeed = DEFAULT_MINE_SPEED;
    private int mineInterval = DEFAULT_MINE_INTERVAL;
    private double jumpLimit = DEFAULT_JUMP;
    private double stepLimit = DEFAULT_STEP;
    private List<String> attributeLimits = new ArrayList<>();
    private List<String> allyProtection = new ArrayList<>(List.of("type:touhou_little_maid:maid"));

    @Override
    public double getWalkSpeedLimit() {
        return this.walkSpeed;
    }

    @Override
    public void setWalkSpeedLimit(double value) {
        this.walkSpeed = value;
    }

    @Override
    public double getFlySpeedHorizontalLimit() {
        return this.flySpeedHorizontal;
    }

    @Override
    public void setFlySpeedHorizontalLimit(double value) {
        this.flySpeedHorizontal = value;
    }

    @Override
    public double getFlySpeedVerticalLimit() {
        return this.flySpeedVertical;
    }

    @Override
    public void setFlySpeedVerticalLimit(double value) {
        this.flySpeedVertical = value;
    }

    @Override
    public int getMineSpeedLimit() {
        return this.mineSpeed;
    }

    @Override
    public void setMineSpeedLimit(int value) {
        this.mineSpeed = value;
    }

    @Override
    public int getMineInterval() {
        return this.mineInterval;
    }

    @Override
    public void setMineInterval(int value) {
        this.mineInterval = value;
    }

    @Override
    public boolean isMiningForbidden() {
        return this.mineSpeed < 0;
    }

    @Override
    public double getJumpLimit() {
        return this.jumpLimit;
    }

    @Override
    public void setJumpLimit(double value) {
        this.jumpLimit = value;
    }

    @Override
    public double getStepLimit() {
        return this.stepLimit;
    }

    @Override
    public void setStepLimit(double value) {
        this.stepLimit = value;
    }

    @Override
    public List<String> getAttributeLimits() {
        return this.attributeLimits;
    }

    @Override
    public void setAttributeLimits(List<String> entries) {
        this.attributeLimits = entries == null ? new ArrayList<>() : new ArrayList<>(entries);
    }

    @Override
    public List<String> getAllyProtection() {
        return this.allyProtection;
    }

    @Override
    public void setAllyProtection(List<String> entries) {
        this.allyProtection = entries == null ? new ArrayList<>() : new ArrayList<>(entries);
    }

    @Override
    public void copyFrom(IPlayerLimits other) {
        if (other == null) {
            return;
        }
        this.walkSpeed = other.getWalkSpeedLimit();
        this.flySpeedHorizontal = other.getFlySpeedHorizontalLimit();
        this.flySpeedVertical = other.getFlySpeedVerticalLimit();
        this.mineSpeed = other.getMineSpeedLimit();
        this.mineInterval = other.getMineInterval();
        this.jumpLimit = other.getJumpLimit();
        this.stepLimit = other.getStepLimit();
        this.attributeLimits = new ArrayList<>(other.getAttributeLimits());
        this.allyProtection = new ArrayList<>(other.getAllyProtection());
    }

    @Override
    public void saveNBT(CompoundTag tag) {
        tag.putDouble("WalkSpeed", this.walkSpeed);
        tag.putDouble("FlySpeedH", this.flySpeedHorizontal);
        tag.putDouble("FlySpeedV", this.flySpeedVertical);
        tag.putInt("MineSpeed", this.mineSpeed);
        tag.putInt("MineInterval", this.mineInterval);
        tag.putDouble("JumpLimit", this.jumpLimit);
        tag.putDouble("StepLimit", this.stepLimit);
        ListTag attributes = new ListTag();
        for (String entry : this.attributeLimits) {
            attributes.add(StringTag.valueOf(entry));
        }
        tag.put("AttributeLimits", attributes);
        ListTag allies = new ListTag();
        for (String entry : this.allyProtection) {
            allies.add(StringTag.valueOf(entry));
        }
        tag.put("AllyProtection", allies);
    }

    @Override
    public void loadNBT(CompoundTag tag) {
        if (tag.contains("WalkSpeed")) {
            this.walkSpeed = tag.getDouble("WalkSpeed");
        }
        if (tag.contains("FlySpeedH")) {
            this.flySpeedHorizontal = tag.getDouble("FlySpeedH");
        }
        if (tag.contains("FlySpeedV")) {
            this.flySpeedVertical = tag.getDouble("FlySpeedV");
        }
        if (tag.contains("MineSpeed")) {
            this.mineSpeed = tag.getInt("MineSpeed");
        }
        if (tag.contains("MineInterval")) {
            this.mineInterval = tag.getInt("MineInterval");
        }
        if (tag.contains("JumpLimit")) {
            this.jumpLimit = tag.getDouble("JumpLimit");
        }
        if (tag.contains("StepLimit")) {
            this.stepLimit = tag.getDouble("StepLimit");
        }
        if (tag.contains("AttributeLimits")) {
            List<String> attributes = new ArrayList<>();
            ListTag list = tag.getList("AttributeLimits", 8);
            for (int i = 0; i < list.size(); i++) {
                attributes.add(list.getString(i));
            }
            this.attributeLimits = attributes;
        }
        if (tag.contains("AllyProtection")) {
            List<String> allies = new ArrayList<>();
            ListTag list = tag.getList("AllyProtection", 8);
            for (int i = 0; i < list.size(); i++) {
                allies.add(list.getString(i));
            }
            this.allyProtection = allies;
        }
    }
}
