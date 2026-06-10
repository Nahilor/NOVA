package renderer.acceleration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import renderer.geometry.HitRecord;
import renderer.geometry.Shape;
import renderer.geometry.TriangleMesh;
import renderer.math.BBox;
import renderer.math.Ray;
import renderer.math.Vector2;
import renderer.math.Vector3;

/**
 * Benchmark tool comparing BVH acceleration structure traversal against brute force linear search.
 */
public final class BVHBenchmark {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("        BVH Acceleration Structure Benchmark     ");
        System.out.println("=================================================");

        // 1. Generate a procedural dense UV sphere mesh
        int segments = 60;
        int rings = 60;
        System.out.printf("Generating UV Sphere mesh (%d segments, %d rings)...\n", segments, rings);
        TriangleMesh sphereMesh = generateUVSphere(new Vector3(0, 0, 0), 2.0, segments, rings);
        List<Shape> triangles = sphereMesh.getTriangles();
        int numTriangles = triangles.size();
        System.out.printf("Mesh generated: %d vertices, %d triangles.\n\n", sphereMesh.vertices.length, numTriangles);

        // 2. Build BVH
        System.out.println("Building BVH acceleration structure...");
        long bvhBuildStart = System.nanoTime();
        BVH bvh = new BVH(triangles);
        long bvhBuildEnd = System.nanoTime();
        double bvhBuildTimeMs = (bvhBuildEnd - bvhBuildStart) / 1e6;
        System.out.printf("BVH build completed in %.2f ms.\n\n", bvhBuildTimeMs);

        // 3. Generate random test rays
        int numRays = 100000;
        System.out.printf("Generating %d random test rays...\n", numRays);
        BBox bounds = bvh.getBounds();
        List<Ray> testRays = generateRandomRays(bounds, numRays);
        System.out.println("Rays generated.\n");

        // 4. Run Brute Force Benchmark
        System.out.println("Running Brute Force benchmark (linear intersection scan)...");
        long bruteStart = System.nanoTime();
        int bruteHits = 0;
        double bruteTotalT = 0;
        for (Ray r : testRays) {
            // We must copy the ray since intersect modifies ray.tMax
            Ray rayCopy = new Ray(r.origin, r.direction, r.tMin, r.tMax);
            HitRecord rec = new HitRecord();
            boolean hit = false;
            for (Shape tri : triangles) {
                if (tri.intersect(rayCopy, rec)) {
                    hit = true;
                    rayCopy.tMax = rec.t;
                }
            }
            if (hit) {
                bruteHits++;
                bruteTotalT += rec.t;
            }
        }
        long bruteEnd = System.nanoTime();
        double bruteTimeMs = (bruteEnd - bruteStart) / 1e6;
        double bruteRaysPerSec = numRays / (bruteTimeMs / 1000.0);

        System.out.printf("Brute Force: %d hits, Total time: %.2f ms, Speed: %.0f rays/sec\n\n",
                bruteHits, bruteTimeMs, bruteRaysPerSec);

        // 5. Run BVH Benchmark
        System.out.println("Running BVH Traversal benchmark...");
        long bvhStart = System.nanoTime();
        int bvhHits = 0;
        double bvhTotalT = 0;
        for (Ray r : testRays) {
            Ray rayCopy = new Ray(r.origin, r.direction, r.tMin, r.tMax);
            HitRecord rec = new HitRecord();
            if (bvh.intersect(rayCopy, rec)) {
                bvhHits++;
                bvhTotalT += rec.t;
            }
        }
        long bvhEnd = System.nanoTime();
        double bvhTimeMs = (bvhEnd - bvhStart) / 1e6;
        double bvhRaysPerSec = numRays / (bvhTimeMs / 1000.0);

        System.out.printf("BVH Traversal: %d hits, Total time: %.2f ms, Speed: %.0f rays/sec\n\n",
                bvhHits, bvhTimeMs, bvhRaysPerSec);

        // 6. Output Statistics
        System.out.println("=================================================");
        System.out.println("                    Summary                      ");
        System.out.println("=================================================");
        System.out.printf("Triangles:              %d\n", numTriangles);
        System.out.printf("Rays tested:            %d\n", numRays);
        System.out.printf("Brute Force Hits:       %d  (Distance sum: %.4f)\n", bruteHits, bruteTotalT);
        System.out.printf("BVH Traversal Hits:     %d  (Distance sum: %.4f)\n", bvhHits, bvhTotalT);
        System.out.printf("Correctness Validation: %s\n", 
                (bruteHits == bvhHits && Math.abs(bruteTotalT - bvhTotalT) < 1e-3) ? "PASSED (Identical Results)" : "FAILED");
        System.out.printf("Speedup Factor:         %.2fx\n", bruteTimeMs / bvhTimeMs);
        System.out.println("=================================================");
    }

    /**
     * Procedurally generates a UV sphere mesh centered at `center` with radius `r`.
     */
    private static TriangleMesh generateUVSphere(Vector3 center, double r, int segments, int rings) {
        List<Vector3> verts = new ArrayList<>();
        List<Vector3> norms = new ArrayList<>();
        List<Vector2> uvs = new ArrayList<>();
        List<Integer> inds = new ArrayList<>();

        for (int ring = 0; ring <= rings; ring++) {
            double theta = ring * Math.PI / rings;
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);

            for (int seg = 0; seg <= segments; seg++) {
                double phi = seg * 2.0 * Math.PI / segments;
                double sinPhi = Math.sin(phi);
                double cosPhi = Math.cos(phi);

                double x = cosPhi * sinTheta;
                double y = cosTheta;
                double z = sinPhi * sinTheta;

                Vector3 outwardNormal = new Vector3(x, y, z);
                verts.add(center.add(outwardNormal.mul(r)));
                norms.add(outwardNormal);
                
                double u = (double) seg / segments;
                double v = (double) ring / rings;
                uvs.add(new Vector2(u, v));
            }
        }

        // Generate indices
        for (int ring = 0; ring < rings; ring++) {
            for (int seg = 0; seg < segments; seg++) {
                int first = ring * (segments + 1) + seg;
                int second = first + segments + 1;

                inds.add(first);
                inds.add(second);
                inds.add(first + 1);

                inds.add(second);
                inds.add(second + 1);
                inds.add(first + 1);
            }
        }

        Vector3[] finalVerts = verts.toArray(new Vector3[0]);
        Vector3[] finalNorms = norms.toArray(new Vector3[0]);
        Vector2[] finalUvs = uvs.toArray(new Vector2[0]);
        int[] finalInds = new int[inds.size()];
        for (int i = 0; i < inds.size(); i++) {
            finalInds[i] = inds.get(i);
        }

        return new TriangleMesh(finalVerts, finalNorms, finalUvs, finalInds);
    }

    /**
     * Generates random rays that shoot towards and through the bounding box bounds.
     */
    private static List<Ray> generateRandomRays(BBox bounds, int numRays) {
        List<Ray> rays = new ArrayList<>(numRays);
        Random rand = new Random(42); // Seeded for reproducibility
        Vector3 size = bounds.max.sub(bounds.min);
        Vector3 centroid = bounds.centroid();
        double maxRadius = size.length() * 1.5;

        for (int i = 0; i < numRays; i++) {
            // Pick a point on a sphere surrounding the bounding box as ray origin
            double theta = rand.nextDouble() * 2.0 * Math.PI;
            double phi = Math.acos(2.0 * rand.nextDouble() - 1.0);
            double sinPhi = Math.sin(phi);

            double ox = centroid.x + maxRadius * Math.cos(theta) * sinPhi;
            double oy = centroid.y + maxRadius * Math.sin(theta) * sinPhi;
            double oz = centroid.z + maxRadius * Math.cos(phi);
            Vector3 origin = new Vector3(ox, oy, oz);

            // Shoot the ray towards a random target point inside the bounding box
            double tx = bounds.min.x + rand.nextDouble() * size.x;
            double ty = bounds.min.y + rand.nextDouble() * size.y;
            double tz = bounds.min.z + rand.nextDouble() * size.z;
            Vector3 target = new Vector3(tx, ty, tz);

            Vector3 direction = target.sub(origin).normalize();
            rays.add(new Ray(origin, direction));
        }
        return rays;
    }
}
