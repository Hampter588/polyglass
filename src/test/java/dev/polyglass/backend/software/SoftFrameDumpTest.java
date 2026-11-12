package dev.polyglass.backend.software;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Acceptance test for the software tier: a complete frame, rendered with no GPU
 * and no window. If this passes, the rasteriser genuinely works; it does not
 * mean the {@code GpuDevice} binding exists.
 */
class SoftFrameDumpTest {

    private static final int WIDTH = 320;
    private static final int HEIGHT = 240;

    private static final int RED = 0xFFFF5A5A;
    private static final int YELLOW = 0xFFFFD24D;
    private static final int PURPLE = 0xFF9B7BFF;

    @Test
    void frameIsNotEmptyAndNotAFlatFill() {
        SoftRaster raster = SoftFrameDump.render(WIDTH, HEIGHT);

        Set<Integer> colours = new HashSet<>();
        for (int pixel : raster.pixels()) {
            colours.add(pixel);
        }
        assertTrue(colours.size() > 20, "expected a shaded scene, got " + colours.size() + " colours");
    }

    @Test
    void nearerTriangleWinsEvenThoughItIsDrawnAfter() {
        SoftRaster raster = SoftFrameDump.render(WIDTH, HEIGHT);

        // Inside both the red (z=0.10) and yellow (z=0.30) triangles.
        assertEquals(RED, raster.pixelAt(200, 120));
        // Inside the yellow triangle only.
        assertEquals(YELLOW, raster.pixelAt(167, 120));
    }

    @Test
    void triangleStraddlingTheLeftEdgeIsClippedNotSkipped() {
        SoftRaster raster = SoftFrameDump.render(WIDTH, HEIGHT);

        assertEquals(PURPLE, raster.pixelAt(5, 190));
    }

    @Test
    void groundBehindEverythingIsVisibleBelowThemes() {
        SoftRaster raster = SoftFrameDump.render(WIDTH, HEIGHT);

        assertNotEquals(0, raster.pixelAt(319, 239));
        assertTrue(Float.isFinite(raster.depthAt(319, 239)));
    }

    @Test
    void degenerateSizesDoNotThrow() {
        assertTrue(SoftFrameDump.render(1, 1).width() == 1);
        assertTrue(SoftFrameDump.render(2, 1).height() == 1);
    }
}
