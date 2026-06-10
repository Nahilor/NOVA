package renderer.geometry;

import java.util.ArrayList;
import java.util.List;
import renderer.math.BBox;
import renderer.math.Ray;
import renderer.math.Vector2;
import renderer.math.Vector3;

/**
 * Represents a 3D triangle mesh sharing vertex, normal, and texture coordinate arrays.
 */
public final class TriangleMesh {
    public final Vector3[] vertices;
    public final Vector3[] normals;
    public final Vector2[] uvs;
    public final int[] indices; // Length is 3 * numTriangles

    public TriangleMesh(Vector3[] vertices, Vector3[] normals, Vector2[] uvs, int[] indices) {
        this.vertices = vertices;
        this.normals = normals;
        this.uvs = uvs;
        this.indices = indices;
    }

    /**
     * Returns a list of all triangles in this mesh, each represented as a Shape.
     * Sharing the parent mesh vertex buffers minimizes memory consumption.
     * 
     * @return List of Shape objects representing the triangles in the mesh.
     */
    public List<Shape> getTriangles() {
        int numTriangles = indices.length / 3;
        List<Shape> triangles = new ArrayList<>(numTriangles);
        for (int i = 0; i < numTriangles; i++) {
            triangles.add(new MeshTriangle(this, i));
        }
        return triangles;
    }

    /**
     * An individual triangle within the TriangleMesh, implementing the Shape interface.
     */
    public static class MeshTriangle implements Shape {
        public final TriangleMesh mesh;
        public final int index; // Index of the triangle in the mesh (i.e. 0, 1, ..., numTriangles - 1)
        private final BBox bounds;
        private final Vector3 geometricNormal;

        public MeshTriangle(TriangleMesh mesh, int index) {
            this.mesh = mesh;
            this.index = index;

            Vector3 v0 = getVertex(0);
            Vector3 v1 = getVertex(1);
            Vector3 v2 = getVertex(2);

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

        public Vector3 getVertex(int i) {
            return mesh.vertices[mesh.indices[index * 3 + i]];
        }

        public Vector3 getNormal(int i) {
            if (mesh.normals == null || mesh.normals.length == 0) return null;
            return mesh.normals[mesh.indices[index * 3 + i]];
        }

        public Vector2 getUv(int i) {
            if (mesh.uvs == null || mesh.uvs.length == 0) return null;
            return mesh.uvs[mesh.indices[index * 3 + i]];
        }

        @Override
        public boolean intersect(Ray ray, HitRecord rec) {
            Vector3 v0 = getVertex(0);
            Vector3 v1 = getVertex(1);
            Vector3 v2 = getVertex(2);

            Vector3 e1 = v1.sub(v0);
            Vector3 e2 = v2.sub(v0);
            Vector3 pvec = ray.direction.cross(e2);
            double det = e1.dot(pvec);

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

            double w = 1.0 - u - v;
            Vector3 n0 = getNormal(0);
            Vector3 n1 = getNormal(1);
            Vector3 n2 = getNormal(2);
            Vector3 outwardNormal;
            if (n0 != null && n1 != null && n2 != null) {
                outwardNormal = n0.mul(w).add(n1.mul(u)).add(n2.mul(v)).normalize();
            } else {
                outwardNormal = geometricNormal;
            }
            rec.setFaceNormal(ray, outwardNormal);

            Vector2 uv0 = getUv(0);
            Vector2 uv1 = getUv(1);
            Vector2 uv2 = getUv(2);
            if (uv0 != null && uv1 != null && uv2 != null) {
                rec.uv = uv0.mul(w).add(uv1.mul(u)).add(uv2.mul(v));
            } else {
                rec.uv = new Vector2(u, v);
            }

            return true;
        }

        @Override
        public BBox getBounds() {
            return bounds;
        }
    }
}
