package renderer.math;

/**
 * Represents a 3D ray: R(t) = origin + t * direction.
 * Includes parametric bounds tMin and tMax to prevent self-intersection (shadow acne)
 * and optimize traversal in acceleration structures.
 */
public final class Ray {
    public final Vector3 origin;
    public final Vector3 direction; // Normalized direction vector
    public double tMin;
    public double tMax;

    public Ray(Vector3 origin, Vector3 direction) {
        this(origin, direction, 1e-4, Double.POSITIVE_INFINITY);
    }

    public Ray(Vector3 origin, Vector3 direction, double tMin, double tMax) {
        this.origin = origin;
        this.direction = direction.normalize();
        this.tMin = tMin;
        this.tMax = tMax;
    }

    /**
     * Calculates the 3D position along the ray at parameter t.
     */
    public Vector3 pointAt(double t) {
        return origin.add(direction.mul(t));
    }

    @Override
    public String toString() {
        return String.format("Ray(org=%s, dir=%s, tMin=%.6f, tMax=%.6f)", origin, direction, tMin, tMax);
    }
}
