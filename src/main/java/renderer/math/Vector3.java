package renderer.math;

/**
 * An immutable 3D vector representing points, directions, normals, or RGB colors.
 */
public final class Vector3 {
    public final double x;
    public final double y;
    public final double z;

    public static final Vector3 ZERO = new Vector3(0, 0, 0);
    public static final Vector3 ONE = new Vector3(1, 1, 1);
    public static final Vector3 UP = new Vector3(0, 1, 0);
    public static final Vector3 RIGHT = new Vector3(1, 0, 0);
    public static final Vector3 FORWARD = new Vector3(0, 0, -1);

    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3 add(Vector3 v) {
        return new Vector3(this.x + v.x, this.y + v.y, this.z + v.z);
    }

    public Vector3 sub(Vector3 v) {
        return new Vector3(this.x - v.x, this.y - v.y, this.z - v.z);
    }

    public Vector3 mul(double s) {
        return new Vector3(this.x * s, this.y * s, this.z * s);
    }

    public Vector3 mul(Vector3 v) {
        return new Vector3(this.x * v.x, this.y * v.y, this.z * v.z);
    }

    public Vector3 div(double s) {
        if (s == 0.0) {
            throw new ArithmeticException("Division by zero");
        }
        double inv = 1.0 / s;
        return new Vector3(this.x * inv, this.y * inv, this.z * inv);
    }

    public double dot(Vector3 v) {
        return this.x * v.x + this.y * v.y + this.z * v.z;
    }

    public Vector3 cross(Vector3 v) {
        return new Vector3(
            this.y * v.z - this.z * v.y,
            this.z * v.x - this.x * v.z,
            this.x * v.y - this.y * v.x
        );
    }

    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public Vector3 normalize() {
        double len = length();
        if (len == 0.0) {
            return ZERO;
        }
        return div(len);
    }

    public Vector3 negate() {
        return new Vector3(-this.x, -this.y, -this.z);
    }

    public Vector3 min(Vector3 v) {
        return new Vector3(Math.min(this.x, v.x), Math.min(this.y, v.y), Math.min(this.z, v.z));
    }

    public Vector3 max(Vector3 v) {
        return new Vector3(Math.max(this.x, v.x), Math.max(this.y, v.y), Math.max(this.z, v.z));
    }

    /**
     * Reflects this vector (assumed to be the incoming ray direction pointing towards the hit point)
     * about a surface normal.
     * Formula: R = V - 2 * (V . N) * N
     */
    public Vector3 reflect(Vector3 normal) {
        return this.sub(normal.mul(2.0 * this.dot(normal)));
    }

    /**
     * Refracts this vector (assumed to be normalized and pointing towards the hit point)
     * about a surface normal using Snell's Law.
     * Returns null if total internal reflection occurs.
     */
    public Vector3 refract(Vector3 normal, double etaiOverEtat) {
        double cosTheta = Math.min(-this.dot(normal), 1.0);
        Vector3 rOutPerp = this.add(normal.mul(cosTheta)).mul(etaiOverEtat);
        double rOutParallelLengthSq = 1.0 - rOutPerp.lengthSquared();
        if (rOutParallelLengthSq < 0.0) {
            return null; // Total internal reflection
        }
        Vector3 rOutParallel = normal.mul(-Math.sqrt(rOutParallelLengthSq));
        return rOutPerp.add(rOutParallel);
    }

    public double get(int i) {
        return switch (i) {
            case 0 -> x;
            case 1 -> y;
            case 2 -> z;
            default -> throw new IllegalArgumentException("Index out of bounds: " + i);
        };
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Vector3)) return false;
        Vector3 other = (Vector3) obj;
        return Double.compare(this.x, other.x) == 0 &&
               Double.compare(this.y, other.y) == 0 &&
               Double.compare(this.z, other.z) == 0;
    }

    @Override
    public int hashCode() {
        long xBits = Double.doubleToLongBits(x);
        long yBits = Double.doubleToLongBits(y);
        long zBits = Double.doubleToLongBits(z);
        int result = Long.hashCode(xBits);
        result = 31 * result + Long.hashCode(yBits);
        result = 31 * result + Long.hashCode(zBits);
        return result;
    }

    @Override
    public String toString() {
        return String.format("Vector3(%.4f, %.4f, %.4f)", x, y, z);
    }
}
