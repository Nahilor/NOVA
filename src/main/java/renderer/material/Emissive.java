package renderer.material;

import renderer.math.Vector3;
import renderer.geometry.HitRecord;
import renderer.math.Ray;

/**
 * Emissive material - produces light.
 *
 * Physics:
 * An emissive material produces light through photon emission (e.g., lights, LEDs).
 * These surfaces don't scatter incoming light; they only emit.
 *
 * In the rendering equation:
 *   Lo(x, ω) = Le(x, ω) + ∫ f(l,v) * Li(x, l) * cos(θ) dΩ
 *
 * For a pure emissive material:
 *   Lo(x, ω) = Le(x, ω)
 *
 * Emissive surfaces are crucial for:
 * - Direct illumination (area lights)
 * - Global illumination (seeds the path tracing algorithm)
 *
 * Parameters:
 * - emittance: the radiance emitted in all directions
 */
public class Emissive extends Material {

    private final Vector3 emittance;  // Radiance (power per unit area per solid angle)

    /**
     * Create an emissive material.
     *
     * @param emittance the radiance emitted (e.g., (1.0, 1.0, 1.0) for white light)
     */
    public Emissive(Vector3 emittance) {
        this.emittance = emittance;
    }

    /**
     * Emissive surfaces don't scatter light; they only emit.
     *
     * @param incomingRay the incoming ray (ignored for emissive)
     * @param hitRecord the intersection information
     * @return no scatter, just emission
     */
    @Override
    public ScatterRecord scatter(Ray incomingRay, HitRecord hitRecord) {
        // Don't scatter - emissive surfaces only emit
        return new ScatterRecord(false, Vector3.BLACK, null);
    }

    /**
     * Return the emitted radiance.
     *
     * Physics:
     * The emitted radiance is independent of position (u, v) for simple materials.
     * More complex materials could have spatially varying emittance (patterns, etc.).
     *
     * @param u texture coordinate (unused for uniform emittance)
     * @param v texture coordinate (unused for uniform emittance)
     * @return the emitted radiance
     */
    @Override
    public Vector3 emitted(double u, double v) {
        return emittance;
    }
}
