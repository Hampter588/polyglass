package dev.polyglass.backend;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure ordering policy: no Minecraft, no GPU, no I/O. */
class BackendPlanTest {

    private static final List<String> AVAILABLE = List.of("vulkan", "dx12", "opengl", "software");

    @Test
    void autoFollowsConfiguredOrder() {
        List<String> plan = BackendPlan.resolve("auto", List.of("vulkan", "dx12", "opengl"), AVAILABLE);

        assertEquals(List.of("vulkan", "dx12", "opengl", "software"), plan);
    }

    @Test
    void explicitChoiceGoesFirst() {
        List<String> plan = BackendPlan.resolve("opengl", List.of("vulkan", "opengl"), AVAILABLE);

        assertEquals("opengl", plan.get(0));
        assertTrue(plan.contains("vulkan"));
    }

    @Test
    void explicitChoiceIsNotDuplicated() {
        List<String> plan = BackendPlan.resolve("dx12", List.of("vulkan", "dx12"), AVAILABLE);

        assertEquals(1L, plan.stream().filter("dx12"::equals).count());
    }

    @Test
    void unknownChoiceFallsThroughToTheOrder() {
        List<String> plan = BackendPlan.resolve("metal", List.of("vulkan", "opengl"), AVAILABLE);

        assertEquals("vulkan", plan.get(0));
    }

    @Test
    void unavailableIdsAreDropped() {
        List<String> plan = BackendPlan.resolve("auto", List.of("vulkan", "dx12", "opengl"), List.of("vulkan", "opengl"));

        assertEquals(List.of("vulkan", "opengl"), plan);
    }

    @Test
    void availableButUnlistedBackendIsStillTriedLast() {
        List<String> plan = BackendPlan.resolve("auto", List.of("opengl"), AVAILABLE);

        assertEquals(List.of("opengl", "vulkan", "dx12", "software"), plan);
    }

    @Test
    void nothingAvailableProducesAnEmptyPlan() {
        assertTrue(BackendPlan.resolve("auto", List.of("vulkan"), List.of()).isEmpty());
    }

    @Test
    void idsAreNormalised() {
        List<String> plan = BackendPlan.resolve("  VULKAN ", List.of(" OpenGL "), AVAILABLE);

        assertEquals(List.of("vulkan", "opengl", "dx12", "software"), plan);
    }
}
