package com.purpleclicker;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Purple-accented settings menu. Drag the header to move it. */
public class ClickerGui extends GuiScreen {

    private static final int W = 230;
    private static final int HEADER = 26;
    private static final int PAD = 10;

    private static final int BG        = 0xF0121019;
    private static final int ROW_HOVER = 0x18FFFFFF;
    private static final int TEXT      = 0xFFEDE9F5;
    private static final int TEXT_DIM  = 0xFF9A93AD;
    private static final int TRACK     = 0xFF2A2638;
    private static final int OFF       = 0xFF3B3650;

    // Remembered between openings
    private static int panelX = -1, panelY = -1;

    private final List<Element> elements = new ArrayList<Element>();
    private boolean draggingPanel;
    private int dragOffX, dragOffY;
    private Slider activeSlider;

    public ClickerGui() {
        elements.add(new Header("Clicker"));
        elements.add(new Toggle("Enabled", () -> Settings.enabled, v -> Settings.enabled = v));
        elements.add(new Header("Left click"));
        elements.add(new Toggle("Left clicker", () -> Settings.leftEnabled, v -> Settings.leftEnabled = v));
        elements.add(new Slider("Min CPS", 1, 20, () -> Settings.leftMinCps, v -> {
            Settings.leftMinCps = v;
            if (Settings.leftMaxCps < v) Settings.leftMaxCps = v;
        }, ""));
        elements.add(new Slider("Max CPS", 1, 20, () -> Settings.leftMaxCps, v -> {
            Settings.leftMaxCps = v;
            if (Settings.leftMinCps > v) Settings.leftMinCps = v;
        }, ""));
        elements.add(new Toggle("Pause on blocks", () -> Settings.breakBlocks, v -> Settings.breakBlocks = v));
        elements.add(new Header("Right click"));
        elements.add(new Toggle("Right clicker", () -> Settings.rightEnabled, v -> Settings.rightEnabled = v));
        elements.add(new Slider("CPS", 1, 20, () -> Settings.rightCps, v -> Settings.rightCps = v, ""));
        elements.add(new Header("Appearance"));
        elements.add(new Slider("Accent hue", 0, 360, () -> Settings.accentHue, v -> Settings.accentHue = v, "°"));
        elements.add(new Slider("Accent saturation", 0, 100, () -> Settings.accentSat, v -> Settings.accentSat = v, "%"));
        elements.add(new Toggle("Show HUD", () -> Settings.showHud, v -> Settings.showHud = v));
        elements.add(new Button("Reset to purple", () -> { Settings.accentHue = 275; Settings.accentSat = 70; }));
    }

    private int panelHeight() {
        int h = HEADER + 6;
        for (Element e : elements) h += e.height();
        return h + 22; // footer
    }

    @Override
    public void initGui() {
        if (panelX < 0) {
            panelX = (width - W) / 2;
            panelY = (height - panelHeight()) / 2;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0x66000000);

        if (draggingPanel) {
            panelX = mouseX - dragOffX;
            panelY = mouseY - dragOffY;
        }
        int ph = panelHeight();
        panelX = Settings.clamp(panelX, 0, Math.max(0, width - W));
        panelY = Settings.clamp(panelY, 0, Math.max(0, height - ph));

        if (activeSlider != null) activeSlider.setFromMouse(mouseX);

        int accent = Settings.accent();
        int accentDark = Settings.accentDark();
        int x = panelX, y = panelY;

        // glow + border + background
        drawRect(x - 2, y - 2, x + W + 2, y + ph + 2, (accent & 0x00FFFFFF) | 0x30000000);
        drawRect(x - 1, y - 1, x + W + 1, y + ph + 1, accentDark);
        drawRect(x, y, x + W, y + ph, BG);

        // header with gradient
        drawGradientRect(x, y, x + W, y + HEADER, accent, accentDark);
        fontRendererObj.drawStringWithShadow("PurpleClicker", x + PAD, y + 9, 0xFFFFFFFF);
        String ver = "v1.0";
        fontRendererObj.drawString(ver, x + W - PAD - fontRendererObj.getStringWidth(ver), y + 9, 0xCCFFFFFF);

        int cy = y + HEADER + 6;
        for (Element e : elements) {
            e.y = cy;
            e.draw(x, cy, mouseX, mouseY, accent);
            cy += e.height();
        }

        // footer
        drawRect(x + PAD, cy + 2, x + W - PAD, cy + 3, 0x22FFFFFF);
        String keys = "Toggle: " + Keyboard.getKeyName(PurpleClicker.toggleKey.getKeyCode())
                + "   Menu: " + Keyboard.getKeyName(PurpleClicker.menuKey.getKeyCode());
        fontRendererObj.drawString(keys, x + PAD, cy + 8, TEXT_DIM);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (button == 0) {
            if (inside(mouseX, mouseY, panelX, panelY, W, HEADER)) {
                draggingPanel = true;
                dragOffX = mouseX - panelX;
                dragOffY = mouseY - panelY;
                return;
            }
            for (Element e : elements) {
                if (inside(mouseX, mouseY, panelX, e.y, W, e.height())) {
                    e.click(mouseX);
                    break;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        draggingPanel = false;
        activeSlider = null;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == PurpleClicker.menuKey.getKeyCode()) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode); // ESC closes
    }

    @Override
    public void onGuiClosed() {
        Settings.save();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ------------------------------------------------------------ elements

    private abstract class Element {
        final String label;
        int y;
        Element(String label) { this.label = label; }
        abstract int height();
        abstract void draw(int x, int y, int mx, int my, int accent);
        void click(int mx) {}
    }

    private class Header extends Element {
        Header(String label) { super(label); }
        int height() { return 16; }
        void draw(int x, int y, int mx, int my, int accent) {
            fontRendererObj.drawString(label.toUpperCase(), x + PAD, y + 5, accent);
        }
    }

    private class Toggle extends Element {
        final BooleanSupplier get;
        final Consumer<Boolean> set;
        Toggle(String label, BooleanSupplier get, Consumer<Boolean> set) {
            super(label); this.get = get; this.set = set;
        }
        int height() { return 18; }
        void draw(int x, int y, int mx, int my, int accent) {
            if (inside(mx, my, x, y, W, height())) drawRect(x, y, x + W, y + height(), ROW_HOVER);
            fontRendererObj.drawString(label, x + PAD, y + 5, TEXT);
            boolean on = get.getAsBoolean();
            int sx = x + W - PAD - 22, sy = y + 4;
            drawRect(sx, sy, sx + 22, sy + 10, on ? accent : OFF);
            int kx = on ? sx + 13 : sx + 1;
            Gui.drawRect(kx, sy + 1, kx + 8, sy + 9, 0xFFFFFFFF);
        }
        void click(int mx) { set.accept(!get.getAsBoolean()); }
    }

    private class Slider extends Element {
        final int min, max;
        final IntSupplier get;
        final IntConsumer set;
        final String suffix;
        Slider(String label, int min, int max, IntSupplier get, IntConsumer set, String suffix) {
            super(label); this.min = min; this.max = max; this.get = get; this.set = set; this.suffix = suffix;
        }
        int height() { return 26; }
        int trackX() { return panelX + PAD; }
        int trackW() { return W - PAD * 2; }
        void draw(int x, int y, int mx, int my, int accent) {
            if (inside(mx, my, x, y, W, height()) || activeSlider == this)
                drawRect(x, y, x + W, y + height(), ROW_HOVER);
            int v = get.getAsInt();
            fontRendererObj.drawString(label, x + PAD, y + 4, TEXT);
            String val = v + suffix;
            fontRendererObj.drawString(val, x + W - PAD - fontRendererObj.getStringWidth(val), y + 4, accent);

            int tx = trackX(), tw = trackW(), ty = y + 17;
            float t = (v - min) / (float) (max - min);
            int fill = (int) (tw * t);
            drawRect(tx, ty, tx + tw, ty + 3, TRACK);
            drawRect(tx, ty, tx + fill, ty + 3, accent);
            drawRect(tx + fill - 2, ty - 3, tx + fill + 2, ty + 6, 0xFFFFFFFF);
        }
        void click(int mx) {
            activeSlider = this;
            setFromMouse(mx);
        }
        void setFromMouse(int mx) {
            float t = (mx - trackX()) / (float) trackW();
            t = Math.max(0f, Math.min(1f, t));
            set.accept(Math.round(min + t * (max - min)));
        }
    }

    private class Button extends Element {
        final Runnable action;
        Button(String label, Runnable action) { super(label); this.action = action; }
        int height() { return 22; }
        void draw(int x, int y, int mx, int my, int accent) {
            int bx = x + PAD, bw = W - PAD * 2, by = y + 3;
            boolean hover = inside(mx, my, bx, by, bw, 16);
            drawRect(bx, by, bx + bw, by + 16, hover ? accent : OFF);
            int tw = fontRendererObj.getStringWidth(label);
            fontRendererObj.drawString(label, bx + (bw - tw) / 2, by + 4, 0xFFFFFFFF);
        }
        void click(int mx) { action.run(); }
    }
}
