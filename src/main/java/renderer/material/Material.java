package renderer.material;

import renderer.math.Ray;
import renderer.math.Vector3;
import renderer.geometry.HitRecord;

/**
 * Base interface for all physically based materials in the renderer.
 */
public interface Material {
    
    /**
     * Default fallback material (matte black absorber).
     */
    public static final Material DEFAULT = new Material() {
        @Override
        public boolean scatter(Ray rayIn, HitRecord rec, ScatterRecord srec) {
            return false;
        }
    };

    /**
     * Computes the scattering direction and attenuation for an incoming ray hitting a surface.
     * 
     * @param rayIn The incoming ray.
     * @param rec The HitRecord at the intersection point.
     * @param srec The ScatterRecord to populate with the scattered ray and attenuation.
     * @return true if the ray scatters, false if it is absorbed.
     */
    boolean scatter(Ray rayIn, HitRecord rec, ScatterRecord srec);

    /**
     * Computes the emitted light color and intensity at a given surface point.
     * Non-emissive materials return Vector3.ZERO by default.
     * 
     * @param u The texture coordinate u.
     * @param v The texture coordinate v.
     * @param p The intersection point.
     * @return The emitted light intensity (RGB).
     */
    default Vector3 emitted(double u, double v, Vector3 p) {
        return Vector3.ZERO;
    }
}
