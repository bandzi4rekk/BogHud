package pl.boghud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Rysuje elementy HUD-a jako ciemne ramki z pioruńmi w tle, każdą w swoim miejscu na ekranie. */
public final class HudRenderer {
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final int PAD = 3;
    private static final int ICON_W = 11;
    private static final int BOLT_YELLOW = 0xFFFFE135;
    private static final int KEYS_W = 58;
    private static final int KEYS_H = 70;

    /** Gdy true, HUD nie rysuje się sam - elementy rysuje edytor HUD. */
    public static boolean editorOpen = false;

    /** Piksel-art pioruna: {początek x, szerokość} dla każdego z 8 wierszy (szerokość całości 7). */
    private static final int[][] BOLT = {
            {4, 3}, {3, 3}, {2, 3}, {1, 5}, {3, 3}, {2, 3}, {1, 3}, {0, 2}
    };

    private HudRenderer() {}

    private static class Box {
        HudConfig.Element e;
        List<String[]> rows = new ArrayList<>();
        List<ItemStack> armor = new ArrayList<>();
        boolean icon;
        boolean keys;
        int w, h;
        float scale;
    }

    public static void drawBolt(DrawContext ctx, int x, int y, int color) {
        for (int i = 0; i < BOLT.length; i++) {
            ctx.fill(x + BOLT[i][0], y + i, x + BOLT[i][0] + BOLT[i][1], y + i + 1, color);
        }
    }

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Cps.poll(mc);

        HudConfig c = HudMod.CONFIG;
        if (c == null || !c.enabled || mc.player == null || mc.options.hudHidden) return;
        if (editorOpen) return; // edytor rysuje elementy sam

        TextRenderer tr = mc.textRenderer;
        for (int i = 0; i < HudConfig.ELEMENTS; i++) {
            HudConfig.Element e = c.elements[i];
            if (!e.enabled) continue;
            Box b = buildBox(mc, c, tr, i, false);
            if (b != null) drawBox(ctx, tr, b, e.x, e.y);
        }

        TargetHud.render(ctx, mc, c);
        CrosshairFx.render(ctx, mc, c);
    }

    // ------------------------------------------------------------------ API dla edytora i podglądów

    /** Rozmiar elementu w pikselach ekranu (z paddingiem i skalą) albo null. */
    public static int[] sizeOf(int idx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        HudConfig c = HudMod.CONFIG;
        if (c == null || mc.player == null || idx < 0 || idx >= HudConfig.ELEMENTS) return null;
        Box b = buildBox(mc, c, mc.textRenderer, idx, true);
        if (b == null) return null;
        return new int[]{Math.round((b.w + 2 * PAD) * b.scale), Math.round((b.h + 2 * PAD) * b.scale)};
    }

    /** Rysuje element w podanym miejscu (do edytora). */
    public static void drawAt(DrawContext ctx, int idx, int x, int y) {
        MinecraftClient mc = MinecraftClient.getInstance();
        HudConfig c = HudMod.CONFIG;
        if (c == null || mc.player == null || idx < 0 || idx >= HudConfig.ELEMENTS) return;
        Box b = buildBox(mc, c, mc.textRenderer, idx, true);
        if (b != null) drawBox(ctx, mc.textRenderer, b, x, y);
    }

    /** Podgląd jednego elementu (okno ustawień). Zwraca false, gdy nie ma czego pokazać. */
    public static boolean drawPreview(DrawContext ctx, int idx, int x, int y, float maxW, float maxH) {
        HudConfig c = HudMod.CONFIG;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || mc.player == null || idx < 0 || idx >= HudConfig.ELEMENTS) return false;
        TextRenderer tr = mc.textRenderer;
        Box b = buildBox(mc, c, tr, idx, true);
        if (b == null) return false;

        float bw = (b.w + 2 * PAD) * b.scale;
        float bh = (b.h + 2 * PAD) * b.scale;
        float pf = Math.min(1f, Math.min(maxW / bw, maxH / bh));

        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0f);
        m.scale(pf, pf, 1f);
        drawBox(ctx, tr, b, 0, 0);
        m.pop();
        return true;
    }

    // ------------------------------------------------------------------ budowanie i rysowanie ramek

    private static Box buildBox(MinecraftClient mc, HudConfig c, TextRenderer tr, int i, boolean editor) {
        if (mc.player == null) return null;
        Box b = new Box();
        b.e = c.elements[i];

        switch (i) {
            case 0:
                b.icon = true;
                b.rows.add(new String[]{mc.player.getName().getString(), ""});
                break;
            case 1:
                if (c.customText != null && !c.customText.isEmpty()) {
                    b.rows.add(new String[]{c.customText, ""});
                } else if (editor) {
                    b.rows.add(new String[]{"Tekst", ""});
                }
                break;
            case 2:
                b.rows.add(new String[]{"FPS:", String.valueOf(mc.getCurrentFps())});
                break;
            case 3: {
                int ping = 0;
                if (mc.getNetworkHandler() != null) {
                    PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                    if (entry != null) ping = entry.getLatency();
                }
                b.rows.add(new String[]{"Ping:", ping + " ms"});
                break;
            }
            case 4:
                b.rows.add(new String[]{"XYZ:", String.format(Locale.ROOT, "%.1f / %.1f / %.1f",
                        mc.player.getX(), mc.player.getY(), mc.player.getZ())});
                break;
            case 5:
                for (EquipmentSlot slot : ARMOR) {
                    ItemStack stack = mc.player.getEquippedStack(slot);
                    if (!stack.isEmpty()) b.armor.add(stack);
                }
                if (b.armor.isEmpty() && editor) {
                    // w edytorze i podglądzie pokazujemy przykładowy pancerz
                    b.armor.add(new ItemStack(Items.DIAMOND_HELMET));
                    b.armor.add(new ItemStack(Items.DIAMOND_CHESTPLATE));
                    b.armor.add(new ItemStack(Items.DIAMOND_LEGGINGS));
                    b.armor.add(new ItemStack(Items.DIAMOND_BOOTS));
                }
                break;
            case 6:
                b.rows.add(new String[]{"CPS:", Cps.left() + " | " + Cps.right()});
                break;
            default:
                b.keys = true;
                break;
        }
        if (!b.keys && b.rows.isEmpty() && b.armor.isEmpty()) return null;

        if (b.keys) {
            b.w = KEYS_W;
            b.h = KEYS_H;
        } else {
            int w = 0;
            for (String[] r : b.rows) {
                int rw = tr.getWidth(r[0]) + (r[1].isEmpty() ? 0 : tr.getWidth(" " + r[1]));
                if (b.icon) rw += ICON_W;
                w = Math.max(w, rw);
            }
            for (ItemStack s : b.armor) {
                w = Math.max(w, 20 + tr.getWidth(durabilityText(s)));
            }
            b.w = w;
            b.h = b.rows.size() * (tr.fontHeight + 1) + b.armor.size() * 18;
        }
        b.scale = Math.max(0.5f, b.e.scalePercent / 100f);
        return b;
    }

    private static void drawBox(DrawContext ctx, TextRenderer tr, Box b, int sx, int sy) {
        HudConfig.Element e = b.e;
        int accent = 0xFF000000 | e.accentColor;
        int text = 0xFF000000 | e.textColor;
        int lineH = tr.fontHeight + 1;
        float s = b.scale;

        MatrixStack m = ctx.getMatrices();
        m.push();
        m.scale(s, s, 1f);

        int left = Math.round(sx / s);
        int top = Math.round(sy / s);
        int boxW = b.w + 2 * PAD;
        int boxH = b.h + 2 * PAD;
        int x = left + PAD;
        int y = top + PAD;

        drawBackground(ctx, e, left, top, boxW, boxH);

        if (b.keys) {
            drawKeys(ctx, tr, e, left + (boxW - KEYS_W) / 2, y);
        } else {
            int cy = y;
            for (int ri = 0; ri < b.rows.size(); ri++) {
                String[] r = b.rows.get(ri);
                int tx = x;
                if (b.icon && ri == 0) {
                    drawBolt(ctx, x, cy, BOLT_YELLOW);
                    tx = x + ICON_W;
                }
                ctx.drawText(tr, r[0], tx, cy, accent, e.shadow);
                if (!r[1].isEmpty()) {
                    ctx.drawText(tr, r[1], tx + tr.getWidth(r[0] + " "), cy, text, e.shadow);
                }
                cy += lineH;
            }
            for (ItemStack st : b.armor) {
                ctx.drawItem(st, x, cy);
                ctx.drawText(tr, durabilityText(st), x + 20, cy + 4, text, e.shadow);
                cy += 18;
            }
        }

        m.pop();
    }

    /** Ciemne tło ramki z delikatnymi piorunami w kolorze akcentu. */
    public static void drawBackground(DrawContext ctx, HudConfig.Element e, int left, int top, int boxW, int boxH) {
        if (!e.background || e.backgroundAlpha <= 0) return;
        ctx.fill(left, top, left + boxW, top + boxH, (e.backgroundAlpha << 24) | 0x0A0E17);
        int boltColor = (Math.min(0x55, e.backgroundAlpha / 3) << 24) | e.accentColor;
        int k = 0;
        for (int bx = left + 5; bx + 7 <= left + boxW - 2; bx += 22, k++) {
            int by = top + (boxH - 8) / 2 + (k % 2 == 0 ? -1 : 1);
            by = Math.max(top + 1, Math.min(top + boxH - 9, by));
            drawBolt(ctx, bx, by, boltColor);
        }
    }

    /** Klawisze: W A S D, LPM / PPM i spacja. Wciśnięty klawisz świeci kolorem akcentu. */
    private static void drawKeys(DrawContext ctx, TextRenderer tr, HudConfig.Element e, int kx, int ky) {
        MinecraftClient mc = MinecraftClient.getInstance();
        key(ctx, tr, e, kx + 20, ky, 18, 18, "W", mc.options.forwardKey.isPressed());
        key(ctx, tr, e, kx, ky + 20, 18, 18, "A", mc.options.leftKey.isPressed());
        key(ctx, tr, e, kx + 20, ky + 20, 18, 18, "S", mc.options.backKey.isPressed());
        key(ctx, tr, e, kx + 40, ky + 20, 18, 18, "D", mc.options.rightKey.isPressed());
        key(ctx, tr, e, kx, ky + 40, 28, 18, "LPM", mc.options.attackKey.isPressed());
        key(ctx, tr, e, kx + 30, ky + 40, 28, 18, "PPM", mc.options.useKey.isPressed());
        key(ctx, tr, e, kx, ky + 60, 58, 10, "", mc.options.jumpKey.isPressed());
    }

    private static void key(DrawContext ctx, TextRenderer tr, HudConfig.Element e,
                            int x, int y, int w, int h, String label, boolean pressed) {
        int bg = pressed ? ((0xCC << 24) | e.accentColor) : 0x88000000;
        ctx.fill(x, y, x + w, y + h, bg);
        Ui.outline(ctx, x, y, w, h, (0x88 << 24) | e.accentColor);
        int fg = pressed ? 0xFF0A0E17 : (0xFF000000 | e.textColor);
        if (!label.isEmpty()) {
            ctx.drawText(tr, label, x + (w - tr.getWidth(label)) / 2, y + (h - 8) / 2 + 1, fg, false);
        } else {
            ctx.fill(x + w / 2 - 10, y + h / 2 - 1, x + w / 2 + 10, y + h / 2 + 1, fg);
        }
    }

    private static String durabilityText(ItemStack s) {
        if (!s.isDamageable()) return "";
        return (s.getMaxDamage() - s.getDamage()) + "/" + s.getMaxDamage();
    }
}
