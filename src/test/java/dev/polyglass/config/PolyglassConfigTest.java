package dev.polyglass.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Vanilla's Graphics API setting, as it appears in {@code options.txt}. Parsed
 * without touching a game directory so the format is covered.
 */
class PolyglassConfigTest {

    @Test
    void vanillaVulkanIsRead() {
        assertEquals("vulkan", PolyglassConfig.parseVanillaPreference(List.of(
                "version:4903",
                "preferredGraphicsBackend:\"vulkan\"",
                "graphicsPreset:\"fancy\"")));
    }

    @Test
    void vanillaOpenGlIsRead() {
        assertEquals("opengl", PolyglassConfig.parseVanillaPreference(List.of(
                "preferredGraphicsBackend:\"opengl\"")));
    }

    @Test
    void defaultStaysDefault() {
        assertEquals("default", PolyglassConfig.parseVanillaPreference(List.of(
                "preferredGraphicsBackend:\"default\"")));
    }

    @Test
    void unquotedValueIsAccepted() {
        assertEquals("vulkan", PolyglassConfig.parseVanillaPreference(List.of(
                "preferredGraphicsBackend:vulkan")));
    }

    @Test
    void missingLineMeansDefault() {
        assertEquals("default", PolyglassConfig.parseVanillaPreference(List.of(
                "version:4903",
                "graphicsPreset:\"fancy\"")));
    }

    @Test
    void unknownValueMeansDefault() {
        assertEquals("default", PolyglassConfig.parseVanillaPreference(List.of(
                "preferredGraphicsBackend:\"metal\"")));
    }

    @Test
    void emptyFileMeansDefault() {
        assertEquals("default", PolyglassConfig.parseVanillaPreference(List.of()));
    }
}
