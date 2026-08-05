package com.example.api;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

// PORT NOTE: copied verbatim from the Zemlya project (src/main/java/com/example/api).
// Zemlya is a compileOnly dependency that is not part of this repository, so the
// three public API types it exposes are bundled here. No API changes were needed
// for 1.21.11: Direction and Vec3 are unchanged from 26.1.
public enum Gravity {
    DOWN(Direction.DOWN, new Quaternionf().identity()),
    UP(Direction.UP, new Quaternionf().rotateZ((float) Math.toRadians(-180))),
    NORTH(Direction.NORTH, new Quaternionf().rotateX((float) Math.toRadians(-90))),
    SOUTH(Direction.SOUTH,
            new Quaternionf().rotateX((float) Math.toRadians(-90)).rotateY((float) Math.toRadians(-180))),
    WEST(Direction.WEST, new Quaternionf().rotateX((float) Math.toRadians(-90)).rotateY((float) Math.toRadians(-90))),
    EAST(Direction.EAST, new Quaternionf().rotateX((float) Math.toRadians(-90)).rotateY((float) Math.toRadians(-270)));

    private final Direction direction;
    private final Quaternionf rotation;
    private final Quaternionf inverseRotation;
    private final Matrix3f transformMatrix;
    private final Matrix3f inverseTransformMatrix;

    Gravity(Direction direction, Quaternionf rotation) {
        this.direction = direction;
        this.rotation = rotation;
        this.inverseRotation = new Quaternionf(rotation).conjugate();
        this.transformMatrix = new Matrix3f().set(rotation);
        this.inverseTransformMatrix = new Matrix3f().set(inverseRotation);
    }

    public Direction getDirection() {
        return direction;
    }

    public Quaternionf getRotation() {
        return new Quaternionf(rotation);
    }

    public Quaternionf getInverseRotation() {
        return new Quaternionf(inverseRotation);
    }


    public Vec3 toWorld(Vec3 localVector) {
        double x = localVector.x;
        double y = localVector.y;
        double z = localVector.z;
        return switch (this) {
            case DOWN -> new Vec3(x, y, z);
            case UP -> new Vec3(-x, -y, z);
            case NORTH -> new Vec3(x, -z, y);
            case SOUTH -> new Vec3(-x, -z, -y);
            case WEST -> new Vec3(y, -z, -x);
            case EAST -> new Vec3(-y, -z, x);
        };
    }


    public Vec3 toLocal(Vec3 worldVector) {
        double x = worldVector.x;
        double y = worldVector.y;
        double z = worldVector.z;
        return switch (this) {
            case DOWN -> new Vec3(x, y, z);
            case UP -> new Vec3(-x, -y, z);
            case NORTH -> new Vec3(x, z, -y);
            case SOUTH -> new Vec3(-x, -z, -y);
            case WEST -> new Vec3(-z, x, -y);
            case EAST -> new Vec3(z, -x, -y);
        };
    }

    public static Gravity fromDirection(Direction direction) {
        for (Gravity g : values()) {
            if (g.direction == direction)
                return g;
        }
        return DOWN;
    }
}
