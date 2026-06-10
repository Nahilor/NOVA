package renderer.material;

import renderer.math.Ray;
import renderer.math.Vector3;

/**
 * Stores the result of a ray scattering event.
 * Using a mutable record avoids memory allocations during path tracing.
 */
public class ScatterRecord {
    public Ray scatteredRay;
    public Vector3 attenuation;
}
