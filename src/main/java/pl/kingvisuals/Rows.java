package pl.kingvisuals;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Wiersze ustawień modułu (przełączniki, suwaki, przyciski wyboru) w fioletowo-niebieskim stylu. */
public final class Rows {
    private Rows() {}

    public abstract static class Row {
        final int h;

        Row(int h) {
            this.h = h;
        }

        int labelWidth(TextRenderer tr) {
            return 0;
        }

        abstract void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int lw, int mx, int my);

        void click(double mx, double my, int x, int w, int lw) {
        }

        void drag(double mx, double my, int x, int w, int lw) {
        }
    }

    /** Przełącznik w stylu pastylki z gradientem. */
    static void toggle(DrawContext ctx, int x, int y, boolean on) {
        if (on) {
            Ui.gradPill(ctx, x, y, 24, 12, Ui.PURPLE, Ui.BLUE, 0xFF);
        } else {
            Ui.gradPill(ctx, x, y, 24, 12, 0x4A4766, 0x4A4766, 0xFF);
        }
        int kx = on ? x + 24 - 10 : x + 2;
        Ui.roundRect(ctx, kx, y + 2, 8, 8, 3, 0xFFFFFFFF);
    }

    public static class ToggleRow extends Row {
        private final String label;
        private final BooleanSupplier get;
        private final Consumer<Boolean> set;

        ToggleRow(String label, BooleanSupplier get, Consumer<Boolean> set) {
            super(16);
            this.label = label;
            this.get = get;
            this.set = set;
        }

        @Override
        void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int lw, int mx, int my) {
            if (mx >= x && mx < x + w && my >= y && my < y + h) {
                Ui.roundRect(ctx, x, y, w, h, 4, 0x22FFFFFF);
            }
            ctx.drawText(tr, label, x + 3, y + 4, Ui.TEXT, false);
            toggle(ctx, x + w - 28, y + 2, get.getAsBoolean());
        }

        @Override
        void click(double mx, double my, int x, int w, int lw) {
            set.accept(!get.getAsBoolean());
        }
    }

    public static class SliderRow extends Row {
        private final String label;
        private final int min, max, tint;
        private final IntSupplier get;
        private final IntConsumer set;

        SliderRow(String label, int min, int max, IntSupplier get, IntConsumer set, int tint) {
            super(14);
            this.label = label;
            this.min = min;
            this.max = max;
            this.get = get;
            this.set = set;
            this.tint = tint;
        }

        @Override
        int labelWidth(TextRenderer tr) {
            return tr.getWidth(label) + 8;
        }

        private void setFromMouse(double mx, int x, int w, int lw) {
            int tw = w - lw - 28;
            double t = (mx - (x + lw)) / tw;
            t = Math.max(0, Math.min(1, t));
            set.accept((int) Math.round(min + t * (max - min)));
        }

        @Override
        void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int lw, int mx, int my) {
            int cy = y + h / 2;
            ctx.drawText(tr, label, x + 3, cy - 4, Ui.TEXT, false);
            int tx = x + lw;
            int tw = w - lw - 28;
            int v = Math.max(min, Math.min(max, get.getAsInt()));
            double t = (v - min) / (double) (max - min);
            int fw = (int) Math.round(t * tw);
            ctx.fill(tx, cy - 1, tx + tw, cy + 2, 0x40FFFFFF);
            if (fw > 0) {
                if (tint == 0) Ui.hGradient(ctx, tx, cy - 1, fw, 3, Ui.PURPLE, Ui.BLUE, 0xFF);
                else ctx.fill(tx, cy - 1, tx + fw, cy + 2, tint);
            }
            Ui.roundRect(ctx, tx + fw - 3, cy - 5, 6, 10, 3, 0xFFFFFFFF);
            String s = String.valueOf(v);
            ctx.drawText(tr, s, x + w - tr.getWidth(s) - 2, cy - 4, Ui.MUTED, false);
        }

        @Override
        void click(double mx, double my, int x, int w, int lw) {
            if (mx >= x + lw - 6) setFromMouse(mx, x, w, lw);
        }

        @Override
        void drag(double mx, double my, int x, int w, int lw) {
            setFromMouse(mx, x, w, lw);
        }
    }

    public static class HeadingRow extends Row {
        private final String text;
        private final IntSupplier swatch;

        HeadingRow(String text, IntSupplier swatch) {
            super(14);
            this.text = text;
            this.swatch = swatch;
        }

        @Override
        void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int lw, int mx, int my) {
            ctx.drawText(tr, text, x + 3, y + 3, Ui.LABEL, false);
            if (swatch != null) {
                Ui.roundRect(ctx, x + w - 22, y + 1, 20, 12, 3, 0x55FFFFFF);
                Ui.roundRect(ctx, x + w - 21, y + 2, 18, 10, 3, 0xFF000000 | swatch.getAsInt());
            }
        }
    }

    public static class TextRow extends Row {
        private final String text;

        TextRow(String text) {
            super(11);
            this.text = text;
        }

        @Override
        void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int lw, int mx, int my) {
            ctx.drawText(tr, text, x + 3, y + 2, Ui.MUTED, false);
        }
    }

    /** Rząd przycisków wyboru jednej wartości (np. rodzaj cząsteczek). */
    public static class ChoiceRow extends Row {
        private final int[] values;
        private final String[] labels;
        private final IntSupplier get;
        private final IntConsumer set;

        ChoiceRow(int[] values, String[] labels, IntSupplier get, IntConsumer set) {
            super(18);
            this.values = values;
            this.labels = labels;
            this.get = get;
            this.set = set;
        }

        @Override
        void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int lw, int mx, int my) {
            int cw = w / values.length;
            for (int i = 0; i < values.length; i++) {
                int bx = x + i * cw;
                boolean sel = get.getAsInt() == values[i];
                boolean hover = mx >= bx && mx < bx + cw && my >= y + 1 && my < y + h - 1;
                if (sel) Ui.gradPill(ctx, bx + 1, y + 2, cw - 2, h - 4, Ui.PURPLE, Ui.BLUE, 0xFF);
                else Ui.roundRect(ctx, bx + 1, y + 2, cw - 2, h - 4, 5, hover ? 0x44FFFFFF : 0x28FFFFFF);
                String t = labels[i];
                ctx.drawText(tr, t, bx + (cw - tr.getWidth(t)) / 2, y + 5, sel ? 0xFFFFFFFF : Ui.TEXT, false);
            }
        }

        @Override
        void click(double mx, double my, int x, int w, int lw) {
            int cw = w / values.length;
            int i = (int) ((mx - x) / cw);
            if (i >= 0 && i < values.length) set.accept(values[i]);
        }
    }

    /** Gotowe kolory do kliknięcia. */
    public static class PresetRow extends Row {
        private final int[] colors;
        private final IntConsumer set;

        PresetRow(int[] colors, IntConsumer set) {
            super(18);
            this.colors = colors;
            this.set = set;
        }

        @Override
        void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int lw, int mx, int my) {
            for (int i = 0; i < colors.length; i++) {
                int bx = x + 3 + i * 16;
                Ui.roundRect(ctx, bx, y + 3, 12, 12, 3, 0xFF000000 | colors[i]);
                if (mx >= bx && mx < bx + 12 && my >= y + 3 && my < y + 15) {
                    Ui.roundOutline(ctx, bx - 1, y + 2, 14, 14, 4, 0xFFFFFFFF);
                }
            }
        }

        @Override
        void click(double mx, double my, int x, int w, int lw) {
            double rel = mx - (x + 3);
            if (rel < 0) return;
            int i = (int) (rel / 16);
            if (i < colors.length && rel - i * 16 < 12) set.accept(colors[i]);
        }
    }
}
