package dev.polyglass.backend.software;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Renders a known scene with {@link SoftRaster} and writes it as a PNG.
 *
 * <p>This is the software tier's acceptance test in miniature: a complete
 * frame produced with no GPU, no driver and no Minecraft classes. It exists so
 * that the raster core can be demonstrated from the picker ("CPU raster test")
 * and covered by a headless unit test, instead of being taken on faith.</p>
 */
public final class SoftFrameDump {

    public static final String FILE_NAME = "polyglass-selftest.png";

    private SoftFrameDump() {
    }

    /**
     * A deterministic scene: vertical sky gradient, a ground plane, three
     * overlapping triangles at different depths (so a broken depth test is
     * visible), and one triangle clipped by the frame edge.
     */
    public static SoftRaster render(int width, int height) {
        SoftRaster raster = new SoftRaster(width, height);

        for (int y = 0; y < height; y++) {
            int color = lerp(0xFF0A1424, 0xFF2E5C8A, y / (float) Math.max(1, height - 1));
            int row = y * width;
            for (int x = 0; x < width; x++) {
                raster.pixels()[row + x] = color;
            }
        }
        raster.clearDepth();

        float w = width;
        float h = height;

        // Ground: two triangles, farther than everything else.
        int ground = 0xFF1D3B2A;
        raster.triangle(
                new SoftRaster.Vertex(0f, h * 0.62f, 0.9f, ground),
                new SoftRaster.Vertex(w, h * 0.62f, 0.9f, ground),
                new SoftRaster.Vertex(0f, h, 0.9f, ground));
        raster.triangle(
                new SoftRaster.Vertex(w, h * 0.62f, 0.9f, ground),
                new SoftRaster.Vertex(w, h, 0.9f, ground),
                new SoftRaster.Vertex(0f, h, 0.9f, ground));

        // Three overlapping triangles. Depth order is deliberately not the draw
        // order, so the nearest one must win on the CPU as it would on a GPU.
        raster.triangle(
                new SoftRaster.Vertex(w * 0.20f, h * 0.30f, 0.50f, 0xFF4D8DFF),
                new SoftRaster.Vertex(w * 0.62f, h * 0.30f, 0.50f, 0xFF4D8DFF),
                new SoftRaster.Vertex(w * 0.41f, h * 0.78f, 0.50f, 0xFF4D8DFF));

        raster.triangle(
                new SoftRaster.Vertex(w * 0.38f, h * 0.22f, 0.30f, 0xFFFFD24D),
                new SoftRaster.Vertex(w * 0.80f, h * 0.22f, 0.30f, 0xFFFFD24D),
                new SoftRaster.Vertex(w * 0.59f, h * 0.70f, 0.30f, 0xFFFFD24D));

        raster.triangle(
                new SoftRaster.Vertex(w * 0.50f, h * 0.40f, 0.10f, 0xFFFF5A5A),
                new SoftRaster.Vertex(w * 0.90f, h * 0.40f, 0.10f, 0xFFFF5A5A),
                new SoftRaster.Vertex(w * 0.70f, h * 0.88f, 0.10f, 0xFFFF5A5A));

        // Straddles the left edge: exercises the per-pixel clip without throwing.
        raster.triangle(
                new SoftRaster.Vertex(-w * 0.25f, h * 0.55f, 0.05f, 0xFF9B7BFF),
                new SoftRaster.Vertex(w * 0.16f, h * 0.55f, 0.05f, 0xFF9B7BFF),
                new SoftRaster.Vertex(-w * 0.05f, h * 0.95f, 0.05f, 0xFF9B7BFF));

        return raster;
    }

    /** Renders {@link #render} and writes {@code polyglass-selftest.png} into {@code dir}. */
    public static Path write(Path dir, int width, int height) throws IOException {
        SoftRaster raster = render(width, height);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, raster.pixels(), 0, width);
        Files.createDirectories(dir);
        Path out = dir.resolve(FILE_NAME);
        ImageIO.write(image, "png", out.toFile());
        return out;
    }

    /** Channel-wise lerp between two ARGB colours. */
    private static int lerp(int from, int to, float t) {
        float clamped = Math.max(0f, Math.min(1f, t));
        int a = (int) (((from >>> 24) & 0xFF) * (1 - clamped) + ((to >>> 24) & 0xFF) * clamped);
        int r = (int) (((from >>> 16) & 0xFF) * (1 - clamped) + ((to >>> 16) & 0xFF) * clamped);
        int g = (int) (((from >>> 8) & 0xFF) * (1 - clamped) + ((to >>> 8) & 0xFF) * clamped);
        int b = (int) ((from & 0xFF) * (1 - clamped) + (to & 0xFF) * clamped);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
