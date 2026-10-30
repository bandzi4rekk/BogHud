package pl.kingvisuals;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import pl.kingvisuals.Rows.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Edytuj HUD: Watermark, FPS i Ping to osobne elementy - każdy przeciągasz myszką (albo strzałkami)
 * w dowolne miejsce, włączasz osobno i skalujesz osobno.
 */
public class KvHudEditor extends Screen {
    private static final int N = Overlay.COUNT;
    private static final int PANEL_W = 124;

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private int selected = 0;
    private int dragIdx = -1;
    private int dragOffX, dragOffY;
    private final int[] bx = new int[N], by = new int[N], bw = new int[N], bh = new int[N];
    private int[] rowY = new int[0];
    private int rowX, rowW, rowLw;
    private Row activeRow;

    public KvHudEditor(Screen parent) {
        super(Text.literal("Edytuj HUD"));
        this.parent = parent;
    }

    private KvConfig c() {
        return KvMod.CONFIG;
    }

    @Override
    protected void init() {
        Overlay.editorOpen = true;
        rows.clear();
        rows.add(new SliderRow("Skala %", 50, 200, () -> c().item(selected).scalePercent,
                v -> c().item(selected).scalePercent = v, 0));
        rows.add(new ButtonRow("Reset pozycji", () -> c().resetHud()));
        rows.add(new ButtonRow("Gotowe", this::close));
        rows.add(new TextRow("Strzałki: 1 px (Shift: 8 px)."));
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    private int posX(int i) {
        return this.width - c().item(i).offX - bw[i];
    }

    private void setPos(int i, int x, int y) {
        c().item(i).offX = this.width - (x + bw[i]);
        c().item(i).offY = y;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        TextRenderer tr = this.textRenderer;
        ctx.fill(0, 0, this.width, this.height, 0x77080618);

        // elementy HUD
        for (int i = 0; i < N; i++) {
            bw[i] = 0;
            KvConfig.HudItem it = c().item(i);
            if (!it.enabled) continue;
            int[] sz = Overlay.size(i);
            bw[i] = sz[0];
            bh[i] = sz[1];
            bx[i] = posX(i);
            by[i] = it.offY;
            Overlay.draw(ctx, i, bx[i], by[i]);
        }
        for (int i = 0; i < N; i++) {
            if (bw[i] <= 0) continue;
            boolean hover = mx >= bx[i] - 3 && mx < bx[i] + bw[i] + 3 && my >= by[i] - 3 && my < by[i] + bh[i] + 3;
            boolean sel = i == selected;
            int col = sel ? 0xFFB07CFF : (hover ? 0xAAFFFFFF : 0x55FFFFFF);
            Ui.roundOutline(ctx, bx[i] - 4, by[i] - 4, bw[i] + 8, bh[i] + 8, 4, col);
        }

        // panel po lewej
        int px = 10, py = 10;
        int ph = Math.min(this.height - 20, 170);
        Ui.roundRect(ctx, px, py, PANEL_W, ph, 10, 0xC0140E30);
        Ui.roundOutline(ctx, px, py, PANEL_W, ph, 10, 0x40FFFFFF);
        ctx.drawText(tr, "Edytuj HUD", px + 10, py + 8, Ui.LABEL, false);

        int ly = py + 22;
        for (int i = 0; i < N; i++) {
            int ry = ly + i * 14;
            boolean sel = i == selected;
            if (sel) Ui.roundRect(ctx, px + 4, ry, PANEL_W - 8, 13, 4, 0x33B07CFF);
            ctx.drawText(tr, Overlay.NAMES[i], px + 10, ry + 3, sel ? Ui.TEXT : Ui.MUTED, false);
            Rows.toggle(ctx, px + PANEL_W - 34, ry + 0, c().item(i).enabled);
        }

        int ry0 = ly + N * 14 + 8;
        ctx.fill(px + 8, ry0 - 4, px + PANEL_W - 8, ry0 - 3, 0x28FFFFFF);
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

        String hint = "Przeciągnij element, żeby go przesunąć.  Esc: wróć.";
        ctx.drawText(tr, hint, (this.width - tr.getWidth(hint)) / 2, this.height - 18, Ui.MUTED, false);

        super.render(ctx, mx, my, delta);
    }

    private void move(int dx, int dy) {
        if (bw[selected] <= 0) return;
        int nx = Math.max(0, Math.min(Math.max(0, this.width - bw[selected]), bx[selected] + dx));
        int ny = Math.max(0, Math.min(Math.max(0, this.height - bh[selected]), by[selected] + dy));
        setPos(selected, nx, ny);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        int px = 10, py = 10;
        int ph = Math.min(this.height - 20, 170);
        if (mx >= px && mx < px + PANEL_W && my >= py && my < py + ph) {
            int ly = py + 22;
            for (int i = 0; i < N; i++) {
                int ry = ly + i * 14;
                if (my >= ry && my < ry + 13) {
                    if (mx >= px + PANEL_W - 38) c().item(i).enabled = !c().item(i).enabled;
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
            if (bw[i] > 0 && mx >= bx[i] - 4 && mx < bx[i] + bw[i] + 4 && my >= by[i] - 4 && my < by[i] + bh[i] + 4) {
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
        Overlay.editorOpen = false;
        c().save();
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }
}
