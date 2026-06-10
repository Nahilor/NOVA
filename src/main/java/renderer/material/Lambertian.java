package renderer.material;

import renderer.math.Vector3;
import renderer.math.MathUtils;
import renderer.geometry.HitRecord;
import renderer.math.Ray;

/**
 * Lambertian (diffuse) material.
 *
 * Physics:
 * Lambertian reflectance models ideal matte surfaces where light scatters
 * uniformly in all directions (hemispherical). The BRDF is:
 *
 *   f(l, v) = ρ / π
 *
 * where:
 *   - ρ is the albedo (0 to 1, surface reflectivity)
 *   - π accounts for the hemispherical solid angle integral
 *
 * This is "ideal" diffuse reflection. Real surfaces may have some directional
 * dependence (Fresnel effect), but Lambertian is a good approximation for
 * most matte surfaces like paper, cloth, walls.
 *
 * Sampling Strategy: Cosine-weighted hemisphere sampling
 * This is optimal for Lambertian surfaces as it samples directions proportional
 * to the BRDF, minimizing variance.
 *
 * PDF: p(ω) = cos(θ) / π
 *
 * Implementation uses rejection sampling to ensure numerical stability.
 */
public class Lambertian extends Material {

    private final Vector3 albedo;  // Surface color / reflectivity

    /**
     * Create a Lambertian material.
     *
     * @param albedo the surface reflectivity (0 to 1 in each channel)
     */
    public Lambertian(Vector3 albedo) {
        this.albedo = albedo;
    }

    /**
     * Scatter a ray at this Lambertian surface.
     *
     * Physics: Monte Carlo sampling from cosine-weighted hemisphere.
     *
     * The scattered ray direction is sampled using cosine-weighted
     * hemisphere sampling for importance sampling.
     *
     * Contribution to radiance:
     *   L = ρ/π * cos(θ) * (π/cos(θ)) * L_incident
     *     = ρ * L_incident
     *
     * The cos(θ)/π from BRDF cancels with the π/cos(θ) from the
     * cosine-weighted PDF, leaving just ρ * L_incident.
     * This is stored as 'attenuation'.
     *
     * @param incomingRay the incoming ray (used for debugging, not physics)
     * @param hitRecord the intersection information
     * @return scatter record with attenuation = albedo
     */
    @Override
    public ScatterRecord scatter(Ray incomingRay, HitRecord hitRecord) {
        // Sample direction from cosine-weighted hemisphere
        Vector3 scatterDirection = sampleCosineWeightedHemisphere(hitRecord.normal());

        // Small epsilon to avoid self-intersection errors
        Ray scattered = new Ray(
            hitRecord.point(),
            scatterDirection,
            0.0001
        );

        // Attenuation is the BRDF: f(l,v) = ρ/π
        // Combined with importance sampling PDF (π/cos(θ)),
        // the attenuation becomes just ρ (the albedo).
        return new ScatterRecord(true, albedo, scattered);
    }

    /**
     * Sample a direction from the cosine-weighted hemisphere.
     *
     * Algorithm:
     * 1. Generate random point in unit disk (r, θ)
     * 2. Construct orthonormal basis from surface normal
     * 3. Transform sampled direction to world space
     *
     * Mathematics:
     * In local coordinates (u, v, n):
     *   direction = sqrt(r) * (cos(θ) * u + sin(θ) * v) + sqrt(1-r) * n
     *
     * This gives:
     *   PDF(ω) = cos(θ_n) / π
     *
     * where θ_n is the angle from the normal.
     *
     * Rejection sampling: If z < 0, we're below the surface normal;
     * reject and resample. This ensures all samples are in the hemisphere.
     *
     * @param normal the surface normal
     * @return normalized direction in the cosine-weighted hemisphere
     */
    private Vector3 sampleCosineWeightedHemisphere(Vector3 normal) {
        // Generate random point in unit disk using concentric mapping
        double u = Math.random();
        double v = Math.random();

        // Radius and angle in disk
        double r = Math.sqrt(u);
        double theta = 2.0 * Math.PI * v;

        // Orthonormal basis: u and v vectors perpendicular to normal
        Vector3 u_axis = MathUtils.getOrthogonal(normal).normalized();
        Vector3 v_axis = normal.cross(u_axis).normalized();

        // Height of hemisphere
        double z = Math.sqrt(Math.max(0.0, 1.0 - u));

        // Construct sampled direction in local coordinates
        double x = r * Math.cos(theta);
        double y = r * Math.sin(theta);

        // Transform to world coordinates and normalize
        Vector3 sampledDir = new Vector3(
            x, y, z
        );

        // Transform from local basis to world space
        return u_axis.scaled(sampledDir.x())
                    .plus(v_axis.scaled(sampledDir.y()))
                    .plus(normal.scaled(sampledDir.z()))
                    .normalized();
    }
}
