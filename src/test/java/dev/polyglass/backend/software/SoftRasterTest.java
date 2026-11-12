package dev.polyglass.backend.software;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The CPU raster core. Runs headless, which is the point of the fallback. */
class SoftRasterTest {

    private static final int RED = 0xFFFF0000;
    private static final int GREEN = 0xFF00FF00;
    private static final int BLUE = 0xFF0000FF;

    @Test
    void clearFillsEveryPixel() {
        SoftRaster raster = new SoftRaster(4, 3);
        raster.clear(BLUE);

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 4; x++) {
                assertEquals(BLUE, raster.pixelAt(x, y), "pixel " + x + "," + y);
            }
        }
    }

    @Test
    void triangleCoversItsInteriorOnly() {
        SoftRaster raster = new SoftRaster(16, 16);
        raster.clear(0);
        raster.triangle(
                new SoftRaster.Vertex(0f, 0f, 0.5f, RED),
                new SoftRaster.Vertex(16f, 0f, 0.5f, RED),
                new SoftRaster.Vertex(0f, 16f, 0.5f, RED));

        assertEquals(RED, raster.pixelAt(2, 2));
        assertEquals(0, raster.pixelAt(14, 14), "outside the hypotenuse");
    }

    @Test
    void nearerFragmentWinsRegardlessOfDrawOrder() {
        SoftRaster raster = new SoftRaster(8, 8);

        fill(raster, 0.9f, RED);
        fill(raster, 0.1f, GREEN);
        assertEquals(GREEN, raster.pixelAt(2, 2));

        raster.clear(0);
        raster.clearDepth();
        fill(raster, 0.1f, GREEN);
        fill(raster, 0.9f, RED);
        assertEquals(GREEN, raster.pixelAt(2, 2));
    }

    @Test
    void fullyClippedTriangleDoesNotThrow() {
        SoftRaster raster = new SoftRaster(8, 8);
        raster.clear(0);

        raster.triangle(
                new SoftRaster.Vertex(-40f, -40f, 0f, RED),
                new SoftRaster.Vertex(-20f, -40f, 0f, RED),
                new SoftRaster.Vertex(-40f, -20f, 0f, RED));

        assertEquals(0, raster.pixelAt(4, 4));
    }

    @Test
    void partiallyClippedTriangleStillFillsTheVisiblePart() {
        SoftRaster raster = new SoftRaster(16, 16);
        raster.clear(0);

        raster.triangle(
                new SoftRaster.Vertex(-8f, -8f, 0f, RED),
                new SoftRaster.Vertex(16f, -8f, 0f, RED),
                new SoftRaster.Vertex(-8f, 16f, 0f, RED));

        assertNotEquals(0, raster.pixelAt(2, 2));
    }

    @Test
    void degenerateTriangleIsANoOp() {
        SoftRaster raster = new SoftRaster(8, 8);
        raster.clear(0);

        raster.triangle(
                new SoftRaster.Vertex(2f, 2f, 0f, RED),
                new SoftRaster.Vertex(4f, 4f, 0f, RED),
                new SoftRaster.Vertex(6f, 6f, 0f, RED));

        assertEquals(0, raster.pixelAt(4, 4));
    }

    @Test
    void depthStartsAtInfinity() {
        SoftRaster raster = new SoftRaster(2, 2);

        assertTrue(Float.isInfinite(raster.depthAt(0, 0)));
        assertEquals(0, raster.pixelAt(-1, 0), "out of bounds reads are safe");
    }

    /** The triangle used by the depth test: origin + two edges, covering 4,4. */
    private static void fill(SoftRaster raster, float z, int argb) {
        raster.triangle(
                new SoftRaster.Vertex(0f, 0f, z, argb),
                new SoftRaster.Vertex(8f, 0f, z, argb),
                new SoftRaster.Vertex(0f, 8f, z, argb));
    }
}
