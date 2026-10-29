package pl.kingvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.List;

/** Napis w prawym górnym rogu: KingVisuals (fioletowy gradient), FPS i ping. */
public final class Overlay {
    /** Gdy true, overlay rysuje edytor HUD, a nie zwykły callback. */
    public static boolean editorOpen = false;

    private Overlay() {}

    private static List<String[]> lines(MinecraftClient mc, KvConfig c) {
        List<String[]> l = new ArrayList<>();
        if (c.overlay.watermark) l.add(new String[]{"mark", "KingVisuals"});
        if (c.overlay.fps) l.add(new String[]{"FPS:", String.valueOf(mc.getCurrentFps())});
        if (c.overlay.ping) {
            int ping = 0;
            if (mc.player != null && mc.getNetworkHandler() != null) {
                PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (e != null) ping = e.getLatency();
            }
            l.add(new String[]{"Ping:", ping + " ms"});
        }
        return l;
    }

    private static int lineWidth(TextRenderer tr, String[] l) {
        if (l[0].equals("mark")) return Ui.gradWidth(tr, l[1]);
        return tr.getWidth(l[0] + " " + l[1]);
    }

    /** Rozmiar overlayu w pikselach ekranu (ze skalą) albo null, gdy nic nie jest włączone. */
    public static int[] size() {
        KvConfig c = KvMod.CONFIG;
        MinecraftClient mc = MinecraftClient.getInstance();
        List<String[]> ls = lines(mc, c);
        if (ls.isEmpty()) return null;
        int w = 0;
        for (String[] l : ls) w = Math.max(w, lineWidth(mc.textRenderer, l));
        float s = Math.max(0.5f, c.overlay.scalePercent / 100f);
        return new int[]{Math.round(w * s), Math.round(ls.size() * 10 * s)};
    }

    public static void render(DrawContext ctx) {
        if (editorOpen) return;
        KvConfig c = KvMod.CONFIG;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || mc.player == null || mc.options.hudHidden) return;
        int[] sz = size();
        if (sz == null) return;
        int rightX = mc.getWindow().getScaledWidth() - c.overlay.offX;
        draw(ctx, rightX - sz[0], c.overlay.offY);
    }

    /** Rysuje overlay z lewym górnym rogiem w (x, y). */
    public static void draw(DrawContext ctx, int x, int y) {
        KvConfig c = KvMod.CONFIG;
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        List<String[]> ls = lines(mc, c);
        if (ls.isEmpty()) return;

        int w = 0;
        for (String[] l : ls) w = Math.max(w, lineWidth(tr, l));
        float s = Math.max(0.5f, c.overlay.scalePercent / 100f);

        MatrixStack m = ctx.getMatrices();
        m.push();
        m.scale(s, s, 1f);
        int bx = Math.round(x / s);
        int by = Math.round(y / s);
        int cy = by;
        for (String[] l : ls) {
            int lw = lineWidth(tr, l);
            int lx = bx + (w - lw); // wyrównanie do prawej
            if (l[0].equals("mark")) {
                Ui.gradText(ctx, tr, l[1], lx, cy, Ui.PURPLE, Ui.BLUE, true);
            } else {
                ctx.drawText(tr, l[0], lx, cy, Ui.LABEL, true);
                ctx.drawText(tr, l[1], lx + tr.getWidth(l[0] + " "), cy, 0xFFFFFFFF, true);
            }
            cy += 10;
        }
        m.pop();
    }
}
