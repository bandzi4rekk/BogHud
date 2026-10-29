package pl.kingvisuals;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Fioletowo-niebieski motyw i rysowanie zaokrąglonych kształtów oraz gradientów. */
public final class Ui {
    public static final int PURPLE = 0x8A4DFF;
    public static final int BLUE = 0x3D8BFF;
    public static final int TEXT = 0xFFF2F0FF;
    public static final int MUTED = 0xFF9A96B8;
    public static final int LABEL = 0xFFB07CFF;

    private Ui() {}

    public static int mix(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static int inset(int r, int dy) {
        double d = r - dy - 0.5;
        return (int) Math.round(r - Math.sqrt(Math.max(0, r * (double) r - d * d)));
    }

    /** Wypełniony prostokąt z zaokrąglonymi rogami (każdy piksel rysowany raz - działa z przezroczystością). */
    public static void roundRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int dy = 0; dy < r; dy++) {
            int in = inset(r, dy);
            ctx.fill(x + in, y + dy, x + w - in, y + dy + 1, color);
            ctx.fill(x + in, y + h - 1 - dy, x + w - in, y + h - dy, color);
        }
        ctx.fill(x, y + r, x + w, y + h - r, color);
    }

    /** Cienki obrys zaokrąglonego prostokąta. */
    public static void roundOutline(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        r = Math.max(1, Math.min(r, Math.min(w, h) / 2));
        int in0 = inset(r, 0);
        ctx.fill(x + in0, y, x + w - in0, y + 1, color);
        ctx.fill(x + in0, y + h - 1, x + w - in0, y + h, color);
        for (int dy = 1; dy < r; dy++) {
            int in = inset(r, dy);
            int prev = inset(r, dy - 1);
            int run = Math.max(1, prev - in);
            ctx.fill(x + in, y + dy, x + in + run, y + dy + 1, color);
            ctx.fill(x + w - in - run, y + dy, x + w - in, y + dy + 1, color);
            ctx.fill(x + in, y + h - 1 - dy, x + in + run, y + h - dy, color);
            ctx.fill(x + w - in - run, y + h - 1 - dy, x + w - in, y + h - dy, color);
        }
        ctx.fill(x, y + r, x + 1, y + h - r, color);
        ctx.fill(x + w - 1, y + r, x + w, y + h - r, color);
    }

    /** Pastylka z poziomym gradientem (np. zaznaczona zakładka, włączony przełącznik). */
    public static void gradPill(DrawContext ctx, int x, int y, int w, int h, int c1, int c2, int alpha) {
        int r = h / 2;
        for (int i = 0; i < w; i++) {
            double t = w <= 1 ? 0 : i / (double) (w - 1);
            int col = (alpha << 24) | mix(c1, c2, t);
            int vin = 0;
            if (i < r) vin = inset(r, i);
            else if (i >= w - r) vin = inset(r, w - 1 - i);
            ctx.fill(x + i, y + vin, x + i + 1, y + h - vin, col);
        }
    }

    /** Prostokąt z poziomym gradientem. */
    public static void hGradient(DrawContext ctx, int x, int y, int w, int h, int c1, int c2, int alpha) {
        for (int i = 0; i < w; i++) {
            double t = w <= 1 ? 0 : i / (double) (w - 1);
            ctx.fill(x + i, y, x + i + 1, y + h, (alpha << 24) | mix(c1, c2, t));
        }
    }

    /** Pionowy pasek z gradientem (akcent po lewej stronie włączonego modułu). */
    public static void vGradient(DrawContext ctx, int x, int y, int w, int h, int c1, int c2) {
        for (int i = 0; i < h; i++) {
            double t = h <= 1 ? 0 : i / (double) (h - 1);
            ctx.fill(x, y + i, x + w, y + i + 1, 0xFF000000 | mix(c1, c2, t));
        }
    }

    /** Tekst z gradientem po literach. Zwraca szerokość. */
    public static int gradText(DrawContext ctx, TextRenderer tr, String s, int x, int y, int c1, int c2, boolean shadow) {
        int cx = x;
        int n = s.length();
        for (int i = 0; i < n; i++) {
            String ch = String.valueOf(s.charAt(i));
            int col = 0xFF000000 | mix(c1, c2, n <= 1 ? 0 : i / (double) (n - 1));
            ctx.drawText(tr, ch, cx, y, col, shadow);
            cx += tr.getWidth(ch);
        }
        return cx - x;
    }

    public static int gradWidth(TextRenderer tr, String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) w += tr.getWidth(String.valueOf(s.charAt(i)));
        return w;
    }

    public static void outline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }
}
