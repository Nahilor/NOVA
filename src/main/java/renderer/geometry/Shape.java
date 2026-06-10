package renderer.geometry;

import renderer.math.Ray;
import renderer.math.BBox;

/**
 * Interface representing any geometry that can be intersected by a ray.
 */
public interface Shape {
    /**
     * Test intersection between a ray and this shape.
     * If the ray intersects the shape within the ray's tMin and tMax range,
     * updates the HitRecord with details (t, point, normal, uv, frontFacing)
     * and returns true. Otherwise, returns false.
     * 
     * @param ray The ray to test against.
     * @param rec The HitRecord to populate with intersection details if a hit occurs.
     * @return true if an intersection is found, false otherwise.
     */
    boolean intersect(Ray ray, HitRecord rec);

    /**
     * Returns the axis-aligned bounding box (AABB) of the shape.
     * Used for building acceleration structures like BVH.
     * 
     * @return The bounding box of the shape.
     */
    BBox getBounds();
}
