package renderer.geometry;

import renderer.math.Vector2;
import renderer.math.Vector3;
import renderer.math.Ray;
import renderer.math.BBox;

/**
 * Represents an infinite 3D plane.
 */
public final class Plane implements Shape {
    public final Vector3 point;
    public final Vector3 normal;
    public final renderer.material.Material material;
    private final BBox bounds;
    
    // Orthonormal basis vectors for texture coordinates
    private final Vector3 uAxis;
    private final Vector3 vAxis;

    public Plane(Vector3 point, Vector3 normal) {
        this(point, normal, renderer.material.Material.DEFAULT);
    }

    public Plane(Vector3 point, Vector3 normal, renderer.material.Material material) {
        this.point = point;
        this.normal = normal.normalize();
        this.material = material;
        
        // Construct an orthonormal basis (uAxis, vAxis) on the plane
        Vector3 temp = (Math.abs(this.normal.x) > 0.9) ? new Vector3(0, 1, 0) : new Vector3(1, 0, 0);
        this.uAxis = this.normal.cross(temp).normalize();
        this.vAxis = this.normal.cross(this.uAxis).normalize();

        // Infinite plane bounds
        this.bounds = new BBox(
            new Vector3(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY),
            new Vector3(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
        );
    }

    @Override
    public boolean intersect(Ray ray, HitRecord rec) {
        double denom = normal.dot(ray.direction);
        
        // Ray is parallel or almost parallel to the plane
        if (Math.abs(denom) < 1e-6) {
            return false;
        }

        double t = point.sub(ray.origin).dot(normal) / denom;
        
        // Check if intersection point lies within ray's parametric range
        if (t < ray.tMin || t > ray.tMax) {
            return false;
        }

        rec.t = t;
        rec.point = ray.pointAt(t);
        rec.setFaceNormal(ray, normal);
        
        // Project local coordinates onto the plane's basis vectors
        Vector3 local = rec.point.sub(point);
        double u = local.dot(uAxis);
        double v = local.dot(vAxis);
        rec.uv = new Vector2(u, v);
        rec.material = this.material;

        return true;
    }

    @Override
    public BBox getBounds() {
        return bounds;
    }
}
