package pl.boghud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Kółko wokół celownika (czerwone, gdy celujesz w stwora). */
public final class CrosshairFx {
    private CrosshairFx() {}

    public static void render(DrawContext ctx, MinecraftClient mc, HudConfig c) {
        if (c.cross == null || !c.cross.enabled || mc.currentScreen != null) return;
        if (!mc.options.getPerspective().isFirstPerson()) return;

        int cx = mc.getWindow().getScaledWidth() / 2;
        int cy = mc.getWindow().getScaledHeight() / 2;
        int col = 0xFF000000 | c.cross.color;
        if (c.cross.reactToTarget && Target.looking()) col = 0xFFFF3030;

        int r = Math.max(2, c.cross.radius);
        int th = Math.max(1, c.cross.thickness);
        int steps = Math.max(48, r * 8);
        int lastX = Integer.MIN_VALUE, lastY = Integer.MIN_VALUE;
        for (int i = 0; i < steps; i++) {
            double a = Math.PI * 2 * i / steps;
            int x = cx + (int) Math.round(Math.cos(a) * r) - th / 2;
            int y = cy + (int) Math.round(Math.sin(a) * r) - th / 2;
            if (x == lastX && y == lastY) continue;
            lastX = x;
            lastY = y;
            ctx.fill(x, y, x + th, y + th, col);
        }
    }
}
