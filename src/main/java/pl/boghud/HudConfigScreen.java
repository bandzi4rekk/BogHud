package pl.boghud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Ekran ustawień: ciemny panel z zakładkami, przełącznikami, suwakami i przeciąganiem HUD-a myszką. */
public class HudConfigScreen extends Screen {
    private static final int PANEL_W = 340;
    private static final int PANEL_H = 180;
    private static final int RED = 0xFFE05555;
    private static final int GREEN = 0xFF55E077;
    private static final int BLUE = 0xFF5599E0;

    private final Screen parent;
    private int tab = 0;
    private int px, py;
    private boolean draggingHud;
    private double fx, fy;

    public HudConfigScreen(Screen parent) {
        super(Text.literal("Bog HUD"));
        this.parent = parent;
    }

    private HudConfig c() {
        return HudMod.CONFIG;
    }

    @Override
    protected void init() {
        px = (this.width - PANEL_W) / 2;
        py = Math.max(4, (this.height - PANEL_H) / 2);

        String[] names = {"Elementy", "Kolory", "Układ"};
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            addDrawableChild(new Ui.Button(px + 10 + i * 74, py + 25, 70, 15, names[i],
                    () -> {
                        tab = idx;
                        clearAndInit();
                    },
                    () -> tab == idx));
        }
        addDrawableChild(new Ui.Button(px + PANEL_W - 70, py + 25, 60, 15, "Reset",
                () -> {
                    HudMod.CONFIG = new HudConfig();
                    HudMod.CONFIG.save();
                    clearAndInit();
                },
                () -> false));
        addDrawableChild(new Ui.Button(px + PANEL_W - 20, py + 4, 14, 14, "X", this::close, () -> false));

        int cy = py + 52;
        if (tab == 0) initElements(cy);
        else if (tab == 1) initColors(cy);
        else initLayout(cy);
    }

    private void initElements(int cy) {
        int colW = 155;
        int x1 = px + 10;
        int x2 = px + 10 + colW + 10;
        addDrawableChild(new Ui.Toggle(x1, cy, colW, 18, "HUD", () -> c().enabled, v -> c().enabled = v));
        addDrawableChild(new Ui.Toggle(x1, cy + 20, colW, 18, "FPS", () -> c().showFps, v -> c().showFps = v));
        addDrawableChild(new Ui.Toggle(x1, cy + 40, colW, 18, "Ping", () -> c().showPing, v -> c().showPing = v));
        addDrawableChild(new Ui.Toggle(x1, cy + 60, colW, 18, "Koordynaty", () -> c().showCoords, v -> c().showCoords = v));
        addDrawableChild(new Ui.Toggle(x2, cy, colW, 18, "Pancerz", () -> c().showArmor, v -> c().showArmor = v));
        addDrawableChild(new Ui.Toggle(x2, cy + 20, colW, 18, "Własny tekst", () -> c().showText, v -> c().showText = v));
        addDrawableChild(new Ui.Toggle(x2, cy + 40, colW, 18, "Cień tekstu", () -> c().shadow, v -> c().shadow = v));
        addDrawableChild(new Ui.Toggle(x2, cy + 60, colW, 18, "Tło", () -> c().background, v -> c().background = v));

        TextFieldWidget field = new TextFieldWidget(this.textRenderer, px + 10, py + 148, PANEL_W - 20, 18,
                Text.literal("Własny tekst"));
        field.setMaxLength(64);
        field.setText(c().customText == null ? "" : c().customText);
        field.setChangedListener(s -> c().customText = s);
        addDrawableChild(field);
    }

    private void initColors(int cy) {
        int colW = 155;
        int x1 = px + 10;
        int x2 = px + 10 + colW + 10;
        channelSliders(x1, cy + 14, colW, () -> c().textColor, v -> c().textColor = v);
        channelSliders(x2, cy + 14, colW, () -> c().accentColor, v -> c().accentColor = v);
        addDrawableChild(new Ui.Slider(x1, cy + 76, PANEL_W - 20, 16, "Tło (alfa)", 0, 255,
                () -> c().backgroundAlpha, v -> c().backgroundAlpha = v, 0xFF9AA5B1));
    }

    private void channelSliders(int x, int y, int w, IntSupplier getRgb, IntConsumer setRgb) {
        addDrawableChild(new Ui.Slider(x, y, w, 16, "Czerwony", 0, 255,
                () -> (getRgb.getAsInt() >> 16) & 255,
                v -> setRgb.accept((getRgb.getAsInt() & 0x00FFFF) | (v << 16)), RED));
        addDrawableChild(new Ui.Slider(x, y + 18, w, 16, "Zielony", 0, 255,
                () -> (getRgb.getAsInt() >> 8) & 255,
                v -> setRgb.accept((getRgb.getAsInt() & 0xFF00FF) | (v << 8)), GREEN));
        addDrawableChild(new Ui.Slider(x, y + 36, w, 16, "Niebieski", 0, 255,
                () -> getRgb.getAsInt() & 255,
                v -> setRgb.accept((getRgb.getAsInt() & 0xFFFF00) | v), BLUE));
    }

    private void initLayout(int cy) {
        int x = px + 10;
        int w = PANEL_W - 20;
        addDrawableChild(new Ui.Slider(x, cy, w, 16, "Skala %", 50, 200,
                () -> c().scalePercent, v -> c().scalePercent = v, Ui.ACCENT));
        addDrawableChild(new Ui.Slider(x, cy + 20, w, 16, "Pozycja X", 0, Math.max(1, this.width),
                () -> c().x, v -> c().x = v, Ui.ACCENT));
        addDrawableChild(new Ui.Slider(x, cy + 40, w, 16, "Pozycja Y", 0, Math.max(1, this.height),
                () -> c().y, v -> c().y = v, Ui.ACCENT));
    }

    // Bez rozmycia i przyciemnienia, żeby HUD był widoczny na żywo
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // panel
        ctx.fill(px - 1, py - 1, px + PANEL_W + 1, py + PANEL_H + 1, Ui.BORDER);
        ctx.fill(px, py, px + PANEL_W, py + PANEL_H, Ui.BG);
        ctx.fill(px, py, px + PANEL_W, py + 22, Ui.TITLE_BG);
        ctx.drawText(this.textRenderer, "Bog HUD", px + 10, py + 7, Ui.ACCENT, false);
        ctx.fill(px + 8, py + 42, px + PANEL_W - 8, py + 43, Ui.LINE);

        int cy = py + 52;
        int colW = 155;
        int x1 = px + 10;
        int x2 = px + 10 + colW + 10;

        if (tab == 0) {
            ctx.drawText(this.textRenderer, "Własny tekst", px + 12, py + 136, Ui.MUTED, false);
        } else if (tab == 1) {
            drawHeading(ctx, "Kolor tekstu", x1, cy, colW, c().textColor);
            drawHeading(ctx, "Kolor akcentu", x2, cy, colW, c().accentColor);
        } else {
            ctx.drawText(this.textRenderer, "Przeciągnij HUD myszką, żeby go przesunąć.",
                    px + 12, cy + 66, Ui.MUTED, false);
        }

        super.render(ctx, mouseX, mouseY, delta);

        // ramka wokół HUD-a (pokazuje, że można go złapać)
        if (HudRenderer.boundsW > 0) {
            Ui.outline(ctx, HudRenderer.boundsX - 1, HudRenderer.boundsY - 1,
                    HudRenderer.boundsW + 2, HudRenderer.boundsH + 2, 0x885CE1E6);
        }
    }

    private void drawHeading(DrawContext ctx, String text, int x, int y, int w, int rgb) {
        ctx.drawText(this.textRenderer, text, x + 4, y + 1, Ui.TEXT, false);
        int sx = x + w - 22;
        ctx.fill(sx - 1, y - 1, sx + 19, y + 11, Ui.BORDER);
        ctx.fill(sx, y, sx + 18, y + 10, 0xFF000000 | rgb);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        boolean inPanel = mouseX >= px && mouseX <= px + PANEL_W && mouseY >= py && mouseY <= py + PANEL_H;
        if (button == 0 && !inPanel && HudRenderer.hit(mouseX, mouseY)) {
            draggingHud = true;
            fx = c().x;
            fy = c().y;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingHud && button == 0) {
            fx = Math.max(0, Math.min(this.width, fx + deltaX));
            fy = Math.max(0, Math.min(this.height, fy + deltaY));
            c().x = (int) Math.round(fx);
            c().y = (int) Math.round(fy);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingHud = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void removed() {
        c().save();
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }
}
