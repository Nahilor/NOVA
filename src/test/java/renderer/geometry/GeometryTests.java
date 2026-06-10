package renderer.geometry;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import renderer.math.BBox;
import renderer.math.Ray;
import renderer.math.Vector2;
import renderer.math.Vector3;

public class GeometryTests {

    private static final double EPSILON = 1e-6;

    private void assertVectorEquals(Vector3 expected, Vector3 actual) {
        assertEquals(expected.x, actual.x, EPSILON);
        assertEquals(expected.y, actual.y, EPSILON);
        assertEquals(expected.z, actual.z, EPSILON);
    }

    private void assertVectorEquals(Vector2 expected, Vector2 actual) {
        assertEquals(expected.x, actual.x, EPSILON);
        assertEquals(expected.y, actual.y, EPSILON);
    }

    @Test
    public void testSphereIntersection() {
        Sphere sphere = new Sphere(new Vector3(0, 0, -5), 1.0);
        
        // Ray pointing directly at sphere center
        Ray ray = new Ray(new Vector3(0, 0, 0), new Vector3(0, 0, -1));
        HitRecord rec = new HitRecord();
        assertTrue(sphere.intersect(ray, rec));
        assertEquals(4.0, rec.t, EPSILON);
        assertVectorEquals(new Vector3(0, 0, -4), rec.point);
        assertVectorEquals(new Vector3(0, 0, 1), rec.normal);
        assertTrue(rec.frontFacing);

        // Ray from inside the sphere
        Ray insideRay = new Ray(new Vector3(0, 0, -5), new Vector3(0, 1, 0));
        assertTrue(sphere.intersect(insideRay, rec));
        assertEquals(1.0, rec.t, EPSILON);
        assertVectorEquals(new Vector3(0, -1, 0), rec.normal); // normal should point inward relative to ray direction
        assertFalse(rec.frontFacing);

        // Ray missing the sphere
        Ray missRay = new Ray(new Vector3(0, 2, 0), new Vector3(0, 0, -1));
        assertFalse(sphere.intersect(missRay, rec));

        // Ray pointing away from sphere
        Ray awayRay = new Ray(new Vector3(0, 0, 0), new Vector3(0, 0, 1));
        assertFalse(sphere.intersect(awayRay, rec));

        // Bounds check
        BBox bounds = sphere.getBounds();
        assertVectorEquals(new Vector3(-1, -1, -6), bounds.min);
        assertVectorEquals(new Vector3(1, 1, -4), bounds.max);
    }

    @Test
    public void testPlaneIntersection() {
        Plane plane = new Plane(new Vector3(0, -1, 0), new Vector3(0, 1, 0));
        HitRecord rec = new HitRecord();

        // Ray shooting down onto the plane
        Ray ray = new Ray(new Vector3(0, 2, 0), new Vector3(0, -1, 0));
        assertTrue(plane.intersect(ray, rec));
        assertEquals(3.0, rec.t, EPSILON);
        assertVectorEquals(new Vector3(0, -1, 0), rec.point);
        assertVectorEquals(new Vector3(0, 1, 0), rec.normal);
        assertTrue(rec.frontFacing);

        // Ray parallel to the plane
        Ray parallelRay = new Ray(new Vector3(0, 2, 0), new Vector3(1, 0, 0));
        assertFalse(plane.intersect(parallelRay, rec));

        // Ray shooting away from the plane
        Ray awayRay = new Ray(new Vector3(0, 2, 0), new Vector3(0, 1, 0));
        assertFalse(plane.intersect(awayRay, rec));
    }

    @Test
    public void testTriangleIntersection() {
        Vector3 v0 = new Vector3(-1, -1, -5);
        Vector3 v1 = new Vector3(1, -1, -5);
        Vector3 v2 = new Vector3(0, 1, -5);
        Triangle triangle = new Triangle(v0, v1, v2);
        HitRecord rec = new HitRecord();

        // Ray hitting the center of the triangle
        Ray ray = new Ray(new Vector3(0, 0, 0), new Vector3(0, 0, -1));
        assertTrue(triangle.intersect(ray, rec));
        assertEquals(5.0, rec.t, EPSILON);
        assertVectorEquals(new Vector3(0, 0, -5), rec.point);
        assertVectorEquals(new Vector3(0, 0, 1), rec.normal);
        assertTrue(rec.frontFacing);

        // Ray missing the triangle
        Ray missRay = new Ray(new Vector3(2, 0, 0), new Vector3(0, 0, -1));
        assertFalse(triangle.intersect(missRay, rec));

        // BBox calculation and padding
        BBox bounds = triangle.getBounds();
        assertEquals(-1.0, bounds.min.x, EPSILON);
        assertEquals(-1.0, bounds.min.y, EPSILON);
        assertEquals(-5.0 - 1e-4, bounds.min.z, EPSILON); // Z-axis is flat, so padded
        assertEquals(1.0, bounds.max.x, EPSILON);
        assertEquals(1.0, bounds.max.y, EPSILON);
        assertEquals(-5.0 + 1e-4, bounds.max.z, EPSILON);
    }

    @Test
    public void testOBJLoaderAndTriangleMesh() throws IOException {
        String objData = 
            "# Simple OBJ test cube\n" +
            "v 0.0 0.0 0.0\n" +
            "v 1.0 0.0 0.0\n" +
            "v 1.0 1.0 0.0\n" +
            "v 0.0 1.0 0.0\n" +
            "vt 0.0 0.0\n" +
            "vt 1.0 0.0\n" +
            "vt 1.0 1.0\n" +
            "vt 0.0 1.0\n" +
            "vn 0.0 0.0 1.0\n" +
            "f 1/1/1 2/2/1 3/3/1 4/4/1\n"; // Quad face (should be triangulated into 2 triangles)
        
        InputStream in = new ByteArrayInputStream(objData.getBytes());
        TriangleMesh mesh = OBJLoader.load(in);

        // Verify vertex count and arrays
        assertEquals(4, mesh.vertices.length);
        assertEquals(4, mesh.uvs.length);
        assertEquals(4, mesh.normals.length);
        
        // Quad triangulation: 4 vertices should produce 2 triangles, i.e., 6 indices
        assertEquals(6, mesh.indices.length);
        
        // Triangle 0: 0, 1, 2
        assertEquals(0, mesh.indices[0]);
        assertEquals(1, mesh.indices[1]);
        assertEquals(2, mesh.indices[2]);

        // Triangle 1: 0, 2, 3
        assertEquals(0, mesh.indices[3]);
        assertEquals(2, mesh.indices[4]);
        assertEquals(3, mesh.indices[5]);

        List<Shape> shapes = mesh.getTriangles();
        assertEquals(2, shapes.size());

        // Test intersection with one of the mesh triangles
        Ray ray = new Ray(new Vector3(0.25, 0.25, 1.0), new Vector3(0, 0, -1));
        HitRecord rec = new HitRecord();
        assertTrue(shapes.get(0).intersect(ray, rec));
        assertEquals(1.0, rec.t, EPSILON);
        assertVectorEquals(new Vector3(0.25, 0.25, 0.0), rec.point);
        assertVectorEquals(new Vector3(0.0, 0.0, 1.0), rec.normal);
    }
}
