package pl.kingvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;

/** Trzy niezależne elementy HUD: Watermark (KingVisuals), FPS i Ping - każdy z własną pozycją i skalą. */
public final class Overlay {
    public static final int COUNT = 3;
    public static final String[] NAMES = {"Watermark", "FPS", "Ping"};

    /** Gdy true, elementy rysuje edytor HUD, a nie zwykły callback. */
    public static boolean editorOpen = false;

    private Overlay() {}

    /** {etykieta, wartość}; dla watermarku etykieta to "mark". */
    private static String[] text(MinecraftClient mc, int i) {
        if (i == 0) return new String[]{"mark", "KingVisuals"};
        if (i == 1) return new String[]{"FPS:", String.valueOf(mc.getCurrentFps())};
        int ping = 0;
        if (mc.player != null && mc.getNetworkHandler() != null) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (e != null) ping = e.getLatency();
        }
        return new String[]{"Ping:", ping + " ms"};
    }

    private static int width(TextRenderer tr, String[] t) {
        if (t[0].equals("mark")) return Ui.gradWidth(tr, t[1]);
        return tr.getWidth(t[0] + " " + t[1]);
    }

    /** Rozmiar elementu w pikselach ekranu (ze skalą). */
    public static int[] size(int i) {
        KvConfig c = KvMod.CONFIG;
        MinecraftClient mc = MinecraftClient.getInstance();
        float s = Math.max(0.5f, c.item(i).scalePercent / 100f);
        return new int[]{Math.round(width(mc.textRenderer, text(mc, i)) * s), Math.round(9 * s)};
    }

    public static void render(DrawContext ctx) {
        if (editorOpen) return;
        KvConfig c = KvMod.CONFIG;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || mc.player == null || mc.options.hudHidden) return;
        int sw = mc.getWindow().getScaledWidth();
        for (int i = 0; i < COUNT; i++) {
            KvConfig.HudItem it = c.item(i);
            if (!it.enabled) continue;
            int[] sz = size(i);
            draw(ctx, i, sw - it.offX - sz[0], it.offY);
        }
    }

    /** Rysuje element z lewym górnym rogiem w (x, y). */
    public static void draw(DrawContext ctx, int i, int x, int y) {
        KvConfig c = KvMod.CONFIG;
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        String[] t = text(mc, i);
        float s = Math.max(0.5f, c.item(i).scalePercent / 100f);

        MatrixStack m = ctx.getMatrices();
        m.push();
        m.scale(s, s, 1f);
        int bx = Math.round(x / s);
        int by = Math.round(y / s);
        if (t[0].equals("mark")) {
            Ui.gradText(ctx, tr, t[1], bx, by, Ui.PURPLE, Ui.BLUE, true);
        } else {
            ctx.drawText(tr, t[0], bx, by, Ui.LABEL, true);
            ctx.drawText(tr, t[1], bx + tr.getWidth(t[0] + " "), by, 0xFFFFFFFF, true);
        }
        m.pop();
    }
}
