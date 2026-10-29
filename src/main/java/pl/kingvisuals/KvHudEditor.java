package pl.kingvisuals;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Edytuj HUD: przeciągnij napis KingVisuals / FPS / ping w dowolne miejsce ekranu. */
public class KvHudEditor extends Screen {
    private final Screen parent;
    private boolean dragging;
    private int dragOffX, dragOffY;
    private int bx, by, bw, bh;

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
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        TextRenderer tr = this.textRenderer;
        ctx.fill(0, 0, this.width, this.height, 0x77080618);

        int[] sz = Overlay.size();
        if (sz != null) {
            bw = sz[0];
            bh = sz[1];
            bx = this.width - c().overlay.offX - bw;
            by = c().overlay.offY;
            Overlay.draw(ctx, bx, by);
            boolean hover = mx >= bx - 3 && mx < bx + bw + 3 && my >= by - 3 && my < by + bh + 3;
            Ui.roundOutline(ctx, bx - 4, by - 4, bw + 8, bh + 8, 4, hover || dragging ? 0xFFB07CFF : 0x88B07CFF);
        } else {
            bw = 0;
            String t = "Włącz Watermark, FPS lub Ping, żeby coś zobaczyć.";
            ctx.drawText(tr, t, (this.width - tr.getWidth(t)) / 2, this.height / 2, Ui.TEXT, false);
        }

        String title = "Edytuj HUD";
        ctx.drawText(tr, title, (this.width - tr.getWidth(title)) / 2, this.height / 2 - 24, Ui.LABEL, false);
        String hint = "Przeciągnij napis, żeby go przesunąć.  Strzałki: 1 px (Shift: 8 px).  Esc: wróć.";
        ctx.drawText(tr, hint, (this.width - tr.getWidth(hint)) / 2, this.height - 20, Ui.MUTED, false);

        super.render(ctx, mx, my, delta);
    }

    private void setBox(int nbx, int nby) {
        nbx = Math.max(0, Math.min(Math.max(0, this.width - bw), nbx));
        nby = Math.max(0, Math.min(Math.max(0, this.height - bh), nby));
        c().overlay.offX = this.width - (nbx + bw);
        c().overlay.offY = nby;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && bw > 0 && mx >= bx - 4 && mx < bx + bw + 4 && my >= by - 4 && my < by + bh + 4) {
            dragging = true;
            dragOffX = (int) mx - bx;
            dragOffY = (int) my - by;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0 && dragging && bw > 0) {
            setBox((int) Math.round(mx) - dragOffX, (int) Math.round(my) - dragOffY);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bw > 0) {
            int step = (modifiers & 1) != 0 ? 8 : 1; // Shift
            if (keyCode == 262) {
                setBox(bx + step, by);
                return true;
            }
            if (keyCode == 263) {
                setBox(bx - step, by);
                return true;
            }
            if (keyCode == 264) {
                setBox(bx, by + step);
                return true;
            }
            if (keyCode == 265) {
                setBox(bx, by - step);
                return true;
            }
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
