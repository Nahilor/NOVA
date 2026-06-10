package renderer.ui;

import renderer.math.Vector3;
import renderer.math.Ray;
import renderer.geometry.Sphere;
import renderer.geometry.Plane;
import renderer.material.Lambertian;
import renderer.material.Metal;
import renderer.material.Dielectric;
import renderer.material.Emissive;
import renderer.acceleration.BVH;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Interactive Swing GUI for NOVA path tracer.
 *
 * Features:
 * - Real-time rendering preview with Swing
 * - Simple scene with multiple material types
 * - Ray tracing with BVH acceleration
 * - Material selection and parameter adjustment
 *
 * This is a minimal demo to visualize path tracing output.
 */
public class PathTracerFrame extends JFrame {

    private RenderPanel renderPanel;
    private ControlPanel controlPanel;

    public PathTracerFrame() {
        setTitle("NOVA Path Tracer - Interactive Demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Create main panels
        renderPanel = new RenderPanel(640, 480);
        controlPanel = new ControlPanel(renderPanel);

        // Layout
        setLayout(new BorderLayout());
        add(renderPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        // Start initial render
        renderPanel.startRender();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PathTracerFrame());
    }
}

/**
 * Panel that displays the rendered image.
 */
class RenderPanel extends JPanel {

    private BufferedImage image;
    private int width, height;
    private volatile boolean rendering = false;
    private RenderThread renderThread;

    // Scene parameters
    private int samplesPerPixel = 4;
    private int maxDepth = 3;

    public RenderPanel(int width, int height) {
        this.width = width;
        this.height = height;
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        setPreferredSize(new Dimension(width, height));
        setBackground(Color.BLACK);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            g.drawImage(image, 0, 0, null);
        }

        if (rendering) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 14));
            g.drawString("Rendering...", 10, 20);
        }
    }

    public void startRender() {
        if (rendering) return;

        rendering = true;
        renderThread = new RenderThread(this, image, width, height, samplesPerPixel, maxDepth);
        renderThread.start();
    }

    public void setSamplesPerPixel(int samples) {
        this.samplesPerPixel = Math.max(1, samples);
    }

    public void setMaxDepth(int depth) {
        this.maxDepth = Math.max(1, depth);
    }

    public void setImageData(int[] pixels) {
        image.setRGB(0, 0, width, height, pixels, 0, width);
        repaint();
    }

    public void setRendering(boolean rendering) {
        this.rendering = rendering;
        repaint();
    }
}

/**
 * Render thread that performs path tracing.
 */
class RenderThread extends Thread {

    private final RenderPanel panel;
    private final BufferedImage image;
    private final int width, height;
    private final int samplesPerPixel;
    private final int maxDepth;

    public RenderThread(RenderPanel panel, BufferedImage image, int width, int height,
                        int samplesPerPixel, int maxDepth) {
        this.panel = panel;
        this.image = image;
        this.width = width;
        this.height = height;
        this.samplesPerPixel = samplesPerPixel;
        this.maxDepth = maxDepth;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            // Create simple scene
            List<renderer.geometry.Shape> shapes = createScene();
            BVH bvh = new BVH(shapes);

            // Camera setup
            Vector3 cameraPos = new Vector3(0, 1, 3);
            Vector3 lookAt = new Vector3(0, 0, -5);
            Vector3 up = new Vector3(0, 1, 0);
            double fov = 50.0;

            // Render
            int[] pixels = new int[width * height];
            long startTime = System.currentTimeMillis();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Vector3 color = Vector3.BLACK;

                    // Multi-sample anti-aliasing
                    for (int s = 0; s < samplesPerPixel; s++) {
                        double u = (x + Math.random()) / width;
                        double v = (height - y + Math.random()) / height;

                        Ray ray = createCameraRay(cameraPos, lookAt, up, fov, u, v);
                        color = color.plus(traceRay(ray, bvh, maxDepth));
                    }

                    // Average samples and tonemap
                    color = color.scaled(1.0 / samplesPerPixel);
                    color = toneMap(color);

                    // Convert to RGB
                    int r = (int) (Math.min(1.0, color.x()) * 255);
                    int g = (int) (Math.min(1.0, color.y()) * 255);
                    int b = (int) (Math.min(1.0, color.z()) * 255);

                    pixels[y * width + x] = (r << 16) | (g << 8) | b;
                }

                // Update display every scanline
                if (y % 10 == 0) {
                    panel.setImageData(pixels.clone());
                }
            }

            panel.setImageData(pixels);
            panel.setRendering(false);

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("Render completed in " + elapsed + "ms");

        } catch (Exception e) {
            e.printStackTrace();
            panel.setRendering(false);
        }
    }

    /**
     * Create a simple test scene with multiple materials.
     */
    private List<renderer.geometry.Shape> createScene() {
        List<renderer.geometry.Shape> shapes = new ArrayList<>();

        // Ground plane
        shapes.add(new Plane(
            new Vector3(0, -1, 0),
            new Vector3(0, 1, 0),
            new Lambertian(new Vector3(0.7, 0.7, 0.7))
        ));

        // Red diffuse sphere (left)
        shapes.add(new Sphere(
            new Vector3(-2, 0, -5),
            0.8,
            new Lambertian(new Vector3(0.9, 0.2, 0.2))
        ));

        // Green metal sphere (center)
        shapes.add(new Sphere(
            new Vector3(0, 0, -5),
            0.8,
            new Metal(new Vector3(0.8, 0.9, 0.8), 0.1)
        ));

        // Blue glass sphere (right)
        shapes.add(new Sphere(
            new Vector3(2, 0, -5),
            0.8,
            new Dielectric(1.5)
        ));

        // Light source (emissive sphere)
        shapes.add(new Sphere(
            new Vector3(0, 3, -3),
            0.5,
            new renderer.material.Emissive(new Vector3(2.0, 2.0, 2.0))
        ));

        return shapes;
    }

    /**
     * Create a camera ray for given screen coordinates.
     */
    private Ray createCameraRay(Vector3 origin, Vector3 target, Vector3 up,
                                 double fov, double u, double v) {
        // Simplified perspective projection
        double theta = Math.toRadians(fov);
        double h = Math.tan(theta / 2.0);
        double w = h * (width / (double) height);

        Vector3 forward = target.minus(origin).normalized();
        Vector3 right = forward.cross(up).normalized();
        Vector3 newUp = right.cross(forward).normalized();

        Vector3 horizontal = right.scaled(2.0 * w);
        Vector3 vertical = newUp.scaled(2.0 * h);
        Vector3 lowerLeft = origin
            .plus(forward)
            .minus(horizontal.scaled(0.5))
            .minus(vertical.scaled(0.5));

        Vector3 rayDir = lowerLeft
            .plus(horizontal.scaled(u))
            .plus(vertical.scaled(v))
            .minus(origin);

        return new Ray(origin, rayDir.normalized());
    }

    /**
     * Trace a ray through the scene.
     */
    private Vector3 traceRay(Ray ray, BVH scene, int depth) {
        if (depth <= 0) return Vector3.BLACK;

        var hitOpt = scene.hit(ray, 0.0001, Double.MAX_VALUE);

        if (hitOpt.isEmpty()) {
            // Sky gradient
            double t = 0.5 * (ray.direction().normalized().y() + 1.0);
            return new Vector3(1, 1, 1).scaled(1.0 - t)
                .plus(new Vector3(0.5, 0.7, 1.0).scaled(t));
        }

        var hit = hitOpt.get();
        renderer.material.Material material = hit.material();

        // Check if emissive
        Vector3 emitted = material.emitted(hit.u(), hit.v());

        // Scatter ray
        var scatter = material.scatter(ray, hit);

        if (!scatter.scattered()) {
            return emitted;
        }

        Vector3 scattered = traceRay(scatter.scatteredRay(), scene, depth - 1);
        return emitted.plus(scatter.attenuation().times(scattered));
    }

    /**
     * Simple tone mapping (gamma correction).
     */
    private Vector3 toneMap(Vector3 color) {
        double gamma = 2.2;
        double r = Math.pow(color.x(), 1.0 / gamma);
        double g = Math.pow(color.y(), 1.0 / gamma);
        double b = Math.pow(color.z(), 1.0 / gamma);
        return new Vector3(r, g, b);
    }
}

/**
 * Control panel for rendering parameters.
 */
class ControlPanel extends JPanel {

    private final RenderPanel renderPanel;
    private final JSlider samplesSlider;
    private final JSlider depthSlider;
    private final JButton renderButton;

    public ControlPanel(RenderPanel renderPanel) {
        this.renderPanel = renderPanel;

        setLayout(new FlowLayout(FlowLayout.LEFT, 20, 10));
        setBackground(new Color(240, 240, 240));

        // Samples slider
        JLabel samplesLabel = new JLabel("Samples/Pixel:");
        samplesSlider = new JSlider(1, 16, 4);
        samplesSlider.setPreferredSize(new Dimension(150, 40));
        samplesSlider.setMajorTickSpacing(3);
        samplesSlider.setMinorTickSpacing(1);
        samplesSlider.setPaintTicks(true);
        samplesSlider.setPaintLabels(true);
        samplesSlider.addChangeListener(e -> {
            renderPanel.setSamplesPerPixel(samplesSlider.getValue());
        });

        // Depth slider
        JLabel depthLabel = new JLabel("Max Depth:");
        depthSlider = new JSlider(1, 10, 3);
        depthSlider.setPreferredSize(new Dimension(150, 40));
        depthSlider.setMajorTickSpacing(1);
        depthSlider.setPaintTicks(true);
        depthSlider.setPaintLabels(true);
        depthSlider.addChangeListener(e -> {
            renderPanel.setMaxDepth(depthSlider.getValue());
        });

        // Render button
        renderButton = new JButton("Re-Render");
        renderButton.setPreferredSize(new Dimension(120, 30));
        renderButton.addActionListener(e -> {
            renderButton.setEnabled(false);
            renderPanel.startRender();
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                renderButton.setEnabled(true);
            }).start();
        });

        add(samplesLabel);
        add(samplesSlider);
        add(Box.createHorizontalStrut(20));
        add(depthLabel);
        add(depthSlider);
        add(Box.createHorizontalStrut(20));
        add(renderButton);
    }
}
