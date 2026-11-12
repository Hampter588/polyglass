package dev.polyglass.menu;

import dev.polyglass.Probe;
import dev.polyglass.backend.BackendPlan;
import dev.polyglass.config.PolyglassConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Backend picker.
 *
 * <p>There is no live switch, and there cannot be: the candidate list is
 * consumed once at boot, before the GLFW window exists, and the {@code GpuDevice}
 * owns every buffer, texture and pipeline created afterwards. So this screen
 * writes {@code polyglass.json} and tells the user to restart. That is also how
 * every shipped game with a DirectX/Vulkan toggle behaves.</p>
 */
public final class PolyglassScreen extends Screen {

    private static final int WIDGET_WIDTH = 240;

    public PolyglassScreen() {
        super(Component.literal("Polyglass"));
    }

    @Override
    protected void init() {
        super.init();
        int centre = this.width / 2;
        int y = 48;

        StringWidget title = new StringWidget(Component.literal("Polyglass - graphics backend"), this.font);
        title.setPosition(centre - WIDGET_WIDTH / 2, 24);
        addRenderableWidget(title);

        addRenderableWidget(Button.builder(label(), button -> {
            cycle();
            rebuild();
        }).bounds(centre - WIDGET_WIDTH / 2, y, WIDGET_WIDTH, 20).build());
        y += 26;

        addRenderableWidget(Button.builder(softwareLabel(), button -> {
            PolyglassConfig config = PolyglassConfig.get();
            config.setSoftwareEnabled(!config.softwareEnabled());
            rebuild();
        }).bounds(centre - WIDGET_WIDTH / 2, y, WIDGET_WIDTH, 20).build());
        y += 26;

        Button order = Button.builder(
                        Component.literal("Order: " + String.join(" > ", PolyglassConfig.get().order())),
                        button -> {
                        })
                .bounds(centre - WIDGET_WIDTH / 2, y, WIDGET_WIDTH, 20)
                .build();
        order.active = false; // read-only: edit polyglass.json to reorder
        addRenderableWidget(order);
        y += 26;

        StringWidget note = new StringWidget(restartNote(), this.font);
        note.setPosition(centre - WIDGET_WIDTH / 2, y);
        addRenderableWidget(note);
        y += 22;

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> this.onClose())
                .bounds(centre - 50, y, 100, 20).build());
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    private void cycle() {
        List<String> ids = candidates();
        String current = BackendPlan.normalize(PolyglassConfig.get().backend());
        int index = ids.indexOf(current);
        String next = ids.get((index + 1) % ids.size());
        PolyglassConfig.get().setBackend(next);
    }

    private Component label() {
        String current = PolyglassConfig.get().backend();
        return Component.literal("Backend: " + displayName(current));
    }

    private Component softwareLabel() {
        return Component.literal("Software fallback: " + (PolyglassConfig.get().softwareEnabled() ? "ON" : "OFF"));
    }

    private Component restartNote() {
        return Component.literal("Restart the game to apply.");
    }

    private static List<String> candidates() {
        List<String> ids = new ArrayList<>();
        ids.add(BackendPlan.AUTO);
        ids.add("vulkan");
        if (Probe.isWindows() && Probe.hasClass("com.xgdt.dx12.dx12.Dx12Backend")) {
            ids.add("dx12");
        }
        ids.add("opengl");
        ids.add("software");
        return ids;
    }

    private static String displayName(String id) {
        return switch (BackendPlan.normalize(id)) {
            case BackendPlan.AUTO -> "Auto (" + String.join(" → ", PolyglassConfig.get().order()) + ")";
            case "vulkan" -> "Vulkan (vanilla)";
            case "opengl" -> "OpenGL (vanilla)";
            case "dx12" -> "Direct3D 12 (DirectXMod)";
            case "software" -> "Software (CPU, experimental)";
            default -> id;
        };
    }
}
