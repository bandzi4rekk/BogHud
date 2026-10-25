package pl.boghud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;

import java.util.Locale;

/** Target HUD: ramka przy celowniku z nazwą celu, paskiem zdrowia i dystansem. */
public final class TargetHud {
    private static final int PAD = 3;
    private static LivingEntity lastEntity;
    private static float shownHp;

    private TargetHud() {}

    public static void render(DrawContext ctx, MinecraftClient mc, HudConfig c) {
        if (!c.target.enabled || mc.player == null) return;
        LivingEntity t = Target.get();
        if (t == null) {
            lastEntity = null;
            return;
        }
        if (t != lastEntity) {
            lastEntity = t;
            shownHp = t.getHealth();
        }
        shownHp += (t.getHealth() - shownHp) * 0.15f; // płynna zmiana paska

        int sx = mc.getWindow().getScaledWidth() / 2 + c.targetX;
        int sy = mc.getWindow().getScaledHeight() / 2 + c.targetY;
        draw(ctx, mc.textRenderer, c.target, t.getName().getString(), t.getHealth(), t.getMaxHealth(),
                t.getAbsorptionAmount(), shownHp, mc.player.distanceTo(t), sx, sy);
    }

    /** Podgląd z przykładowym celem (okno ustawień). */
    public static boolean drawPreview(DrawContext ctx, int x, int y, float maxW, float maxH) {
        HudConfig c = HudMod.CONFIG;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null) return false;
        TextRenderer tr = mc.textRenderer;

        int[] sz = measure(tr, "Steve", 14f, 20f, 0f, 3.2);
        float s = Math.max(0.5f, c.target.scalePercent / 100f);
        float bw = (sz[0] + 2 * PAD) * s;
        float bh = (sz[1] + 2 * PAD) * s;
        float pf = Math.min(1f, Math.min(maxW / bw, maxH / bh));

        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0f);
        m.scale(pf, pf, 1f);
        draw(ctx, tr, c.target, "Steve", 14f, 20f, 0f, 14f, 3.2, 0, 0);
        m.pop();
        return true;
    }

    private static String hpText(float hp, float maxHp, float abs) {
        String s = String.format(Locale.ROOT, "%.1f/%.0f", hp, maxHp);
        if (abs > 0) s += " +" + (int) abs;
        return s;
    }

    private static int[] measure(TextRenderer tr, String name, float hp, float maxHp, float abs, double dist) {
        String distS = String.format(Locale.ROOT, "%.1f m", dist);
        int w = Math.max(90, tr.getWidth(name));
        w = Math.max(w, tr.getWidth("HP: " + hpText(hp, maxHp, abs)));
        w = Math.max(w, tr.getWidth("Dystans: " + distS));
        int h = 27 + tr.fontHeight; // nazwa, HP, pasek, dystans
        return new int[]{w, h};
    }

    private static void draw(DrawContext ctx, TextRenderer tr, HudConfig.Element e, String name, float hp,
                             float maxHp, float abs, float shown, double dist, int sx, int sy) {
        if (maxHp <= 0) maxHp = 20f;
        String hpS = hpText(hp, maxHp, abs);
        String distS = String.format(Locale.ROOT, "%.1f m", dist);
        int[] sz = measure(tr, name, hp, maxHp, abs, dist);
        int w = sz[0], h = sz[1];

        float s = Math.max(0.5f, e.scalePercent / 100f);
        int accent = 0xFF000000 | e.accentColor;
        int text = 0xFF000000 | e.textColor;

        MatrixStack m = ctx.getMatrices();
        m.push();
        m.scale(s, s, 1f);

        int left = Math.round(sx / s);
        int top = Math.round(sy / s);
        HudRenderer.drawBackground(ctx, e, left, top, w + 2 * PAD, h + 2 * PAD);

        int x = left + PAD;
        int y = top + PAD;
        ctx.drawText(tr, name, x, y, accent, e.shadow);
        ctx.drawText(tr, "HP: ", x, y + 9, accent, e.shadow);
        ctx.drawText(tr, hpS, x + tr.getWidth("HP: "), y + 9, text, e.shadow);

        // pasek zdrowia: zielony -> czerwony
        int by = y + 19;
        float frac = Math.max(0f, Math.min(1f, shown / maxHp));
        int fw = Math.round(w * frac);
        int r = Math.min(255, (int) (510 * (1 - frac)));
        int g = Math.min(255, (int) (510 * frac));
        ctx.fill(x, by, x + w, by + 5, 0xFF141A24);
        ctx.fill(x, by, x + fw, by + 5, 0xFF000000 | (r << 16) | (g << 8) | 0x30);

        ctx.drawText(tr, "Dystans: ", x, y + 27, accent, e.shadow);
        ctx.drawText(tr, distS, x + tr.getWidth("Dystans: "), y + 27, text, e.shadow);

        m.pop();
    }
}
