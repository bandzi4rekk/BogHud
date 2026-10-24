package pl.boghud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Własne, ładniejsze widgety do ekranu ustawień. */
public final class Ui {
    public static final int BG = 0xF0141A22;
    public static final int TITLE_BG = 0xFF1B222C;
    public static final int BORDER = 0xFF2B3440;
    public static final int LINE = 0xFF2A3038;
    public static final int HOVER = 0x22FFFFFF;
    public static final int TEXT = 0xFFE6EDF3;
    public static final int MUTED = 0xFF8B98A5;
    public static final int ACCENT = 0xFF5CE1E6;
    public static final int OFF = 0xFF3A424C;

    private Ui() {}

    private static TextRenderer tr() {
        return MinecraftClient.getInstance().textRenderer;
    }

    public static void outline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    /** Przycisk tekstowy / zakładka. */
    public static class Button extends ClickableWidget {
        private final String label;
        private final Runnable action;
        private final BooleanSupplier selected;

        public Button(int x, int y, int w, int h, String label, Runnable action, BooleanSupplier selected) {
            super(x, y, w, h, Text.literal(label));
            this.label = label;
            this.action = action;
            this.selected = selected;
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            TextRenderer tr = tr();
            boolean sel = selected.getAsBoolean();
            if (isHovered()) {
                ctx.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), HOVER);
            }
            int tw = tr.getWidth(label);
            ctx.drawText(tr, label, getX() + (getWidth() - tw) / 2, getY() + (getHeight() - 8) / 2,
                    sel ? TEXT : MUTED, false);
            if (sel) {
                ctx.fill(getX() + 2, getY() + getHeight() - 2, getX() + getWidth() - 2, getY() + getHeight(), ACCENT);
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            action.run();
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        }
    }

    /** Przełącznik ON/OFF w stylu "switch". */
    public static class Toggle extends ClickableWidget {
        private final String label;
        private final BooleanSupplier get;
        private final Consumer<Boolean> set;

        public Toggle(int x, int y, int w, int h, String label, BooleanSupplier get, Consumer<Boolean> set) {
            super(x, y, w, h, Text.literal(label));
            this.label = label;
            this.get = get;
            this.set = set;
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            TextRenderer tr = tr();
            if (isHovered()) {
                ctx.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), HOVER);
            }
            ctx.drawText(tr, label, getX() + 4, getY() + (getHeight() - 8) / 2, TEXT, false);

            boolean on = get.getAsBoolean();
            int sw = 22, sh = 10;
            int sx = getX() + getWidth() - sw - 4;
            int sy = getY() + (getHeight() - sh) / 2;
            ctx.fill(sx, sy, sx + sw, sy + sh, on ? ACCENT : OFF);
            int kx = on ? sx + sw - sh + 1 : sx + 1;
            ctx.fill(kx, sy + 1, kx + sh - 2, sy + sh - 1, 0xFFFFFFFF);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            set.accept(!get.getAsBoolean());
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        }
    }

    /** Płaski suwak na liczbach całkowitych (wartość czytana na żywo z configu). */
    public static class Slider extends ClickableWidget {
        private static final int LABEL_W = 54;
        private static final int VALUE_W = 28;

        private final String label;
        private final int min, max, tint;
        private final IntSupplier get;
        private final IntConsumer set;

        public Slider(int x, int y, int w, int h, String label, int min, int max,
                      IntSupplier get, IntConsumer set, int tint) {
            super(x, y, w, h, Text.literal(label));
            this.label = label;
            this.min = min;
            this.max = max;
            this.get = get;
            this.set = set;
            this.tint = tint;
        }

        private int trackX() {
            return getX() + LABEL_W;
        }

        private int trackW() {
            return getWidth() - LABEL_W - VALUE_W;
        }

        private void setFromMouse(double mouseX) {
            double t = (mouseX - trackX()) / trackW();
            t = Math.max(0, Math.min(1, t));
            set.accept((int) Math.round(min + t * (max - min)));
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            TextRenderer tr = tr();
            int cy = getY() + getHeight() / 2;
            ctx.drawText(tr, label, getX() + 4, cy - 4, TEXT, false);

            int tx = trackX();
            int tw = trackW();
            int v = Math.max(min, Math.min(max, get.getAsInt()));
            double t = (v - min) / (double) (max - min);
            int kx = tx + (int) Math.round(t * tw);

            ctx.fill(tx, cy - 1, tx + tw, cy + 2, OFF);
            ctx.fill(tx, cy - 1, kx, cy + 2, tint);
            ctx.fill(kx - 2, cy - 5, kx + 2, cy + 6, isHovered() ? 0xFFFFFFFF : 0xFFDDE6EE);

            String s = String.valueOf(v);
            ctx.drawText(tr, s, getX() + getWidth() - tr.getWidth(s) - 4, cy - 4, MUTED, false);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (mouseX >= trackX() - 6) setFromMouse(mouseX);
        }

        @Override
        public void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
            setFromMouse(mouseX);
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        }
    }
}
