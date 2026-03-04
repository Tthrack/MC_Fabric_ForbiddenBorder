package com.example.forbiddenborder;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;

public class ForbiddenBorderState extends PersistentState {
    public static final String KEY = "forbidden_border";

    private boolean enabled = false;
    private double centerX = 0.0D;
    private double centerZ = 0.0D;
    private double radius = 64.0D;

    public static ForbiddenBorderState createDefault() {
        return new ForbiddenBorderState();
    }

    public static ForbiddenBorderState fromNbt(NbtCompound nbt) {
        ForbiddenBorderState state = new ForbiddenBorderState();
        state.enabled = nbt.getBoolean("enabled", false);
        state.centerX = nbt.getDouble("center_x", 0.0D);
        state.centerZ = nbt.getDouble("center_z", 0.0D);
        state.radius = Math.max(1.0D, nbt.getDouble("radius", 64.0D));
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putBoolean("enabled", this.enabled);
        nbt.putDouble("center_x", this.centerX);
        nbt.putDouble("center_z", this.centerZ);
        nbt.putDouble("radius", this.radius);
        return nbt;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.markDirty();
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterZ() {
        return centerZ;
    }

    public void setCenter(double centerX, double centerZ) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.markDirty();
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = Math.max(1.0D, radius);
        this.markDirty();
    }

    public boolean isInside(double x, double z) {
        double dx = x - this.centerX;
        double dz = z - this.centerZ;
        return (dx * dx + dz * dz) < (this.radius * this.radius);
    }
}
