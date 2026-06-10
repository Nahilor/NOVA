package renderer.geometry;

import renderer.math.Vector2;
import renderer.math.Vector3;
import renderer.math.Ray;
import renderer.math.BBox;

/**
 * Represents a flat 3D triangle.
 */
public final class Triangle implements Shape {
    public final Vector3 v0, v1, v2;
    public final Vector3 n0, n1, n2;
    public final Vector2 uv0, uv1, uv2;
    public final Vector3 geometricNormal;
    public final renderer.material.Material material;
    private final BBox bounds;

    public Triangle(Vector3 v0, Vector3 v1, Vector3 v2) {
        this(v0, v1, v2, renderer.material.Material.DEFAULT);
    }

    public Triangle(Vector3 v0, Vector3 v1, Vector3 v2, renderer.material.Material material) {
        this(v0, v1, v2, null, null, null, null, null, null, material);
    }

    public Triangle(
        Vector3 v0, Vector3 v1, Vector3 v2,
        Vector3 n0, Vector3 n1, Vector3 n2,
        Vector2 uv0, Vector2 uv1, Vector2 uv2
    ) {
        this(v0, v1, v2, n0, n1, n2, uv0, uv1, uv2, renderer.material.Material.DEFAULT);
    }

    public Triangle(
        Vector3 v0, Vector3 v1, Vector3 v2,
        Vector3 n0, Vector3 n1, Vector3 n2,
        Vector2 uv0, Vector2 uv1, Vector2 uv2,
        renderer.material.Material material
    ) {
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
        this.n0 = n0;
        this.n1 = n1;
        this.n2 = n2;
        this.uv0 = uv0;
        this.uv1 = uv1;
        this.uv2 = uv2;
        this.material = material;

        Vector3 e1 = v1.sub(v0);
        Vector3 e2 = v2.sub(v0);
        this.geometricNormal = e1.cross(e2).normalize();

        // Calculate bounding box
        Vector3 min = v0.min(v1).min(v2);
        Vector3 max = v0.max(v1).max(v2);

        // Pad flat dimensions by a small epsilon to prevent zero-volume AABB issues in BVH
        double epsilon = 1e-4;
        double dx = max.x - min.x;
        double dy = max.y - min.y;
        double dz = max.z - min.z;
        if (dx < epsilon) {
            min = new Vector3(min.x - epsilon, min.y, min.z);
            max = new Vector3(max.x + epsilon, max.y, max.z);
        }
        if (dy < epsilon) {
            min = new Vector3(min.x, min.y - epsilon, min.z);
            max = new Vector3(max.x, max.y + epsilon, max.z);
        }
        if (dz < epsilon) {
            min = new Vector3(min.x, min.y, min.z - epsilon);
            max = new Vector3(max.x, max.y, max.z + epsilon);
        }
        this.bounds = new BBox(min, max);
    }

    @Override
    public boolean intersect(Ray ray, HitRecord rec) {
        Vector3 e1 = v1.sub(v0);
        Vector3 e2 = v2.sub(v0);
        Vector3 pvec = ray.direction.cross(e2);
        double det = e1.dot(pvec);

        // If determinant is close to zero, ray lies in the plane of the triangle
        if (Math.abs(det) < 1e-8) {
            return false;
        }

        double invDet = 1.0 / det;
        Vector3 tvec = ray.origin.sub(v0);
        double u = tvec.dot(pvec) * invDet;
        if (u < 0.0 || u > 1.0) {
            return false;
        }

        Vector3 qvec = tvec.cross(e1);
        double v = ray.direction.dot(qvec) * invDet;
        if (v < 0.0 || u + v > 1.0) {
            return false;
        }

        double t = e2.dot(qvec) * invDet;
        if (t < ray.tMin || t > ray.tMax) {
            return false;
        }

        rec.t = t;
        rec.point = ray.pointAt(t);

        // Interpolate normal and texture coordinates using barycentric coordinates
        double w = 1.0 - u - v;
        Vector3 outwardNormal;
        if (n0 != null && n1 != null && n2 != null) {
            outwardNormal = n0.mul(w).add(n1.mul(u)).add(n2.mul(v)).normalize();
        } else {
            outwardNormal = geometricNormal;
        }

        rec.setFaceNormal(ray, outwardNormal);

        if (uv0 != null && uv1 != null && uv2 != null) {
            rec.uv = uv0.mul(w).add(uv1.mul(u)).add(uv2.mul(v));
        } else {
            rec.uv = new Vector2(u, v);
        }
        rec.material = this.material;

        return true;
    }

    @Override
    public BBox getBounds() {
        return bounds;
    }
}
