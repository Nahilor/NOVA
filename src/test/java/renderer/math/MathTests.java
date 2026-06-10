package renderer.math;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MathTests {

    private static final double EPSILON = 1e-6;

    @Test
    public void testVector2Basic() {
        Vector2 v1 = new Vector2(3.0, 4.0);
        Vector2 v2 = new Vector2(1.0, 2.0);

        assertEquals(new Vector2(4.0, 6.0), v1.add(v2));
        assertEquals(new Vector2(2.0, 2.0), v1.sub(v2));
        assertEquals(new Vector2(6.0, 8.0), v1.mul(2.0));
        assertEquals(new Vector2(1.5, 2.0), v1.div(2.0));
        assertEquals(11.0, v1.dot(v2), EPSILON);
        assertEquals(25.0, v1.lengthSquared(), EPSILON);
        assertEquals(5.0, v1.length(), EPSILON);

        Vector2 norm = v1.normalize();
        assertEquals(0.6, norm.x, EPSILON);
        assertEquals(0.8, norm.y, EPSILON);
        assertEquals(1.0, norm.length(), EPSILON);
        
        assertThrows(ArithmeticException.class, () -> v1.div(0.0));
    }

    @Test
    public void testVector3Basic() {
        Vector3 v1 = new Vector3(1.0, 2.0, 3.0);
        Vector3 v2 = new Vector3(4.0, 5.0, 6.0);

        assertEquals(new Vector3(5.0, 7.0, 9.0), v1.add(v2));
        assertEquals(new Vector3(-3.0, -3.0, -3.0), v1.sub(v2));
        assertEquals(new Vector3(2.0, 4.0, 6.0), v1.mul(2.0));
        assertEquals(new Vector3(4.0, 10.0, 18.0), v1.mul(v2));
        assertEquals(new Vector3(0.5, 1.0, 1.5), v1.div(2.0));
        assertEquals(32.0, v1.dot(v2), EPSILON);
        assertEquals(14.0, v1.lengthSquared(), EPSILON);
        assertEquals(Math.sqrt(14.0), v1.length(), EPSILON);

        Vector3 cross = v1.cross(v2);
        assertEquals(new Vector3(-3.0, 6.0, -3.0), cross);
        assertEquals(0.0, cross.dot(v1), EPSILON);
        assertEquals(0.0, cross.dot(v2), EPSILON);

        assertEquals(1.0, v1.get(0));
        assertEquals(2.0, v1.get(1));
        assertEquals(3.0, v1.get(2));
        assertThrows(IllegalArgumentException.class, () -> v1.get(3));
    }

    @Test
    public void testVector3ReflectRefract() {
        // Reflection test
        Vector3 in = new Vector3(1.0, -1.0, 0.0).normalize();
        Vector3 normal = new Vector3(0.0, 1.0, 0.0);
        Vector3 reflected = in.reflect(normal);
        // Should reflect upward
        assertEquals(new Vector3(1.0, 1.0, 0.0).normalize().x, reflected.x, EPSILON);
        assertEquals(new Vector3(1.0, 1.0, 0.0).normalize().y, reflected.y, EPSILON);
        assertEquals(0.0, reflected.z, EPSILON);

        // Refraction test (air to glass, eta = 1.0 / 1.5)
        Vector3 refractNormal = new Vector3(0.0, 1.0, 0.0);
        Vector3 refracted = in.refract(refractNormal, 1.0 / 1.5);
        assertNotNull(refracted);
        assertTrue(refracted.y < 0.0); // still going down
        
        // Total internal reflection test (glass to air, eta = 1.5 / 1.0, high angle)
        Vector3 inHighAngle = new Vector3(0.9, -0.1, 0.0).normalize();
        Vector3 tir = inHighAngle.refract(refractNormal, 1.5 / 1.0);
        assertNull(tir); // should be null due to total internal reflection
    }

    @Test
    public void testMatrix4Basic() {
        Matrix4 id = Matrix4.IDENTITY;
        Vector3 p = new Vector3(1.0, 2.0, 3.0);
        
        assertEquals(p, id.transformPoint(p));
        assertEquals(p, id.transformDirection(p));

        // Translation
        Matrix4 trans = Matrix4.translation(2.0, -3.0, 4.0);
        assertEquals(new Vector3(3.0, -1.0, 7.0), trans.transformPoint(p));
        assertEquals(p, trans.transformDirection(p)); // Translations should not affect directions

        // Scaling
        Matrix4 scale = Matrix4.scale(2.0, 0.5, 10.0);
        assertEquals(new Vector3(2.0, 1.0, 30.0), scale.transformPoint(p));
        assertEquals(new Vector3(2.0, 1.0, 30.0), scale.transformDirection(p));

        // Transpose
        Matrix4 tMat = new Matrix4(
            1, 2, 3, 4,
            5, 6, 7, 8,
            9, 10, 11, 12,
            13, 14, 15, 16
        );
        Matrix4 expectedT = new Matrix4(
            1, 5, 9, 13,
            2, 6, 10, 14,
            3, 7, 11, 15,
            4, 8, 12, 16
        );
        assertArrayEquals(expectedT.m, tMat.transpose().m, EPSILON);
    }

    @Test
    public void testMatrix4Invert() {
        Matrix4 mat = new Matrix4(
            1,  0,  2,  0,
            0,  3,  0,  4,
            5,  0,  6,  0,
            0,  7,  0,  8
        );
        
        Matrix4 inv = mat.invert();
        Matrix4 product = mat.mul(inv);
        
        assertArrayEquals(Matrix4.IDENTITY.m, product.m, EPSILON);

        Matrix4 singular = new Matrix4(
            1, 2, 3, 4,
            2, 4, 6, 8, // Linearly dependent row
            0, 0, 1, 0,
            0, 0, 0, 1
        );
        assertThrows(ArithmeticException.class, singular::invert);
    }

    @Test
    public void testRay() {
        Vector3 origin = new Vector3(1.0, 2.0, 3.0);
        Vector3 direction = new Vector3(0.0, 0.0, 1.0);
        Ray ray = new Ray(origin, direction, 0.5, 10.0);

        assertEquals(origin, ray.origin);
        assertEquals(direction, ray.direction);
        assertEquals(0.5, ray.tMin);
        assertEquals(10.0, ray.tMax);

        assertEquals(new Vector3(1.0, 2.0, 8.0), ray.pointAt(5.0));
    }

    @Test
    public void testBBox() {
        BBox box1 = new BBox(new Vector3(-1, -1, -1), new Vector3(1, 1, 1));
        BBox box2 = new BBox(new Vector3(0, 0, 0), new Vector3(2, 2, 2));

        // Union
        BBox unionBox = box1.union(box2);
        assertEquals(new Vector3(-1, -1, -1), unionBox.min);
        assertEquals(new Vector3(2, 2, 2), unionBox.max);

        // Centroid & Longest Axis
        assertEquals(new Vector3(0.5, 0.5, 0.5), unionBox.centroid());
        BBox flatBox = new BBox(new Vector3(0, 0, 0), new Vector3(10, 2, 1));
        assertEquals(0, flatBox.longestAxis()); // X axis is longest (10)
        assertEquals(2.0 * (10*2 + 2*1 + 1*10), flatBox.surfaceArea(), EPSILON); // 2 * (20 + 2 + 10) = 64

        // Ray box intersection
        Ray rayHit = new Ray(new Vector3(0, 0, -5), new Vector3(0, 0, 1));
        double tHit = box1.intersect(rayHit);
        assertTrue(tHit < Double.POSITIVE_INFINITY);
        assertEquals(4.0, tHit, EPSILON); // enters box at z = -1 (origin.z is -5, distance to -1 is 4)

        Ray rayMiss = new Ray(new Vector3(0, 5, -5), new Vector3(0, 0, 1));
        double tMiss = box1.intersect(rayMiss);
        assertEquals(Double.POSITIVE_INFINITY, tMiss);

        // Ray from inside
        Ray rayInside = new Ray(new Vector3(0, 0, 0), new Vector3(0, 0, 1));
        double tInside = box1.intersect(rayInside);
        assertTrue(tInside < Double.POSITIVE_INFINITY);
        // tInside should clamp/bound to ray.tMin since origin is inside
        assertEquals(rayInside.tMin, tInside, EPSILON);

        // Ray parallel to box (division by zero)
        Ray rayParallel = new Ray(new Vector3(2, 0, -5), new Vector3(0, 0, 1)); // x=2 lies outside box [-1, 1]
        double tParallel = box1.intersect(rayParallel);
        assertEquals(Double.POSITIVE_INFINITY, tParallel); // should miss
    }

    @Test
    public void testMathUtils() {
        double deg = 90.0;
        double rad = MathUtils.degToRad(deg);
        assertEquals(Math.PI / 2.0, rad, EPSILON);
        assertEquals(deg, MathUtils.radToDeg(rad), EPSILON);

        assertEquals(5.0, MathUtils.clamp(10.0, 1.0, 5.0));
        assertEquals(1.0, MathUtils.clamp(-2.0, 1.0, 5.0));
        assertEquals(3.0, MathUtils.clamp(3.0, 1.0, 5.0));

        Vector3 normal = new Vector3(0, 1, 0);
        Vector3 localDir = new Vector3(0, 0, 1); // Z normal locally
        Vector3 aligned = MathUtils.alignToNormal(localDir, normal);
        // Aligned should equal normal (y=1)
        assertEquals(normal.x, aligned.x, EPSILON);
        assertEquals(normal.y, aligned.y, EPSILON);
        assertEquals(normal.z, aligned.z, EPSILON);
    }
}
