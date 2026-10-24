package pl.boghud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HudRenderer {
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /** Ostatnio narysowany obszar HUD-a (w pikselach GUI) - używane do przeciągania myszką. */
    public static int boundsX, boundsY, boundsW, boundsH;

    private HudRenderer() {}

    public static boolean hit(double mx, double my) {
        return boundsW > 0 && mx >= boundsX && mx <= boundsX + boundsW
                && my >= boundsY && my <= boundsY + boundsH;
    }

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        boundsW = 0;
        boundsH = 0;
        HudConfig c = HudMod.CONFIG;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || !c.enabled || mc.player == null || mc.options.hudHidden) return;

        TextRenderer tr = mc.textRenderer;
        int accent = 0xFF000000 | c.accentColor;
        int text = 0xFF000000 | c.textColor;

        // Wiersze tekstowe: {etykieta, wartość}
        List<String[]> rows = new ArrayList<>();
        if (c.showText && c.customText != null && !c.customText.isEmpty()) {
            rows.add(new String[]{c.customText, ""});
        }
        if (c.showFps) {
            rows.add(new String[]{"FPS:", String.valueOf(mc.getCurrentFps())});
        }
        if (c.showPing) {
            int ping = 0;
            if (mc.getNetworkHandler() != null) {
                PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (entry != null) ping = entry.getLatency();
            }
            rows.add(new String[]{"Ping:", ping + " ms"});
        }
        if (c.showCoords) {
            rows.add(new String[]{"XYZ:", String.format(Locale.ROOT, "%.1f / %.1f / %.1f",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ())});
        }

        // Pancerz
        List<ItemStack> armor = new ArrayList<>();
        if (c.showArmor) {
            for (EquipmentSlot slot : ARMOR) {
                ItemStack stack = mc.player.getEquippedStack(slot);
                if (!stack.isEmpty()) armor.add(stack);
            }
        }

        int lineH = tr.fontHeight + 1;
        int armorH = 18;
        int pad = 3;

        int w = 0;
        for (String[] r : rows) {
            int rw = tr.getWidth(r[0]) + (r[1].isEmpty() ? 0 : tr.getWidth(" " + r[1]));
            w = Math.max(w, rw);
        }
        for (ItemStack s : armor) {
            w = Math.max(w, 20 + tr.getWidth(durabilityText(s)));
        }
        int h = rows.size() * lineH + armor.size() * armorH;
        if (w == 0 || h == 0) return;

        float scale = Math.max(0.5f, c.scalePercent / 100f);
        boundsX = Math.round(c.x - pad * scale);
        boundsY = Math.round(c.y - pad * scale);
        boundsW = Math.round((w + 2 * pad) * scale);
        boundsH = Math.round((h + 2 * pad) * scale);
        MatrixStack m = ctx.getMatrices();
        m.push();
        m.scale(scale, scale, 1f);

        int x = Math.round(c.x / scale);
        int y = Math.round(c.y / scale);

        if (c.background && c.backgroundAlpha > 0) {
            int bg = (c.backgroundAlpha << 24); // czarne tło z wybraną przezroczystością
            ctx.fill(x - pad, y - pad, x + w + pad, y + h + pad, bg);
        }

        int cy = y;
        for (String[] r : rows) {
            ctx.drawText(tr, r[0], x, cy, accent, c.shadow);
            if (!r[1].isEmpty()) {
                ctx.drawText(tr, r[1], x + tr.getWidth(r[0] + " "), cy, text, c.shadow);
            }
            cy += lineH;
        }
        for (ItemStack s : armor) {
            ctx.drawItem(s, x, cy);
            ctx.drawText(tr, durabilityText(s), x + 20, cy + 4, text, c.shadow);
            cy += armorH;
        }

        m.pop();
    }

    private static String durabilityText(ItemStack s) {
        if (!s.isDamageable()) return "";
        return (s.getMaxDamage() - s.getDamage()) + "/" + s.getMaxDamage();
    }
}
