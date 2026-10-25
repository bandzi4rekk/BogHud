package pl.boghud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Wiersze używane w ekranach ustawień i edytora (przełączniki, suwaki, przyciski...). */
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
                ctx.fill(x, y, x + w, y + h, Ui.hover());
            }
            ctx.drawText(tr, label, x + 2, y + 4, Ui.text(), false);
            boolean on = get.getAsBoolean();
            int sw = 22, sh = 10;
            int sx = x + w - sw - 2;
            int sy = y + 3;
            ctx.fill(sx, sy, sx + sw, sy + sh, on ? Ui.accent() : Ui.off());
            int kx = on ? sx + sw - sh + 1 : sx + 1;
            ctx.fill(kx, sy + 1, kx + sh - 2, sy + sh - 1, 0xFFFFFFFF);
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
            return tr.getWidth(label) + 6;
        }

        private void setFromMouse(double mx, int x, int w, int lw) {
            int tw = w - lw - 26;
            double t = (mx - (x + lw)) / tw;
            t = Math.max(0, Math.min(1, t));
            set.accept((int) Math.round(min + t * (max - min)));
        }

        @Override
        void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int lw, int mx, int my) {
            int cy = y + h / 2;
            ctx.drawText(tr, label, x + 2, cy - 4, Ui.text(), false);
            int tx = x + lw;
            int tw = w - lw - 26;
            int v = Math.max(min, Math.min(max, get.getAsInt()));
            double t = (v - min) / (double) (max - min);
            int kx = tx + (int) Math.round(t * tw);
            ctx.fill(tx, cy - 1, tx + tw, cy + 2, Ui.off());
            ctx.fill(tx, cy - 1, kx, cy + 2, tint == 0 ? Ui.accent() : tint);
            ctx.fill(kx - 2, cy - 5, kx + 2, cy + 6, 0xFFFFFFFF);
            String s = String.valueOf(v);
            ctx.drawText(tr, s, x + w - tr.getWidth(s) - 2, cy - 4, Ui.muted(), false);
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
            ctx.drawText(tr, text, x + 2, y + 3, Ui.accent(), false);
            if (swatch != null) {
                int sx = x + w - 22;
                ctx.fill(sx - 1, y + 1, sx + 19, y + 13, Ui.border());
                ctx.fill(sx, y + 2, sx + 18, y + 12, 0xFF000000 | swatch.getAsInt());
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
            ctx.drawText(tr, text, x + 2, y + 2, Ui.muted(), false);
        }
    }

    public static class ButtonRow extends Row {
        private final String label;
        private final Runnable action;

        ButtonRow(String label, Runnable action) {
            super(20);
            this.label = label;
            this.action = action;
        }

        @Override
        void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int lw, int mx, int my) {
            boolean hover = mx >= x && mx < x + w && my >= y + 2 && my < y + h;
            ctx.fill(x, y + 2, x + w, y + h - 1, hover ? Ui.border() : Ui.off());
            ctx.drawText(tr, label, x + (w - tr.getWidth(label)) / 2, y + 7, Ui.text(), false);
        }

        @Override
        void click(double mx, double my, int x, int w, int lw) {
            action.run();
        }
    }

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
                int bx = x + 2 + i * 16;
                ctx.fill(bx, y + 3, bx + 12, y + 15, 0xFF000000 | colors[i]);
                if (mx >= bx && mx < bx + 12 && my >= y + 3 && my < y + 15) {
                    Ui.outline(ctx, bx - 1, y + 2, 14, 14, Ui.text());
                }
            }
        }

        @Override
        void click(double mx, double my, int x, int w, int lw) {
            double rel = mx - (x + 2);
            if (rel < 0) return;
            int i = (int) (rel / 16);
            if (i < colors.length && rel - i * 16 < 12) set.accept(colors[i]);
        }
    }

    /** Gotowe motywy: tło + akcent jednym kliknięciem. */
    public static class ThemeRow extends Row {
        /** {akcent, tło} */
        static final int[][] T = {
                {0xFF4D6D, 0x07182E}, {0x5CE1E6, 0x141A22}, {0xB388FF, 0x120E24},
                {0x66E08A, 0x0B1A14}, {0xFFE135, 0x14120A}
        };
        private final IntConsumer pick;

        ThemeRow(IntConsumer pick) {
            super(20);
            this.pick = pick;
        }

        @Override
        void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int lw, int mx, int my) {
            for (int i = 0; i < T.length; i++) {
                int bx = x + 2 + i * 24;
                ctx.fill(bx, y + 3, bx + 20, y + 17, 0xFF000000 | T[i][1]);
                ctx.fill(bx + 4, y + 8, bx + 16, y + 12, 0xFF000000 | T[i][0]);
                Ui.outline(ctx, bx, y + 3, 20, 14, 0xFF000000 | T[i][0]);
                if (mx >= bx && mx < bx + 20 && my >= y + 3 && my < y + 17) {
                    Ui.outline(ctx, bx - 1, y + 2, 22, 16, Ui.text());
                }
            }
        }

        @Override
        void click(double mx, double my, int x, int w, int lw) {
            double rel = mx - (x + 2);
            if (rel < 0) return;
            int i = (int) (rel / 24);
            if (i < T.length && rel - i * 24 < 20) pick.accept(i);
        }
    }

    /** Rząd przycisków wyboru jednej wartości (np. rozmiar GUI, proporcje ekranu). */
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
                boolean hover = mx >= bx && mx < bx + cw && my >= y + 2 && my < y + h - 2;
                ctx.fill(bx + 1, y + 2, bx + cw - 1, y + h - 2, sel ? Ui.accent() : (hover ? Ui.border() : Ui.off()));
                String t = labels != null ? labels[i] : String.valueOf(values[i]);
                ctx.drawText(tr, t, bx + (cw - tr.getWidth(t)) / 2, y + 5, sel ? Ui.onAccent() : Ui.text(), false);
            }
        }

        @Override
        void click(double mx, double my, int x, int w, int lw) {
            int cw = w / values.length;
            int i = (int) ((mx - x) / cw);
            if (i >= 0 && i < values.length) set.accept(values[i]);
        }
    }

    /** Miejsce na pole tekstowe (samo pole jest zwykłym widgetem ustawianym w render). */
    public static class FieldRow extends Row {
        boolean shown;
        int x, y, w;

        FieldRow() {
            super(22);
        }

        @Override
        void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int lw, int mx, int my) {
            this.shown = true;
            this.x = x;
            this.y = y;
            this.w = w;
        }
    }
}
