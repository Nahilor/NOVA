package renderer;

import renderer.ui.PathTracerFrame;

/**
 * Main entry point for NOVA path tracer.
 *
 * This application provides:
 * - Interactive Swing GUI for path tracing visualization
 * - Real-time rendering with adjustable parameters
 * - Multiple material demonstration (diffuse, metal, glass, emissive)
 * - BVH acceleration for performance
 *
 * The GUI allows users to:
 * - Adjust samples per pixel (anti-aliasing quality)
 * - Adjust max recursion depth (indirect lighting quality)
 * - Re-render the scene with different parameters
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  NOVA Path Tracer - Interactive Demo");
        System.out.println("══════════════════════════════════════════���════════════════");
        System.out.println();
        System.out.println("Features:");
        System.out.println("  ✓ Real-time path tracing with Monte Carlo sampling");
        System.out.println("  ✓ Multiple material types (diffuse, metal, glass, emissive)");
        System.out.println("  ✓ BVH acceleration structure for fast ray intersection");
        System.out.println("  ✓ Interactive parameter adjustment");
        System.out.println("  ✓ Anti-aliasing via multi-sampling");
        System.out.println();
        System.out.println("Scene contents:");
        System.out.println("  - Ground plane (gray diffuse)");
        System.out.println("  - Red diffuse sphere (left)");
        System.out.println("  - Green metal sphere (center, reflective)");
        System.out.println("  - Blue glass sphere (right, refractive)");
        System.out.println("  - Emissive light source (top)");
        System.out.println();
        System.out.println("Controls:");
        System.out.println("  - Samples/Pixel: Adjusts anti-aliasing quality (1-16)");
        System.out.println("  - Max Depth: Adjusts ray bounce depth for indirect lighting (1-10)");
        System.out.println("  - Re-Render: Start rendering with current parameters");
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════\n");

        // Launch Swing GUI
        new PathTracerFrame();
    }
}
