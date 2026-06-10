package renderer.math;

/**
 * An immutable 2D vector representing points or directions in 2D space.
 * Commonly used for texture coordinates (u, v) or screen-space coordinates.
 */
public final class Vector2 {
    public final double x;
    public final double y;

    public static final Vector2 ZERO = new Vector2(0, 0);
    public static final Vector2 ONE = new Vector2(1, 1);

    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector2 add(Vector2 v) {
        return new Vector2(this.x + v.x, this.y + v.y);
    }

    public Vector2 sub(Vector2 v) {
        return new Vector2(this.x - v.x, this.y - v.y);
    }

    public Vector2 mul(double s) {
        return new Vector2(this.x * s, this.y * s);
    }

    public Vector2 div(double s) {
        if (s == 0.0) {
            throw new ArithmeticException("Division by zero");
        }
        double inv = 1.0 / s;
        return new Vector2(this.x * inv, this.y * inv);
    }

    public double dot(Vector2 v) {
        return this.x * v.x + this.y * v.y;
    }

    public double lengthSquared() {
        return x * x + y * y;
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public Vector2 normalize() {
        double len = length();
        if (len == 0.0) {
            return ZERO;
        }
        return div(len);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Vector2)) return false;
        Vector2 other = (Vector2) obj;
        return Double.compare(this.x, other.x) == 0 && Double.compare(this.y, other.y) == 0;
    }

    @Override
    public int hashCode() {
        long xBits = Double.doubleToLongBits(x);
        long yBits = Double.doubleToLongBits(y);
        return 31 * Long.hashCode(xBits) + Long.hashCode(yBits);
    }

    @Override
    public String toString() {
        return String.format("Vector2(%.4f, %.4f)", x, y);
    }
}
