package renderer.acceleration;

import java.util.ArrayList;
import java.util.List;
import renderer.geometry.HitRecord;
import renderer.geometry.Shape;
import renderer.math.BBox;
import renderer.math.Ray;

/**
 * Bounding Volume Hierarchy (BVH) acceleration structure.
 * Implements the Shape interface, allowing it to be nested inside other shapes or BVHs.
 */
public final class BVH implements Shape {
    public final BVHNode root;

    public BVH(List<Shape> primitives) {
        // Create a copy of the list to allow sorting and partitioning during build
        this.root = build(new ArrayList<>(primitives));
    }

    @Override
    public boolean intersect(Ray ray, HitRecord rec) {
        if (root == null) return false;

        // Fast rejection: check if ray hits the root bounding box
        if (root.bounds.intersect(ray) == Double.POSITIVE_INFINITY) {
            return false;
        }

        boolean hit = false;

        // Fixed-size traversal stack (size 64 is more than enough for 2^64 primitives)
        BVHNode[] stack = new BVHNode[64];
        int stackPtr = 0;
        stack[stackPtr++] = root;

        while (stackPtr > 0) {
            BVHNode node = stack[--stackPtr];

            if (node.isLeaf()) {
                // Intersect ray with all primitives in the leaf node
                for (Shape prim : node.primitives) {
                    if (prim.intersect(ray, rec)) {
                        hit = true;
                        // Dynamically narrow ray.tMax to enable aggressive pruning of remaining subtrees
                        ray.tMax = rec.t;
                    }
                }
            } else {
                // Intersect ray with left and right children's bounding boxes
                double tLeft = node.left.bounds.intersect(ray);
                double tRight = node.right.bounds.intersect(ray);

                boolean hitLeft = tLeft < ray.tMax;
                boolean hitRight = tRight < ray.tMax;

                if (hitLeft && hitRight) {
                    // Push the further child first, then the closer child,
                    // so the closer child is popped and traversed first.
                    if (tLeft < tRight) {
                        stack[stackPtr++] = node.right;
                        stack[stackPtr++] = node.left;
                    } else {
                        stack[stackPtr++] = node.left;
                        stack[stackPtr++] = node.right;
                    }
                } else if (hitLeft) {
                    stack[stackPtr++] = node.left;
                } else if (hitRight) {
                    stack[stackPtr++] = node.right;
                }
            }
        }

        return hit;
    }

    @Override
    public BBox getBounds() {
        return root != null ? root.bounds : new BBox();
    }

    /**
     * Recursive builder for bucketed Surface Area Heuristic (SAH) BVH.
     */
    private BVHNode build(List<Shape> prims) {
        if (prims.isEmpty()) {
            return null;
        }

        // Compute AABB of all shapes in the list
        BBox bounds = new BBox();
        for (Shape p : prims) {
            bounds = bounds.union(p.getBounds());
        }

        int n = prims.size();
        // Leaf threshold: 4 primitives
        if (n <= 4) {
            return new BVHNode(bounds, prims);
        }

        // Determine centroid bounds
        double minCentroid = Double.POSITIVE_INFINITY;
        double maxCentroid = Double.NEGATIVE_INFINITY;
        int axis = bounds.longestAxis();

        for (Shape p : prims) {
            double cent = getCentroidCoord(p, axis);
            minCentroid = Math.min(minCentroid, cent);
            maxCentroid = Math.max(maxCentroid, cent);
        }

        // If all centroids are at the same coordinate, we cannot partition them.
        // Fall back to equal-counts sorting split.
        if (maxCentroid - minCentroid < 1e-6) {
            return buildEqualCountsSplit(prims, axis, bounds);
        }

        // Bucketed SAH setup (12 buckets is a typical standard)
        final int BUCKETS = 12;
        BBox[] bucketBounds = new BBox[BUCKETS];
        int[] bucketCount = new int[BUCKETS];
        for (int i = 0; i < BUCKETS; i++) {
            bucketBounds[i] = new BBox();
        }

        for (Shape p : prims) {
            double cent = getCentroidCoord(p, axis);
            int b = (int) (BUCKETS * (cent - minCentroid) / (maxCentroid - minCentroid));
            if (b < 0) b = 0;
            if (b >= BUCKETS) b = BUCKETS - 1;

            bucketBounds[b] = bucketBounds[b].union(p.getBounds());
            bucketCount[b]++;
        }

        // Evaluate SAH cost for each bucket split point
        double minCost = Double.POSITIVE_INFINITY;
        int bestSplitBucket = -1;
        double parentArea = bounds.surfaceArea();
        double invParentArea = parentArea <= 0.0 ? 1e-8 : 1.0 / parentArea;

        for (int i = 0; i < BUCKETS - 1; i++) {
            BBox leftBox = new BBox();
            int leftCount = 0;
            for (int j = 0; j <= i; j++) {
                leftBox = leftBox.union(bucketBounds[j]);
                leftCount += bucketCount[j];
            }

            BBox rightBox = new BBox();
            int rightCount = 0;
            for (int j = i + 1; j < BUCKETS; j++) {
                rightBox = rightBox.union(bucketBounds[j]);
                rightCount += bucketCount[j];
            }

            // Cost: traversal_cost (0.125) + intersection_cost * relative_area * primitives_count
            double cost = 0.125 + (leftBox.surfaceArea() * leftCount + rightBox.surfaceArea() * rightCount) * invParentArea;
            if (cost < minCost) {
                minCost = cost;
                bestSplitBucket = i;
            }
        }

        double leafCost = n;
        // Split if cost is lower than creating a leaf, or if we exceed max leaf size of 16
        if (minCost < leafCost || n > 16) {
            List<Shape> leftPrims = new ArrayList<>();
            List<Shape> rightPrims = new ArrayList<>();
            final int splitB = bestSplitBucket;
            final double minCent = minCentroid;
            final double maxCent = maxCentroid;

            for (Shape p : prims) {
                double cent = getCentroidCoord(p, axis);
                int b = (int) (BUCKETS * (cent - minCent) / (maxCent - minCent));
                if (b < 0) b = 0;
                if (b >= BUCKETS) b = BUCKETS - 1;

                if (b <= splitB) {
                    leftPrims.add(p);
                } else {
                    rightPrims.add(p);
                }
            }

            // Guard: if either side is empty (due to floating point precision or layout),
            // fall back to a simple equal-counts split
            if (leftPrims.isEmpty() || rightPrims.isEmpty()) {
                return buildEqualCountsSplit(prims, axis, bounds);
            }

            BVHNode leftNode = build(leftPrims);
            BVHNode rightNode = build(rightPrims);
            return new BVHNode(bounds, leftNode, rightNode);
        } else {
            return new BVHNode(bounds, prims);
        }
    }

    private BVHNode buildEqualCountsSplit(List<Shape> prims, int axis, BBox bounds) {
        int mid = prims.size() / 2;
        prims.sort((a, b) -> Double.compare(getCentroidCoord(a, axis), getCentroidCoord(b, axis)));

        List<Shape> leftPrims = new ArrayList<>(prims.subList(0, mid));
        List<Shape> rightPrims = new ArrayList<>(prims.subList(mid, prims.size()));

        BVHNode leftNode = build(leftPrims);
        BVHNode rightNode = build(rightPrims);
        return new BVHNode(bounds, leftNode, rightNode);
    }

    private double getCentroidCoord(Shape p, int axis) {
        return p.getBounds().centroid().get(axis);
    }
}
