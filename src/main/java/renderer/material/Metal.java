package renderer.material;

import renderer.math.Vector3;
import renderer.math.MathUtils;
import renderer.geometry.HitRecord;
import renderer.math.Ray;

/**
 * Metal (specular reflective) material.
 *
 * Physics:
 * Metal surfaces reflect light specularly (mirror-like), but with roughness.
 * Without roughness: perfect mirror reflection.
 * With roughness: small perturbations to the reflection direction.
 *
 * BRDF for perfectly smooth metal:
 *   f(l, v) = δ(v - reflect(l))
 *
 * With roughness, we perturb the reflection direction by
 * sampling from a hemisphere with radius proportional to roughness.
 *
 * Parameters:
 * - albedo: surface reflectivity (usually high for metals, 0.5-1.0)
 * - roughness: surface roughness (0 = mirror, 1 = very rough)
 *
 * Energy Conservation:
 * Metal reflects all light (albedo = 1 for perfect conductor).
 * Rougher metals may have slightly lower albedo due to internal scattering.
 */
public class Metal extends Material {

    private final Vector3 albedo;
    private final double roughness;  // 0 = smooth, 1 = very rough

    /**
     * Create a metal material.
     *
     * @param albedo the reflectivity
     * @param roughness surface roughness (0 to 1)
     */
    public Metal(Vector3 albedo, double roughness) {
        this.albedo = albedo;
        // Clamp roughness to [0, 1]
        this.roughness = Math.max(0.0, Math.min(1.0, roughness));
    }

    /**
     * Scatter a ray at this metal surface.
     *
     * Physics:
     * 1. Compute perfect mirror reflection: r = d - 2(d·n)n
     * 2. Add roughness by perturbing direction with random hemisphere sample
     * 3. If reflected ray is below surface, absorb (return no scatter)
     *
     * The perturbation simulates surface roughness. Rougher surfaces
     * scatter light in many directions, while smooth metals reflect
     * nearly in the mirror direction.
     *
     * @param incomingRay the incoming ray
     * @param hitRecord the intersection information
     * @return scatter record with metal reflectivity as attenuation
     */
    @Override
    public ScatterRecord scatter(Ray incomingRay, HitRecord hitRecord) {
        Vector3 incidentDir = incomingRay.direction().normalized();
        Vector3 normal = hitRecord.normal();

        // Perfect mirror reflection: r = d - 2(d·n)n
        Vector3 reflected = reflect(incidentDir, normal);

        // Add roughness: perturb reflection with random hemisphere sample
        // Larger roughness = larger perturbation
        Vector3 roughnessDir = sampleUnitHemisphere(normal).scaled(roughness);
        Vector3 scatterDirection = reflected.plus(roughnessDir).normalized();

        // Check if scattered ray is above the surface (into the hemisphere)
        // If below, the ray is absorbed (no scatter)
        if (scatterDirection.dot(normal) <= 0) {
            return new ScatterRecord(false, Vector3.BLACK, null);
        }

        Ray scattered = new Ray(
            hitRecord.point(),
            scatterDirection,
            0.0001
        );

        // Energy is partially absorbed by roughness; smoother = more reflection
        return new ScatterRecord(true, albedo, scattered);
    }

    /**
     * Reflect a direction about a normal.
     *
     * Mathematics:
     *   r = d - 2(d·n)n
     *
     * where:
     *   - d is the incident direction
     *   - n is the surface normal
     *   - r is the reflected direction
     *
     * @param d the incident direction (must be pointing toward surface)
     * @param n the surface normal
     * @return the reflected direction
     */
    private Vector3 reflect(Vector3 d, Vector3 n) {
        return d.minus(n.scaled(2.0 * d.dot(n)));
    }

    /**
     * Sample a random direction in the unit hemisphere above the normal.
     *
     * Uses random direction sampling (not cosine-weighted) for roughness.
     *
     * @param normal the surface normal
     * @return a random direction in the hemisphere
     */
    private Vector3 sampleUnitHemisphere(Vector3 normal) {
        // Generate random direction on unit sphere
        double u = Math.random();
        double v = Math.random();

        double theta = Math.acos(Math.sqrt(1.0 - u));
        double phi = 2.0 * Math.PI * v;

        Vector3 sphereDir = new Vector3(
            Math.sin(theta) * Math.cos(phi),
            Math.sin(theta) * Math.sin(phi),
            Math.cos(theta)
        );

        // Construct orthonormal basis
        Vector3 u_axis = MathUtils.getOrthogonal(normal).normalized();
        Vector3 v_axis = normal.cross(u_axis).normalized();

        // Transform to world space
        return u_axis.scaled(sphereDir.x())
                    .plus(v_axis.scaled(sphereDir.y()))
                    .plus(normal.scaled(sphereDir.z()))
                    .normalized();
    }
}
