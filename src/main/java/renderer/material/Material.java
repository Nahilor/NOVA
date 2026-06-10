package renderer.material;

import renderer.math.Vector3;
import renderer.geometry.HitRecord;
import renderer.math.Ray;

/**
 * Abstract base class for all materials.
 * Defines the interface for computing scatter rays and emitted light.
 *
 * Materials are responsible for:
 * - Computing BRDF response (how light scatters at a surface)
 * - Determining whether a ray is scattered or absorbed
 * - Computing emitted light (for emissive materials)
 *
 * Design: Template Method pattern allows subclasses to implement
 * specific material behavior while maintaining consistent interface.
 */
public abstract class Material {

    /**
     * Record for scatter computation result.
     *
     * Contains:
     * - scattered: whether the ray was scattered at this surface
     * - attenuation: the color/energy attenuation of the scattered ray
     * - scattered_ray: the direction and origin of the scattered ray
     */
    public static record ScatterRecord(
        boolean scattered,
        Vector3 attenuation,
        Ray scatteredRay
    ) {}

    /**
     * Computes how a ray scatters at this material's surface.
     *
     * Parameters:
     * - incomingRay: the ray hitting the surface
     * - hitRecord: intersection information (position, normal, texture coords)
     *
     * Returns:
     * - ScatterRecord with scatter result, attenuation (BRDF), and scattered ray
     *
     * Physics: This implements the BRDF evaluation for Monte Carlo path tracing.
     * The attenuation represents f(l, v) where:
     *   - f is the BRDF (Bidirectional Reflectance Distribution Function)
     *   - l is the incoming light direction
     *   - v is the view direction
     *
     * For importance sampling, the scattered ray should be sampled from
     * a distribution that minimizes variance (typically cosine-weighted hemisphere).
     */
    public abstract ScatterRecord scatter(Ray incomingRay, HitRecord hitRecord);

    /**
     * Computes emitted light from this surface.
     *
     * Parameters:
     * - u, v: texture coordinates at the hit point
     *
     * Returns:
     * - The radiance emitted from this surface in the outgoing direction
     *
     * Physics: This is the Le term in the rendering equation.
     * For most materials this will be black (0, 0, 0).
     * Only emissive materials return non-zero values.
     */
    public Vector3 emitted(double u, double v) {
        return Vector3.BLACK;
    }
}
