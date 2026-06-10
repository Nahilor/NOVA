package renderer.material;

import renderer.math.Vector3;
import renderer.math.MathUtils;
import renderer.geometry.HitRecord;
import renderer.math.Ray;

/**
 * Dielectric (glass/transparent) material.
 *
 * Physics:
 * Dielectric materials allow light to pass through while being partially reflected.
 * Light refracts as it enters/exits the material according to Snell's Law.
 *
 * Key phenomena:
 * 1. Refraction: direction change as light enters denser medium
 * 2. Fresnel effect: more reflection at glancing angles
 * 3. Total internal reflection: reflection when exiting to less dense medium
 *
 * Snell's Law: n1 * sin(θ1) = n2 * sin(θ2)
 *   - n1, n2: refractive indices
 *   - θ1, θ2: angles from surface normal
 *
 * Fresnel approximation (Schlick's approximation):
 *   F(θ) = F0 + (1 - F0) * (1 - cos(θ))^5
 *   - F0 = ((n1 - n2) / (n1 + n2))^2
 *
 * where F(θ) is the probability of reflection.
 *
 * Parameters:
 * - refractiveIndex: ratio of refractive indices (e.g., 1.5 for glass)
 */
public class Dielectric extends Material {

    private final double refractiveIndex;  // n2 / n1 (usually > 1)

    /**
     * Create a dielectric material.
     *
     * Common refractive indices:
     * - Air: 1.0
     * - Water: 1.33
     * - Glass: 1.5
     * - Diamond: 2.42
     *
     * @param refractiveIndex the ratio n_material / n_outside
     */
    public Dielectric(double refractiveIndex) {
        this.refractiveIndex = refractiveIndex;
    }

    /**
     * Scatter a ray at this dielectric surface.
     *
     * Physics algorithm:
     * 1. Determine if ray is entering or exiting the material
     * 2. Compute refraction direction using Snell's law
     * 3. Check for total internal reflection
     * 4. Compute Fresnel probability and decide reflect vs refract
     *
     * @param incomingRay the incoming ray
     * @param hitRecord the intersection information
     * @return scatter record with refracted or reflected ray
     */
    @Override
    public ScatterRecord scatter(Ray incomingRay, HitRecord hitRecord) {
        Vector3 incidentDir = incomingRay.direction().normalized();
        Vector3 normal = hitRecord.normal();

        // Determine if ray is entering or exiting
        boolean rayEntering = incidentDir.dot(normal) < 0;
        Vector3 effectiveNormal = rayEntering ? normal : normal.negated();

        // Compute refractive index ratio
        double etaRatio = rayEntering ? (1.0 / refractiveIndex) : refractiveIndex;

        // Try to refract
        Vector3 refractedDir = refract(incidentDir, effectiveNormal, etaRatio);

        // Compute Fresnel probability for reflection
        double reflectProb = schlickFresnel(Math.abs(incidentDir.dot(normal)), etaRatio);

        // Stochastically choose between refraction and reflection
        Vector3 scatterDirection;
        if (refractedDir == null) {
            // Total internal reflection - must reflect
            scatterDirection = reflect(incidentDir, effectiveNormal);
        } else if (Math.random() < reflectProb) {
            // Fresnel reflection
            scatterDirection = reflect(incidentDir, effectiveNormal);
        } else {
            // Refraction
            scatterDirection = refractedDir;
        }

        Ray scattered = new Ray(
            hitRecord.point(),
            scatterDirection,
            0.0001
        );

        // Glass transmits all light (no energy loss, idealized)
        return new ScatterRecord(true, Vector3.WHITE, scattered);
    }

    /**
     * Refract a ray using Snell's law.
     *
     * Mathematical derivation:
     * Given incident direction d, normal n, and refractive index ratio eta:
     *
     * From Snell's law: n1*sin(θ1) = n2*sin(θ2)
     * eta = n1/n2, so: sin(θ2) = sin(θ1) / eta
     *
     * Refracted direction:
     *   t = eta*d + (eta*cos(θ1) - cos(θ2)) * n
     *
     * where:
     *   cos(θ1) = -d·n
     *   cos(θ2) = sqrt(1 - sin²(θ2)) = sqrt(1 - sin²(θ1)/eta²)
     *
     * Total internal reflection occurs when sin(θ2) > 1,
     * i.e., when (1 - sin²(θ1)/eta²) < 0.
     *
     * @param incidentDir incident direction (toward surface)
     * @param normal surface normal (pointing into incident medium)
     * @param eta refractive index ratio n1/n2
     * @return refracted direction, or null if total internal reflection
     */
    private Vector3 refract(Vector3 incidentDir, Vector3 normal, double eta) {
        double cosTheta1 = -incidentDir.dot(normal);

        // Compute sin²(θ2) using Snell's law
        double sinTheta2Sq = eta * eta * (1.0 - cosTheta1 * cosTheta1);

        // Total internal reflection check
        if (sinTheta2Sq > 1.0) {
            return null;  // Total internal reflection
        }

        // Compute cos(θ2)
        double cosTheta2 = Math.sqrt(1.0 - sinTheta2Sq);

        // Refracted ray formula
        Vector3 refracted = incidentDir.scaled(eta)
                .plus(normal.scaled(eta * cosTheta1 - cosTheta2));

        return refracted.normalized();
    }

    /**
     * Reflect a direction about a normal.
     *
     * @param d incident direction
     * @param n surface normal
     * @return reflected direction
     */
    private Vector3 reflect(Vector3 d, Vector3 n) {
        return d.minus(n.scaled(2.0 * d.dot(n)));
    }

    /**
     * Schlick's approximation of the Fresnel equations.
     *
     * The Fresnel effect describes how much light is reflected
     * versus transmitted at an interface.
     *
     * Schlick's approximation:
     *   F(θ) = F0 + (1 - F0) * (1 - cos(θ))^5
     *
     * where:
     *   F0 = ((n1 - n2) / (n1 + n2))^2
     *   θ is the angle from the surface normal
     *
     * This is an empirical fit to the full Fresnel equations.
     * It's much cheaper to compute and very accurate for rendering.
     *
     * For dielectrics:
     *   F0 = ((1 - n) / (1 + n))^2
     *
     * @param cosTheta cosine of angle from normal
     * @param eta refractive index ratio n1/n2
     * @return probability of reflection (0 to 1)
     */
    private double schlickFresnel(double cosTheta, double eta) {
        // Compute F0 for dielectric interface
        double f0 = (1.0 - eta) / (1.0 + eta);
        f0 = f0 * f0;

        // Apply Schlick's approximation
        double x = 1.0 - cosTheta;
        return f0 + (1.0 - f0) * x * x * x * x * x;
    }
}
