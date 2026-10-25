package pl.boghud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import pl.boghud.Rows.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Edytor HUD: wszystkie elementy widać na ekranie i przeciąga się je myszką (albo strzałkami).
 * Po prawej lista elementów (włączanie, wybór) i ustawienia wybranego elementu.
 */
public class HudEditorScreen extends Screen {
    private static final int N = HudConfig.ELEMENTS + 1; // elementy + Target HUD
    private static final String[] LABELS = {"Nick", "Tekst", "FPS", "Ping", "Koordynaty", "Pancerz", "CPS",
            "Klawisze", "Target HUD"};
    private static final int PANEL_W = 136;

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private int selected = 0;
    private int dragIdx = -1;
    private int dragOffX, dragOffY;
    private boolean grid = true;
    private boolean guideX, guideY;
    private final int[] bx = new int[N], by = new int[N], bw = new int[N], bh = new int[N];
    private int[] rowY = new int[0];
    private int rowX, rowW, rowLw;
    private Row activeRow;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("Edytor HUD"));
        this.parent = parent;
    }

    private HudConfig c() {
        return HudMod.CONFIG;
    }

    private boolean isTarget(int i) {
        return i >= HudConfig.ELEMENTS;
    }

    private HudConfig.Element elem(int i) {
        return isTarget(i) ? c().target : c().elements[i];
    }

    private int posX(int i) {
        return isTarget(i) ? this.width / 2 + c().targetX : c().elements[i].x;
    }

    private int posY(int i) {
        return isTarget(i) ? this.height / 2 + c().targetY : c().elements[i].y;
    }

    private void setPos(int i, int x, int y) {
        if (isTarget(i)) {
            c().targetX = x - this.width / 2;
            c().targetY = y - this.height / 2;
        } else {
            c().elements[i].x = x;
            c().elements[i].y = y;
        }
    }

    @Override
    protected void init() {
        HudRenderer.editorOpen = true;
        rows.clear();
        rows.add(new SliderRow("Skala %", 50, 200, () -> elem(selected).scalePercent,
                v -> elem(selected).scalePercent = v, 0));
        rows.add(new ToggleRow("Siatka (8 px)", () -> grid, v -> grid = v));
        rows.add(new ButtonRow("Ułóż pod sobą", () -> c().resetPositions()));
        rows.add(new ButtonRow("Gotowe", this::close));
        rows.add(new TextRow("Strzałki: 1 px, z Shift: 8 px."));
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        TextRenderer tr = this.textRenderer;
        ctx.fill(0, 0, this.width, this.height, 0x77000000);

        if (grid) {
            for (int x = 0; x < this.width; x += 16) ctx.fill(x, 0, x + 1, this.height, 0x14FFFFFF);
            for (int y = 0; y < this.height; y += 16) ctx.fill(0, y, this.width, y + 1, 0x14FFFFFF);
        }
        int accent = Ui.accent();

        // elementy
        for (int i = 0; i < N; i++) {
            bw[i] = 0;
            if (!elem(i).enabled) continue;
            int[] sz = isTarget(i) ? TargetHud.size() : HudRenderer.sizeOf(i);
            if (sz == null) continue;
            int x = posX(i), y = posY(i);
            bx[i] = x;
            by[i] = y;
            bw[i] = sz[0];
            bh[i] = sz[1];
            if (isTarget(i)) TargetHud.drawAt(ctx, x, y);
            else HudRenderer.drawAt(ctx, i, x, y);
        }

        // prowadnice środka ekranu
        if (guideX) ctx.fill(this.width / 2, 0, this.width / 2 + 1, this.height, 0xAA000000 | (accent & 0xFFFFFF));
        if (guideY) ctx.fill(0, this.height / 2, this.width, this.height / 2 + 1, 0xAA000000 | (accent & 0xFFFFFF));

        // obrysy i podpisy
        for (int i = 0; i < N; i++) {
            if (bw[i] <= 0) continue;
            boolean hover = mx >= bx[i] && mx < bx[i] + bw[i] && my >= by[i] && my < by[i] + bh[i];
            boolean sel = i == selected;
            int col = sel ? accent : (hover ? 0xAAFFFFFF : 0x55FFFFFF);
            Ui.outline(ctx, bx[i] - 1, by[i] - 1, bw[i] + 2, bh[i] + 2, col);
            if (sel || hover) {
                String label = LABELS[i] + "  " + posX(i) + ", " + posY(i);
                int lw = tr.getWidth(label);
                int ly = by[i] - 11 < 0 ? by[i] + bh[i] + 2 : by[i] - 11;
                ctx.fill(bx[i] - 1, ly, bx[i] + lw + 5, ly + 10, 0xCC000000);
                ctx.drawText(tr, label, bx[i] + 2, ly + 1, sel ? accent : 0xFFFFFFFF, false);
            }
        }

        // panel po prawej
        int px = this.width - PANEL_W - 8;
        int py = 8;
        int ph = this.height - 16;
        Ui.roundRect(ctx, px, py, PANEL_W, ph, Ui.cardBorder());
        Ui.roundRect(ctx, px + 1, py + 1, PANEL_W - 2, ph - 2, Ui.cardBg());
        ctx.drawText(tr, "EDYTOR HUD", px + 8, py + 8, Ui.accent(), false);

        int ly = py + 22;
        for (int i = 0; i < N; i++) {
            int ry = ly + i * 13;
            boolean sel = i == selected;
            if (sel) ctx.fill(px + 4, ry, px + PANEL_W - 4, ry + 12, Ui.hover());
            ctx.drawText(tr, LABELS[i], px + 8, ry + 2, sel ? Ui.accent() : Ui.text(), false);
            boolean on = elem(i).enabled;
            int sw = 22, sh = 10;
            int sx = px + PANEL_W - 6 - sw;
            int sy = ry + 1;
            ctx.fill(sx, sy, sx + sw, sy + sh, on ? Ui.accent() : Ui.off());
            int kx = on ? sx + sw - sh + 1 : sx + 1;
            ctx.fill(kx, sy + 1, kx + sh - 2, sy + sh - 1, 0xFFFFFFFF);
        }

        int ry0 = ly + N * 13 + 8;
        ctx.fill(px + 6, ry0 - 4, px + PANEL_W - 6, ry0 - 3, Ui.border());
        rowX = px + 4;
        rowW = PANEL_W - 8;
        int lw = 0;
        for (Row r : rows) lw = Math.max(lw, r.labelWidth(tr));
        rowLw = lw;
        rowY = new int[rows.size()];
        int y = ry0;
        for (int i = 0; i < rows.size(); i++) {
            rowY[i] = y;
            rows.get(i).render(ctx, tr, rowX, y, rowW, rowLw, mx, my);
            y += rows.get(i).h;
        }

        super.render(ctx, mx, my, delta);
    }

    private void move(int dx, int dy) {
        if (bw[selected] <= 0) return;
        int nx = Math.max(0, Math.min(this.width - bw[selected], posX(selected) + dx));
        int ny = Math.max(0, Math.min(this.height - bh[selected], posY(selected) + dy));
        setPos(selected, nx, ny);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        int px = this.width - PANEL_W - 8;
        int py = 8;
        if (mx >= px && mx < px + PANEL_W && my >= py && my < this.height - 8) {
            int ly = py + 22;
            for (int i = 0; i < N; i++) {
                int ry = ly + i * 13;
                if (my >= ry && my < ry + 12) {
                    if (mx >= px + PANEL_W - 30) elem(i).enabled = !elem(i).enabled;
                    else selected = i;
                    return true;
                }
            }
            for (int i = 0; i < rows.size(); i++) {
                Row r = rows.get(i);
                if (my >= rowY[i] && my < rowY[i] + r.h) {
                    activeRow = r;
                    r.click(mx, my, rowX, rowW, rowLw);
                    return true;
                }
            }
            return true;
        }

        for (int i = N - 1; i >= 0; i--) {
            if (bw[i] > 0 && mx >= bx[i] && mx < bx[i] + bw[i] && my >= by[i] && my < by[i] + bh[i]) {
                selected = i;
                dragIdx = i;
                dragOffX = (int) mx - bx[i];
                dragOffY = (int) my - by[i];
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0 && activeRow != null) {
            activeRow.drag(mx, my, rowX, rowW, rowLw);
            return true;
        }
        if (button == 0 && dragIdx >= 0) {
            int i = dragIdx;
            int nx = (int) Math.round(mx) - dragOffX;
            int ny = (int) Math.round(my) - dragOffY;
            if (grid) {
                nx = Math.round(nx / 8f) * 8;
                ny = Math.round(ny / 8f) * 8;
            }
            guideX = false;
            guideY = false;
            if (Math.abs(nx + bw[i] / 2 - this.width / 2) <= 3) {
                nx = this.width / 2 - bw[i] / 2;
                guideX = true;
            }
            if (Math.abs(ny + bh[i] / 2 - this.height / 2) <= 3) {
                ny = this.height / 2 - bh[i] / 2;
                guideY = true;
            }
            nx = Math.max(0, Math.min(Math.max(0, this.width - bw[i]), nx));
            ny = Math.max(0, Math.min(Math.max(0, this.height - bh[i]), ny));
            setPos(i, nx, ny);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        activeRow = null;
        dragIdx = -1;
        guideX = false;
        guideY = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int step = (modifiers & 1) != 0 ? 8 : 1; // Shift
        if (keyCode == 262) {
            move(step, 0);
            return true;
        }
        if (keyCode == 263) {
            move(-step, 0);
            return true;
        }
        if (keyCode == 264) {
            move(0, step);
            return true;
        }
        if (keyCode == 265) {
            move(0, -step);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        HudRenderer.editorOpen = false;
        c().save();
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }
}
