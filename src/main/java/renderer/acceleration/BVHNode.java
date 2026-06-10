package renderer.acceleration;

import java.util.List;
import renderer.math.BBox;
import renderer.geometry.Shape;

/**
 * Represents a node in the Bounding Volume Hierarchy (BVH) tree.
 * Can be an internal node (having left/right children) or a leaf node (having primitive shapes).
 */
public final class BVHNode {
    public final BBox bounds;
    public final BVHNode left;
    public final BVHNode right;
    public final List<Shape> primitives; // Null for internal nodes

    /**
     * Constructs an internal BVH node.
     * 
     * @param bounds The bounding box enclosing all shapes in both children.
     * @param left The left child node.
     * @param right The right child node.
     */
    public BVHNode(BBox bounds, BVHNode left, BVHNode right) {
        this.bounds = bounds;
        this.left = left;
        this.right = right;
        this.primitives = null;
    }

    /**
     * Constructs a leaf BVH node.
     * 
     * @param bounds The bounding box enclosing all shapes in this leaf.
     * @param primitives The list of shapes contained in this leaf.
     */
    public BVHNode(BBox bounds, List<Shape> primitives) {
        this.bounds = bounds;
        this.left = null;
        this.right = null;
        this.primitives = primitives;
    }

    /**
     * Checks if this node is a leaf node.
     * 
     * @return true if this is a leaf node, false otherwise.
     */
    public boolean isLeaf() {
        return primitives != null;
    }
}
