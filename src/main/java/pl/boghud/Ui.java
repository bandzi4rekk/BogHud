package pl.boghud;

import net.minecraft.client.gui.DrawContext;

/** Kolory motywu GUI (zależne od ustawień użytkownika) i drobne narzędzia rysowania. */
public final class Ui {
    private Ui() {}

    private static HudConfig c() {
        return HudMod.CONFIG;
    }

    public static int mix(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static boolean light() {
        int bg = c().guiBackground;
        double l = (0.299 * ((bg >> 16) & 255) + 0.587 * ((bg >> 8) & 255) + 0.114 * (bg & 255)) / 255.0;
        return l > 0.6;
    }

    public static int accent() {
        return 0xFF000000 | c().guiAccent;
    }

    public static int bg() {
        return (c().guiAlpha << 24) | c().guiBackground;
    }

    public static int titleBg() {
        return 0xFF000000 | mix(c().guiBackground, c().guiAccent, 0.2);
    }

    public static int border() {
        return 0xFF000000 | mix(c().guiBackground, c().guiAccent, 0.5);
    }

    public static int off() {
        return 0xFF000000 | mix(c().guiBackground, light() ? 0x000000 : 0xFFFFFF, 0.2);
    }

    /** Kolor napisu leżącego na akcentowym tle (czarny lub biały - zależnie od jasności akcentu). */
    public static int onAccent() {
        int a = c().guiAccent;
        double l = (0.299 * ((a >> 16) & 255) + 0.587 * ((a >> 8) & 255) + 0.114 * (a & 255)) / 255.0;
        return l > 0.6 ? 0xFF10151A : 0xFFFFFFFF;
    }

    public static int text() {
        return light() ? 0xFF10151A : 0xFFE6EDF3;
    }

    public static int muted() {
        return light() ? 0xFF4A5560 : 0xFF8B98A5;
    }

    public static int hover() {
        return light() ? 0x22000000 : 0x22FFFFFF;
    }

    public static void outline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }
}
