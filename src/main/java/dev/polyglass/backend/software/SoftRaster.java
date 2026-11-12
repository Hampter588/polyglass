package dev.polyglass.backend.software;

import java.util.Arrays;

/**
 * A tiny CPU rasteriser: one colour buffer, one depth buffer, filled triangles.
 *
 * <p>This is the part of the software backend that can be built and verified
 * without a GPU, a window, or Minecraft on the classpath, so it is where the
 * fallback renderer starts. It has no Minecraft imports on purpose.</p>
 *
 * <p>Conventions: pixel centres are sampled at (x + 0.5, y + 0.5); {@code y}
 * grows downwards; depth is non-negative and nearer fragments have a smaller
 * value (the same convention as OpenGL after the viewport transform).</p>
 */
public final class SoftRaster {

    private final int width;
    private final int height;
    private final int[] color;
    private final float[] depth;

    public SoftRaster(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("framebuffer must be positive: " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        this.color = new int[width * height];
        this.depth = new float[width * height];
        clearDepth();
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** Live ARGB buffer, {@code width * height}. Row-major, top-left origin. */
    public int[] pixels() {
        return color;
    }

    public void clear(int argb) {
        Arrays.fill(color, argb);
    }

    public void clearDepth() {
        Arrays.fill(depth, Float.POSITIVE_INFINITY);
    }

    public int pixelAt(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return 0;
        }
        return color[y * width + x];
    }

    public float depthAt(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return Float.POSITIVE_INFINITY;
        }
        return depth[y * width + x];
    }

    /**
     * Rasterises one flat-shaded triangle with a depth test.
     *
     * <p>Colour comes from {@code a}; per-vertex interpolation and texturing are
     * later milestones. Winding does not matter — both orientations fill.</p>
     */
    public void triangle(Vertex a, Vertex b, Vertex c) {
        float area = edge(a.x(), a.y(), b.x(), b.y(), c.x(), c.y());
        if (area == 0f || !Float.isFinite(area)) {
            return;
        }

        int minX = clamp((int) Math.floor(min(a.x(), b.x(), c.x())), 0, width - 1);
        int maxX = clamp((int) Math.ceil(max(a.x(), b.x(), c.x())), 0, width - 1);
        int minY = clamp((int) Math.floor(min(a.y(), b.y(), c.y())), 0, height - 1);
        int maxY = clamp((int) Math.ceil(max(a.y(), b.y(), c.y())), 0, height - 1);
        if (minX > maxX || minY > maxY) {
            return;
        }

        boolean positive = area > 0f;
        for (int y = minY; y <= maxY; y++) {
            float py = y + 0.5f;
            int row = y * width;
            for (int x = minX; x <= maxX; x++) {
                float px = x + 0.5f;
                float w0 = edge(b.x(), b.y(), c.x(), c.y(), px, py);
                float w1 = edge(c.x(), c.y(), a.x(), a.y(), px, py);
                float w2 = edge(a.x(), a.y(), b.x(), b.y(), px, py);
                if (positive ? (w0 < 0f || w1 < 0f || w2 < 0f) : (w0 > 0f || w1 > 0f || w2 > 0f)) {
                    continue;
                }
                float z = (w0 * a.z() + w1 * b.z() + w2 * c.z()) / area;
                int index = row + x;
                if (z < depth[index]) {
                    depth[index] = z;
                    color[index] = a.argb();
                }
            }
        }
    }

    public record Vertex(float x, float y, float z, int argb) {
    }

    private static float edge(float ax, float ay, float bx, float by, float px, float py) {
        return (px - ax) * (by - ay) - (py - ay) * (bx - ax);
    }

    private static float min(float a, float b, float c) {
        return Math.min(a, Math.min(b, c));
    }

    private static float max(float a, float b, float c) {
        return Math.max(a, Math.max(b, c));
    }

    private static int clamp(int value, int lo, int hi) {
        return value < lo ? lo : Math.min(value, hi);
    }
}
