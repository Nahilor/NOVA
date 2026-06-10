package renderer.math;

/**
 * Axis-Aligned Bounding Box (AABB) used for spatial acceleration structures.
 */
public final class BBox {
    public final Vector3 min;
    public final Vector3 max;

    /**
     * Creates an empty/invalid bounding box.
     */
    public BBox() {
        this.min = new Vector3(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.max = new Vector3(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
    }

    /**
     * Creates a bounding box that encloses a single point.
     */
    public BBox(Vector3 p) {
        this.min = p;
        this.max = p;
    }

    /**
     * Creates a bounding box with explicit min and max points.
     */
    public BBox(Vector3 min, Vector3 max) {
        this.min = min;
        this.max = max;
    }

    /**
     * Returns a new BBox that is the union of this box and another box.
     */
    public BBox union(BBox other) {
        return new BBox(this.min.min(other.min), this.max.max(other.max));
    }

    /**
     * Returns a new BBox that is the union of this box and a point.
     */
    public BBox union(Vector3 p) {
        return new BBox(this.min.min(p), this.max.max(p));
    }

    /**
     * Calculates the surface area of the bounding box.
     */
    public double surfaceArea() {
        double dx = max.x - min.x;
        double dy = max.y - min.y;
        double dz = max.z - min.z;
        if (dx <= 0.0 || dy <= 0.0 || dz <= 0.0) {
            return 0.0;
        }
        return 2.0 * (dx * dy + dy * dz + dz * dx);
    }

    /**
     * Returns the centroid (center point) of the bounding box.
     */
    public Vector3 centroid() {
        return min.add(max).mul(0.5);
    }

    /**
     * Returns the index of the longest axis:
     * 0 for X, 1 for Y, 2 for Z.
     */
    public int longestAxis() {
        double dx = max.x - min.x;
        double dy = max.y - min.y;
        double dz = max.z - min.z;
        if (dx > dy && dx > dz) return 0;
        if (dy > dz) return 1;
        return 2;
    }

    /**
     * Intersects a ray with this bounding box.
     * Returns the parametric hit distance tMin if it intersects,
     * or Double.POSITIVE_INFINITY if there is no intersection.
     * Fully robust to division by zero and NaN edge cases.
     */
    public double intersect(Ray ray) {
        double tMin = ray.tMin;
        double tMax = ray.tMax;

        for (int i = 0; i < 3; i++) {
            double invD = 1.0 / ray.direction.get(i);
            double t0 = (min.get(i) - ray.origin.get(i)) * invD;
            double t1 = (max.get(i) - ray.origin.get(i)) * invD;

            if (invD < 0.0) {
                double temp = t0;
                t0 = t1;
                t1 = temp;
            }

            // Handle NaN if min/max coordinate difference is zero and direction component is zero
            if (Double.isNaN(t0)) t0 = ray.tMin;
            if (Double.isNaN(t1)) t1 = ray.tMax;

            tMin = Math.max(t0, tMin);
            tMax = Math.min(t1, tMax);

            if (tMax <= tMin) {
                return Double.POSITIVE_INFINITY;
            }
        }
        return tMin;
    }

    @Override
    public String toString() {
        return String.format("BBox(min=%s, max=%s)", min, max);
    }
}
