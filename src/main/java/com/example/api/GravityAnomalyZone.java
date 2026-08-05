package com.example.api;

// PORT NOTE: copied verbatim from the Zemlya project. See Gravity.java.
public class GravityAnomalyZone {
    private static double minY = Double.NaN;
    private static double maxY = Double.NaN;
    private static double centerX;
    private static double centerZ;
    private static double radiusSq = 300.0 * 300.0;

    public static void set(double transitionMinY, double transitionMaxY, double cx, double cz, double radius) {
        minY = transitionMinY;
        maxY = transitionMaxY;
        centerX = cx;
        centerZ = cz;
        radiusSq = radius * radius;
    }

    public static void clear() {
        minY = Double.NaN;
        maxY = Double.NaN;
    }

    public static boolean isActive() {
        return !Double.isNaN(minY);
    }


    public static float computeStrength(double interpY, double entityX, double entityZ, Gravity currentGravity) {
        if (!isActive()) return 0.0f;

        // Check XZ radius
        double dx = entityX - centerX;
        double dz = entityZ - centerZ;
        if (dx * dx + dz * dz > radiusSq) return 0.0f;

        if (currentGravity == Gravity.DOWN) {
            // Going up: transition from 0 to 1
            if (interpY < minY) return 0.0f;
            if (interpY >= maxY) return 1.0f;
            return (float) ((interpY - minY) / (maxY - minY));
        } else if (currentGravity == Gravity.UP) {
            // Coming back down with UP gravity: reverse
            if (interpY >= maxY) return 0.0f;
            if (interpY < minY) return 1.0f;
            return (float) ((maxY - interpY) / (maxY - minY));
        }

        return 0.0f;
    }
}
