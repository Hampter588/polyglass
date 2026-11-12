package dev.polyglass.menu;

import dev.polyglass.Probe;
import dev.polyglass.backend.BackendCatalog;
import dev.polyglass.backend.BackendPlan;
import dev.polyglass.config.PolyglassConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
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

    private static final String DX12_URL = "https://github.com/366862732/DirectXmod";
    private static final int ROW_WIDTH = 280;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_PITCH = 22;

    /** Screen-local message under the buttons; survives {@link #rebuild()}. */
    private String statusLine = "";

    /** Set as soon as the config differs from what this session booted with. */
    private boolean dirty;

    public PolyglassScreen() {
        super(Component.literal("Polyglass"));
    }

    @Override
    protected void init() {
        super.init();

        int top = Math.max(8, (this.height - 196) / 2);
        int left = this.width / 2 - ROW_WIDTH / 2;

        addLine("Polyglass — graphics backend", left, top);
        addLine("This session: " + BackendCatalog.runningBackend(), left, top + 14);
        addLine("Order: " + String.join(" → ", PolyglassConfig.get().order()), left, top + 26);

        int y = top + 44;
        for (Choice choice : choices()) {
            addRenderableWidget(Button.builder(Component.literal(choice.label()), button -> select(choice.id()))
                    .bounds(left, y, ROW_WIDTH, ROW_HEIGHT)
                    .build());
            y += ROW_PITCH;
        }

        int rowY = y + 4;
        addRenderableWidget(Button.builder(dx12Label(),
                        ConfirmLinkScreen.confirmLink(this, DX12_URL))
                .bounds(left, rowY, ROW_WIDTH, ROW_HEIGHT)
                .build());

        addLine(noteText(), left, rowY + 26);

        int bottom = rowY + 40;
        addRenderableWidget(Button.builder(Component.literal("Reset"), button -> {
            PolyglassConfig.get().reset();
            dirty = true;
            statusLine = "Reset to defaults.";
            rebuild();
        }).bounds(left, bottom, 100, ROW_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> this.onClose())
                .bounds(left + ROW_WIDTH - 100, bottom, 100, ROW_HEIGHT).build());
    }

    private void addLine(String text, int x, int y) {
        StringWidget widget = new StringWidget(Component.literal(text), this.font);
        widget.setPosition(x, y);
        addRenderableWidget(widget);
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    /** A backend row: what it is, whether it can exist here, and whether it is chosen. */
    private record Choice(String id, String label) {
    }

    private List<Choice> choices() {
        String selected = BackendPlan.normalize(PolyglassConfig.get().backend());
        List<Choice> choices = new ArrayList<>(4);

        choices.add(new Choice(BackendPlan.AUTO,
                marker(selected, BackendPlan.AUTO) + "Auto — follow order"));
        choices.add(new Choice(BackendCatalog.VULKAN,
                marker(selected, BackendCatalog.VULKAN) + "Vulkan (vanilla)"));
        choices.add(new Choice(BackendCatalog.DX12,
                marker(selected, BackendCatalog.DX12)
                        + "Direct3D 12 (DirectXMod)"
                        + (BackendCatalog.directXModInstalled() && Probe.isWindows()
                        ? "" : " — " + (Probe.isWindows() ? "not installed" : "Windows only"))));
        choices.add(new Choice(BackendCatalog.OPENGL,
                marker(selected, BackendCatalog.OPENGL) + "OpenGL (vanilla)"));
        return choices;
    }

    private static String marker(String selected, String id) {
        return selected.equals(id) ? "▶ " : "   ";
    }

    private void select(String id) {
        PolyglassConfig config = PolyglassConfig.get();
        if (id.equals(BackendPlan.normalize(config.backend()))) {
            return;
        }
        config.setBackend(id);
        dirty = true;
        rebuild();
    }

    private Component dx12Label() {
        return Component.literal(BackendCatalog.directXModInstalled()
                ? "DirectXMod: installed"
                : "Get DirectXMod (optional)");
    }

    private String noteText() {
        if (!statusLine.isEmpty()) {
            return statusLine;
        }
        return dirty ? "Restart the game to apply." : "Changes apply on the next launch.";
    }
}
