package renderer.math;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility functions for math operations and Monte Carlo sampling.
 */
public final class MathUtils {
    public static final double PI = Math.PI;
    public static final double TWO_PI = 2.0 * Math.PI;
    public static final double INV_PI = 1.0 / Math.PI;
    public static final double INV_TWO_PI = 1.0 / (2.0 * Math.PI);
    public static final double EPSILON = 1e-6;

    private MathUtils() {}

    /**
     * Converts degrees to radians.
     */
    public static double degToRad(double deg) {
        return deg * PI / 180.0;
    }

    /**
     * Converts radians to degrees.
     */
    public static double radToDeg(double rad) {
        return rad * 180.0 / PI;
    }

    /**
     * Clamps a value to the range [min, max].
     */
    public static double clamp(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    /**
     * Generates a random double in the range [0.0, 1.0).
     */
    public static double randomDouble() {
        return ThreadLocalRandom.current().nextDouble();
    }

    /**
     * Generates a random double in the range [min, max).
     */
    public static double randomDouble(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    /**
     * Samples a point uniformly from the surface of a unit sphere.
     * Analytical mapping:
     * z = 1 - 2 * xi_1
     * r = sqrt(1 - z^2)
     * phi = 2 * PI * xi_2
     */
    public static Vector3 randomUnitVector() {
        double u1 = randomDouble();
        double u2 = randomDouble();
        double z = 1.0 - 2.0 * u1;
        double r = Math.sqrt(Math.max(0.0, 1.0 - z * z));
        double phi = TWO_PI * u2;
        return new Vector3(r * Math.cos(phi), r * Math.sin(phi), z);
    }

    /**
     * Samples a point uniformly inside a unit sphere.
     * Rejection sampling is simple and robust.
     */
    public static Vector3 randomInUnitSphere() {
        while (true) {
            Vector3 p = new Vector3(randomDouble(-1.0, 1.0), randomDouble(-1.0, 1.0), randomDouble(-1.0, 1.0));
            if (p.lengthSquared() < 1.0) {
                return p;
            }
        }
    }

    /**
     * Samples a point uniformly from the surface of a unit hemisphere oriented along a normal.
     */
    public static Vector3 randomInHemisphere(Vector3 normal) {
        Vector3 inUnitSphere = randomUnitVector();
        if (inUnitSphere.dot(normal) > 0.0) {
            return inUnitSphere;
        } else {
            return inUnitSphere.negate();
        }
    }

    /**
     * Samples a point uniformly inside a 2D unit disk.
     * Analytical mapping:
     * r = sqrt(xi_1)
     * theta = 2 * PI * xi_2
     */
    public static Vector2 randomInUnitDisk() {
        double u1 = randomDouble();
        double u2 = randomDouble();
        double r = Math.sqrt(u1);
        double theta = TWO_PI * u2;
        return new Vector2(r * Math.cos(theta), r * Math.sin(theta));
    }

    /**
     * Samples a direction on a hemisphere using a cosine-weighted PDF: p(theta) = cos(theta) / PI.
     * Returning a local vector where Z is the normal.
     */
    public static Vector3 randomCosineDirection() {
        double u1 = randomDouble();
        double u2 = randomDouble();
        double r = Math.sqrt(u1);
        double phi = TWO_PI * u2;
        double x = r * Math.cos(phi);
        double y = r * Math.sin(phi);
        double z = Math.sqrt(Math.max(0.0, 1.0 - u1));
        return new Vector3(x, y, z);
    }

    /**
     * Aligns a local coordinate vector (where +Z is the local normal) to a world coordinate system
     * defined around the given world normal.
     */
    public static Vector3 alignToNormal(Vector3 local, Vector3 normal) {
        Vector3 w = normal.normalize();
        Vector3 a = (Math.abs(w.x) > 0.9) ? new Vector3(0, 1, 0) : new Vector3(1, 0, 0);
        Vector3 u = a.cross(w).normalize();
        Vector3 v = w.cross(u);
        
        // Transform local vector to world space
        return u.mul(local.x).add(v.mul(local.y)).add(w.mul(local.z));
    }
}
