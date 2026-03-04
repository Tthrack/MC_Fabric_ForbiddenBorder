package com.example.forbiddenborder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.PersistentState;

public class ForbiddenBorderState extends PersistentState {
    public static final String KEY = "forbidden_border";

    public static final Codec<ForbiddenBorderState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("enabled", false).forGetter(ForbiddenBorderState::isEnabled),
        Codec.DOUBLE.optionalFieldOf("center_x", 0.0D).forGetter(ForbiddenBorderState::getCenterX),
        Codec.DOUBLE.optionalFieldOf("center_z", 0.0D).forGetter(ForbiddenBorderState::getCenterZ),
        Codec.DOUBLE.optionalFieldOf("radius", 64.0D).forGetter(ForbiddenBorderState::getRadius)
    ).apply(instance, ForbiddenBorderState::new));

    private boolean enabled;
    private double centerX;
    private double centerZ;
    private double radius;

    public ForbiddenBorderState() {
        this(false, 0.0D, 0.0D, 64.0D);
    }

    private ForbiddenBorderState(boolean enabled, double centerX, double centerZ, double radius) {
        this.enabled = enabled;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = Math.max(1.0D, radius);
    }

    public static ForbiddenBorderState createDefault() {
        return new ForbiddenBorderState();
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
