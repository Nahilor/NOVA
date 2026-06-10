package renderer.math;

/**
 * An immutable 4x4 matrix in row-major order.
 * Used for affine transformations in 3D graphics.
 */
public final class Matrix4 {
    // Array of 16 elements in row-major order:
    // [ m00, m01, m02, m03,
    //   m10, m11, m12, m13,
    //   m20, m21, m22, m23,
    //   m30, m31, m32, m33 ]
    public final double[] m;

    public static final Matrix4 IDENTITY = new Matrix4(new double[] {
        1, 0, 0, 0,
        0, 1, 0, 0,
        0, 0, 1, 0,
        0, 0, 0, 1
    });

    public Matrix4(double[] values) {
        if (values.length != 16) {
            throw new IllegalArgumentException("Matrix4 requires exactly 16 values");
        }
        this.m = values.clone();
    }

    public Matrix4(
        double m00, double m01, double m02, double m03,
        double m10, double m11, double m12, double m13,
        double m20, double m21, double m22, double m23,
        double m30, double m31, double m32, double m33
    ) {
        this.m = new double[] {
            m00, m01, m02, m03,
            m10, m11, m12, m13,
            m20, m21, m22, m23,
            m30, m31, m32, m33
        };
    }

    public double get(int row, int col) {
        return m[row * 4 + col];
    }

    public Matrix4 mul(Matrix4 other) {
        double[] result = new double[16];
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                double sum = 0.0;
                for (int k = 0; k < 4; k++) {
                    sum += this.m[row * 4 + k] * other.m[k * 4 + col];
                }
                result[row * 4 + col] = sum;
            }
        }
        return new Matrix4(result);
    }

    /**
     * Transforms a 3D point (w = 1).
     * Applies translation, scaling, rotation, and divides by the projected w.
     */
    public Vector3 transformPoint(Vector3 p) {
        double x = m[0] * p.x + m[1] * p.y + m[2] * p.z + m[3];
        double y = m[4] * p.x + m[5] * p.y + m[6] * p.z + m[7];
        double z = m[8] * p.x + m[9] * p.y + m[10] * p.z + m[11];
        double w = m[12] * p.x + m[13] * p.y + m[14] * p.z + m[15];

        if (w != 1.0 && w != 0.0) {
            double invW = 1.0 / w;
            return new Vector3(x * invW, y * invW, z * invW);
        }
        return new Vector3(x, y, z);
    }

    /**
     * Transforms a 3D direction vector (w = 0).
     * Translation components of the matrix are ignored.
     */
    public Vector3 transformDirection(Vector3 d) {
        double x = m[0] * d.x + m[1] * d.y + m[2] * d.z;
        double y = m[4] * d.x + m[5] * d.y + m[6] * d.z;
        double z = m[8] * d.x + m[9] * d.y + m[10] * d.z;
        return new Vector3(x, y, z);
    }

    public Matrix4 transpose() {
        return new Matrix4(
            m[0], m[4], m[8], m[12],
            m[1], m[5], m[9], m[13],
            m[2], m[6], m[10], m[14],
            m[3], m[7], m[11], m[15]
        );
    }

    /**
     * Computes the inverse of this matrix.
     * Uses Cramer's rule / analytical cofactors for numerical stability and performance.
     * Throws ArithmeticException if the matrix is singular (determinant is 0).
     */
    public Matrix4 invert() {
        double[] inv = new double[16];

        inv[0] = m[5]  * m[10] * m[15] - 
                 m[5]  * m[11] * m[14] - 
                 m[9]  * m[6]  * m[15] + 
                 m[9]  * m[7]  * m[14] +
                 m[13] * m[6]  * m[11] - 
                 m[13] * m[7]  * m[10];

        inv[4] = -m[4]  * m[10] * m[15] + 
                  m[4]  * m[11] * m[14] + 
                  m[8]  * m[6]  * m[15] - 
                  m[8]  * m[7]  * m[14] - 
                  m[12] * m[6]  * m[11] + 
                  m[12] * m[7]  * m[10];

        inv[8] = m[4]  * m[9]  * m[15] - 
                 m[4]  * m[11] * m[13] - 
                 m[8]  * m[5]  * m[15] + 
                 m[8]  * m[7]  * m[13] + 
                 m[12] * m[5]  * m[11] - 
                 m[12] * m[7]  * m[9];

        inv[12] = -m[4]  * m[9]  * m[14] + 
                   m[4]  * m[10] * m[13] + 
                   m[8]  * m[5]  * m[14] - 
                   m[8]  * m[6]  * m[13] - 
                   m[12] * m[5]  * m[10] + 
                   m[12] * m[6]  * m[9];

        inv[1] = -m[1]  * m[10] * m[15] + 
                  m[1]  * m[11] * m[14] + 
                  m[9]  * m[2]  * m[15] - 
                  m[9]  * m[3]  * m[14] - 
                  m[13] * m[2]  * m[11] + 
                  m[13] * m[3]  * m[10];

        inv[5] = m[0]  * m[10] * m[15] - 
                 m[0]  * m[11] * m[14] - 
                 m[8]  * m[2]  * m[15] + 
                 m[8]  * m[3]  * m[14] + 
                 m[12] * m[2]  * m[11] - 
                 m[12] * m[3]  * m[10];

        inv[9] = -m[0]  * m[9]  * m[15] + 
                  m[0]  * m[11] * m[13] + 
                  m[8]  * m[1]  * m[15] - 
                  m[8]  * m[3]  * m[13] - 
                  m[12] * m[1]  * m[11] + 
                  m[12] * m[3]  * m[9];

        inv[13] = m[0]  * m[9]  * m[14] - 
                  m[0]  * m[10] * m[13] - 
                  m[8]  * m[1]  * m[14] + 
                  m[8]  * m[2]  * m[13] + 
                  m[12] * m[1]  * m[10] - 
                  m[12] * m[2]  * m[9];

        inv[2] = m[1]  * m[6]  * m[15] - 
                 m[1]  * m[7]  * m[14] - 
                 m[5]  * m[2]  * m[15] + 
                 m[5]  * m[3]  * m[14] + 
                 m[13] * m[2]  * m[7] - 
                 m[13] * m[3]  * m[6];

        inv[6] = -m[0]  * m[6]  * m[15] + 
                  m[0]  * m[7]  * m[14] + 
                  m[4]  * m[2]  * m[15] - 
                  m[4]  * m[3]  * m[14] - 
                  m[12] * m[2]  * m[7] + 
                  m[12] * m[3]  * m[6];

        inv[10] = m[0]  * m[5]  * m[15] - 
                  m[0]  * m[7]  * m[13] - 
                  m[4]  * m[1]  * m[15] + 
                  m[4]  * m[3]  * m[13] + 
                  m[12] * m[1]  * m[7] - 
                  m[12] * m[3]  * m[5];

        inv[14] = -m[0]  * m[5]  * m[14] + 
                   m[0]  * m[6]  * m[13] + 
                   m[4]  * m[1]  * m[14] - 
                   m[4]  * m[2]  * m[13] - 
                   m[12] * m[1]  * m[6] + 
                   m[12] * m[2]  * m[5];

        inv[3] = -m[1]  * m[6]  * m[11] + 
                  m[1]  * m[7]  * m[10] + 
                  m[5]  * m[2]  * m[11] - 
                  m[5]  * m[3]  * m[10] - 
                  m[9]  * m[2]  * m[7] + 
                  m[9]  * m[3]  * m[6];

        inv[7] = m[0]  * m[6]  * m[11] - 
                 m[0]  * m[7]  * m[10] - 
                 m[4]  * m[2]  * m[11] + 
                 m[4]  * m[3]  * m[10] + 
                 m[8]  * m[2]  * m[7] - 
                 m[8]  * m[3]  * m[6];

        inv[11] = -m[0]  * m[5]  * m[11] + 
                   m[0]  * m[7]  * m[9] + 
                   m[4]  * m[1]  * m[11] - 
                   m[4]  * m[3]  * m[9] - 
                   m[8]  * m[1]  * m[7] + 
                   m[8]  * m[3]  * m[5];

        inv[15] = m[0]  * m[5]  * m[10] - 
                  m[0]  * m[6]  * m[9] - 
                  m[4]  * m[1]  * m[10] + 
                  m[4]  * m[2]  * m[9] + 
                  m[8]  * m[1]  * m[6] - 
                  m[8]  * m[2]  * m[5];

        double det = m[0] * inv[0] + m[1] * inv[4] + m[2] * inv[8] + m[3] * inv[12];

        if (det == 0.0) {
            throw new ArithmeticException("Matrix is singular and cannot be inverted");
        }

        double invDet = 1.0 / det;
        for (int i = 0; i < 16; i++) {
            inv[i] *= invDet;
        }

        return new Matrix4(inv);
    }

    // --- Static Generator Factories ---

    public static Matrix4 translation(double tx, double ty, double tz) {
        return new Matrix4(
            1, 0, 0, tx,
            0, 1, 0, ty,
            0, 0, 1, tz,
            0, 0, 0, 1
        );
    }

    public static Matrix4 translation(Vector3 t) {
        return translation(t.x, t.y, t.z);
    }

    public static Matrix4 scale(double sx, double sy, double sz) {
        return new Matrix4(
            sx, 0, 0, 0,
            0, sy, 0, 0,
            0, 0, sz, 0,
            0, 0, 0, 1
        );
    }

    public static Matrix4 scale(Vector3 s) {
        return scale(s.x, s.y, s.z);
    }

    public static Matrix4 rotationX(double angleRad) {
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        return new Matrix4(
            1,   0,    0, 0,
            0, cos, -sin, 0,
            0, sin,  cos, 0,
            0,   0,    0, 1
        );
    }

    public static Matrix4 rotationY(double angleRad) {
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        return new Matrix4(
             cos, 0, sin, 0,
               0, 1,   0, 0,
            -sin, 0, cos, 0,
               0, 0,   0, 1
        );
    }

    public static Matrix4 rotationZ(double angleRad) {
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        return new Matrix4(
            cos, -sin, 0, 0,
            sin,  cos, 0, 0,
              0,    0, 1, 0,
              0,    0, 0, 1
        );
    }

    /**
     * Builds a Camera-to-World transformation matrix.
     * Matches the standard right-handed graphics conventions where:
     * - Camera looks along the -Z axis (forward)
     * - Camera up is along +Y
     * - Camera right is along +X
     */
    public static Matrix4 lookAt(Vector3 eye, Vector3 target, Vector3 up) {
        Vector3 forward = target.sub(eye).normalize(); // -z direction in camera space
        Vector3 right = forward.cross(up).normalize(); // +x direction in camera space
        Vector3 trueUp = right.cross(forward).normalize(); // +y direction in camera space

        // Camera to World transformation
        return new Matrix4(
            right.x, trueUp.x, -forward.x, eye.x,
            right.y, trueUp.y, -forward.y, eye.y,
            right.z, trueUp.z, -forward.z, eye.z,
            0.0,     0.0,      0.0,        1.0
        );
    }
}
