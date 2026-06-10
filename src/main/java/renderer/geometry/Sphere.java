package renderer.geometry;

import renderer.math.Vector2;
import renderer.math.Vector3;
import renderer.math.Ray;
import renderer.math.BBox;

/**
 * Represents a 3D sphere.
 */
public final class Sphere implements Shape {
    public final Vector3 center;
    public final double radius;
    public final renderer.material.Material material;
    private final BBox bounds;

    public Sphere(Vector3 center, double radius) {
        this(center, radius, renderer.material.Material.DEFAULT);
    }

    public Sphere(Vector3 center, double radius, renderer.material.Material material) {
        this.center = center;
        this.radius = radius;
        this.material = material;
        Vector3 rVec = new Vector3(radius, radius, radius);
        this.bounds = new BBox(center.sub(rVec), center.add(rVec));
    }

    @Override
    public boolean intersect(Ray ray, HitRecord rec) {
        Vector3 oc = ray.origin.sub(center);
        double a = 1.0; // ray.direction is normalized, so dot(dir, dir) is 1.0
        double halfB = oc.dot(ray.direction);
        double c = oc.lengthSquared() - radius * radius;

        double discriminant = halfB * halfB - c;
        if (discriminant < 0.0) {
            return false;
        }

        double sqrtd = Math.sqrt(discriminant);

        // Find the nearest root that lies in the acceptable range [tMin, tMax]
        double root = -halfB - sqrtd;
        if (root < ray.tMin || root > ray.tMax) {
            root = -halfB + sqrtd;
            if (root < ray.tMin || root > ray.tMax) {
                return false;
            }
        }

        rec.t = root;
        rec.point = ray.pointAt(rec.t);
        Vector3 outwardNormal = rec.point.sub(center).div(radius);
        rec.setFaceNormal(ray, outwardNormal);
        rec.uv = getSphereUv(outwardNormal);
        rec.material = this.material;

        return true;
    }

    @Override
    public BBox getBounds() {
        return bounds;
    }

    /**
     * Compute texture coordinates (u, v) for a point on a unit sphere centered at origin.
     * 
     * @param p A point on the unit sphere.
     * @return Vector2 containing the u, v texture coordinates.
     */
    private Vector2 getSphereUv(Vector3 p) {
        // p is a normalized outward normal pointing from center to intersection point
        double theta = Math.acos(-p.y);
        double phi = Math.atan2(-p.z, p.x) + Math.PI;

        double u = phi / (2.0 * Math.PI);
        double v = theta / Math.PI;
        return new Vector2(u, v);
    }
}
