package renderer.acceleration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import renderer.geometry.HitRecord;
import renderer.geometry.Shape;
import renderer.geometry.Sphere;
import renderer.math.BBox;
import renderer.math.Ray;
import renderer.math.Vector3;

public class BVHTests {

    private static final double EPSILON = 1e-6;

    private void assertVectorEquals(Vector3 expected, Vector3 actual) {
        assertEquals(expected.x, actual.x, EPSILON);
        assertEquals(expected.y, actual.y, EPSILON);
        assertEquals(expected.z, actual.z, EPSILON);
    }

    @Test
    public void testEmptyBVH() {
        List<Shape> emptyList = new ArrayList<>();
        BVH bvh = new BVH(emptyList);
        assertNull(bvh.root);
        assertEquals(new Vector3(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY), bvh.getBounds().min);
        assertEquals(new Vector3(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY), bvh.getBounds().max);
        
        Ray ray = new Ray(new Vector3(0, 0, 0), new Vector3(0, 0, -1));
        HitRecord rec = new HitRecord();
        assertFalse(bvh.intersect(ray, rec));
    }

    @Test
    public void testSingleShapeBVH() {
        List<Shape> single = new ArrayList<>();
        Sphere s = new Sphere(new Vector3(0, 0, -5), 1.0);
        single.add(s);

        BVH bvh = new BVH(single);
        assertNotNull(bvh.root);
        assertTrue(bvh.root.isLeaf());
        assertEquals(1, bvh.root.primitives.size());
        assertSame(s, bvh.root.primitives.get(0));
        
        assertVectorEquals(s.getBounds().min, bvh.getBounds().min);
        assertVectorEquals(s.getBounds().max, bvh.getBounds().max);

        Ray ray = new Ray(new Vector3(0, 0, 0), new Vector3(0, 0, -1));
        HitRecord rec = new HitRecord();
        assertTrue(bvh.intersect(ray, rec));
        assertEquals(4.0, rec.t, EPSILON);
        assertVectorEquals(new Vector3(0, 0, -4), rec.point);
    }

    @Test
    public void testBVHCorrectnessAgainstBruteForce() {
        // Generate random spheres
        List<Shape> shapes = new ArrayList<>();
        Random rand = new Random(1337);

        for (int i = 0; i < 20; i++) {
            double x = (rand.nextDouble() - 0.5) * 10.0;
            double y = (rand.nextDouble() - 0.5) * 10.0;
            double z = -5.0 - rand.nextDouble() * 10.0; // always in front of camera
            double r = 0.2 + rand.nextDouble() * 0.8;
            shapes.add(new Sphere(new Vector3(x, y, z), r));
        }

        BVH bvh = new BVH(shapes);

        // Verify root bounding box matches the union of all shapes
        BBox expectedBounds = new BBox();
        for (Shape s : shapes) {
            expectedBounds = expectedBounds.union(s.getBounds());
        }
        assertVectorEquals(expectedBounds.min, bvh.getBounds().min);
        assertVectorEquals(expectedBounds.max, bvh.getBounds().max);

        // Generate rays from origin pointing in various directions
        for (int i = 0; i < 1000; i++) {
            double rx = (rand.nextDouble() - 0.5) * 2.0;
            double ry = (rand.nextDouble() - 0.5) * 2.0;
            double rz = -1.0;
            Vector3 direction = new Vector3(rx, ry, rz).normalize();
            Ray ray = new Ray(new Vector3(0, 0, 0), direction);

            // 1. Intersect using Brute Force
            Ray bruteRay = new Ray(ray.origin, ray.direction, ray.tMin, ray.tMax);
            HitRecord bruteRec = new HitRecord();
            boolean bruteHit = false;
            for (Shape s : shapes) {
                if (s.intersect(bruteRay, bruteRec)) {
                    bruteHit = true;
                    bruteRay.tMax = bruteRec.t; // Narrow search interval
                }
            }

            // 2. Intersect using BVH
            Ray bvhRay = new Ray(ray.origin, ray.direction, ray.tMin, ray.tMax);
            HitRecord bvhRec = new HitRecord();
            boolean bvhHit = bvh.intersect(bvhRay, bvhRec);

            // 3. Compare Results
            assertEquals(bruteHit, bvhHit);
            if (bruteHit) {
                assertEquals(bruteRec.t, bvhRec.t, EPSILON);
                assertVectorEquals(bruteRec.point, bvhRec.point);
                assertVectorEquals(bruteRec.normal, bvhRec.normal);
                assertEquals(bruteRec.frontFacing, bvhRec.frontFacing);
            }
        }
    }
}
