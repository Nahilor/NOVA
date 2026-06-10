package renderer.geometry;

import renderer.math.Vector2;
import renderer.math.Vector3;
import renderer.math.Ray;

/**
 * A mutable record to store information about a ray-geometry intersection.
 * Passing this by reference avoids garbage collection overhead inside the rendering loop.
 */
public class HitRecord {
    public double t;             // Distance along the ray at which the intersection occurred
    public Vector3 point;        // 3D point in space where the ray intersected the shape
    public Vector3 normal;       // Surface normal vector at the intersection point
    public Vector2 uv;           // Texture coordinates (u, v) at the intersection point
    public boolean frontFacing;  // True if the ray hit the front side of the surface
    public renderer.material.Material material; // Material at the intersection point

    /**
     * Sets the hit normal so that it always points against the ray direction.
     * Stores whether the hit was on the front face of the shape.
     * 
     * @param ray The intersecting ray.
     * @param outwardNormal The outward-pointing normal computed by the shape.
     */
    public void setFaceNormal(Ray ray, Vector3 outwardNormal) {
        frontFacing = ray.direction.dot(outwardNormal) < 0;
        normal = frontFacing ? outwardNormal : outwardNormal.mul(-1.0);
    }
}
