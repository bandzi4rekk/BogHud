package pl.boghud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Ekran ustawień w stylu "TWOJE MODUŁY": ciemny panel z kartami modułów (tag, nazwa, opis, przełącznik).
 * Kliknięcie karty otwiera jej ustawienia z podglądem na żywo, strzałka "< WRÓĆ" wraca do listy.
 */
public class HudConfigScreen extends Screen {
    private static final int CARD_H = 58;
    private static final int GAP = 6;
    private static final int PREVIEW_H = 86;
    private static final int RED = 0xFFE05555;
    private static final int GREEN = 0xFF55E077;
    private static final int BLUE = 0xFF5599E0;
    private static final int[] PRESETS = {0xFF4D6D, 0x5CE1E6, 0xB388FF, 0x66E08A, 0xFFB45C, 0xFFFFFF};

    /** Moduły: 0 = Ogólne, 1..ELEMENTS = elementy HUD, potem Target HUD, Target Ring i Motyw GUI. */
    private static final int M_TARGET = HudConfig.ELEMENTS + 1;
    private static final int M_RING = HudConfig.ELEMENTS + 2;
    private static final int M_GUI = HudConfig.ELEMENTS + 3;
    private static final int MODULES = HudConfig.ELEMENTS + 4;
    private static final String[] TAGS = {"HUD", "NICK", "TEXT", "FPS", "PING", "XYZ", "ARMOR", "CPS", "KEYS",
            "TARGET", "RING", "GUI"};
    private static final String[] NAMES = {"Ogólne", "Nick", "Tekst", "FPS", "Ping", "Koordynaty", "Pancerz",
            "Licznik CPS", "Klawisze", "Target HUD", "Target Ring", "Motyw GUI"};
    private static final String[] DESCS = {"Pozycja HUD i reset", "Twój nick z piorunem", "Własny napis",
            "Klatki na sekundę", "Opóźnienie do serwera", "Pozycja X, Y, Z", "Ikony i wytrzymałość",
            "Kliknięcia LPM i PPM", "WASD, spacja i myszka", "Info o celu walki",
            "Pierścień wokół celu", "Kolory i rozmiar okna"};

    private final Screen parent;
    private TextFieldWidget field;
    private FieldRow fieldRow = new FieldRow();
    private final List<Row> rows = new ArrayList<>();
    private int[] rowX = new int[0], rowY = new int[0], rowW = new int[0];
    private int rowLw;

    private int selected = -1; // -1 = lista modułów
    private Row activeRow;
    private int activeX, activeW;

    private int px, py, pw, ph, cols, cardW;

    public HudConfigScreen(Screen parent) {
        super(Text.literal("Bog HUD"));
        this.parent = parent;
    }

    private HudConfig c() {
        return HudMod.CONFIG;
    }

    private HudConfig.Element el(int i) {
        return HudMod.CONFIG.elements[i];
    }

    // ------------------------------------------------------------------ init i budowa ustawień modułu

    @Override
    protected void init() {
        field = new TextFieldWidget(this.textRenderer, 0, 0, 100, 16, Text.literal("Napis"));
        field.setMaxLength(64);
        field.setText(c().customText == null ? "" : c().customText);
        field.setChangedListener(s -> c().customText = s);
        field.visible = false;
        addDrawableChild(field);

        activeRow = null;
        if (selected >= 0) openModule(selected);
        else rows.clear();
    }

    private void openModule(int m) {
        selected = m;
        rows.clear();
        fieldRow = new FieldRow();
        activeRow = null;
        if (m == 0) buildGeneral();
        else if (m == M_GUI) buildGui();
        else if (m == M_TARGET) buildTarget();
        else if (m == M_RING) buildRing();
        else buildElement(m - 1);
    }

    private void buildGeneral() {
        final int mw = Math.max(1, this.width);
        final int mh = Math.max(1, this.height);
        rows.add(new ToggleRow("HUD włączony", () -> c().enabled, v -> c().enabled = v));
        rows.add(new SliderRow("HUD X", 0, mw, () -> c().stackX, v -> c().stackX = v, 0));
        rows.add(new SliderRow("HUD Y", 0, mh, () -> c().stackY, v -> c().stackY = v, 0));
        rows.add(new TextRow("Elementy HUD układają się"));
        rows.add(new TextRow("jeden pod drugim."));
        rows.add(new ButtonRow("Reset wszystkiego", () -> {
            HudMod.CONFIG = new HudConfig();
            HudMod.CONFIG.fixup();
            HudMod.CONFIG.save();
            clearAndInit();
        }));
    }

    private void buildElement(int ei) {
        if (ei == 1) { // element "Tekst" ma pole na własny napis
            rows.add(new HeadingRow("Napis", null));
            rows.add(fieldRow);
        }
        rows.add(new ToggleRow("Włączony", () -> el(ei).enabled, v -> el(ei).enabled = v));
        rows.add(new SliderRow("Skala %", 50, 200, () -> el(ei).scalePercent, v -> el(ei).scalePercent = v, 0));
        rows.add(new HeadingRow("Kolor tekstu", () -> el(ei).textColor));
        addChannels(() -> el(ei).textColor, v -> el(ei).textColor = v);
        rows.add(new HeadingRow("Kolor akcentu", () -> el(ei).accentColor));
        addChannels(() -> el(ei).accentColor, v -> el(ei).accentColor = v);
        rows.add(new ToggleRow("Tło z piorunami", () -> el(ei).background, v -> el(ei).background = v));
        rows.add(new SliderRow("Krycie tła", 0, 255, () -> el(ei).backgroundAlpha, v -> el(ei).backgroundAlpha = v, 0));
        rows.add(new ToggleRow("Cień tekstu", () -> el(ei).shadow, v -> el(ei).shadow = v));
    }

    private void buildTarget() {
        rows.add(new ToggleRow("Włączony", () -> c().target.enabled, v -> c().target.enabled = v));
        rows.add(new SliderRow("Skala %", 50, 200, () -> c().target.scalePercent, v -> c().target.scalePercent = v, 0));
        rows.add(new SliderRow("Offset X", -300, 300, () -> c().targetX, v -> c().targetX = v, 0));
        rows.add(new SliderRow("Offset Y", -200, 200, () -> c().targetY, v -> c().targetY = v, 0));
        rows.add(new HeadingRow("Kolor tekstu", () -> c().target.textColor));
        addChannels(() -> c().target.textColor, v -> c().target.textColor = v);
        rows.add(new HeadingRow("Kolor akcentu", () -> c().target.accentColor));
        addChannels(() -> c().target.accentColor, v -> c().target.accentColor = v);
        rows.add(new ToggleRow("Tło z piorunami", () -> c().target.background, v -> c().target.background = v));
        rows.add(new SliderRow("Krycie tła", 0, 255, () -> c().target.backgroundAlpha, v -> c().target.backgroundAlpha = v, 0));
        rows.add(new ToggleRow("Cień tekstu", () -> c().target.shadow, v -> c().target.shadow = v));
    }

    private void buildRing() {
        rows.add(new ToggleRow("Włączony", () -> c().ring.enabled, v -> c().ring.enabled = v));
        rows.add(new SliderRow("Promień %", 50, 200, () -> c().ring.radiusPercent, v -> c().ring.radiusPercent = v, 0));
        rows.add(new ToggleRow("Ruch góra-dół", () -> c().ring.animate, v -> c().ring.animate = v));
        rows.add(new SliderRow("Krycie", 20, 255, () -> c().ring.alpha, v -> c().ring.alpha = v, 0));
        rows.add(new HeadingRow("Kolor pierścienia", () -> c().ring.color));
        addChannels(() -> c().ring.color, v -> c().ring.color = v);
        rows.add(new TextRow("Pierścień pojawia się wokół"));
        rows.add(new TextRow("celu, na którego patrzysz"));
        rows.add(new TextRow("albo którego ostatnio uderzyłeś."));
    }

    private void buildGui() {
        rows.add(new HeadingRow("Rozmiar GUI (%)", null));
        rows.add(new ChoiceRow(new int[]{70, 85, 100, 125, 150}, () -> c().guiScale, v -> c().guiScale = v));
        rows.add(new HeadingRow("Gotowe motywy", null));
        rows.add(new ThemeRow(i -> {
            c().guiAccent = ThemeRow.T[i][0];
            c().guiBackground = ThemeRow.T[i][1];
        }));
        rows.add(new HeadingRow("Akcent GUI", () -> c().guiAccent));
        rows.add(new PresetRow(PRESETS, v -> c().guiAccent = v));
        addChannels(() -> c().guiAccent, v -> c().guiAccent = v);
        rows.add(new HeadingRow("Tło GUI", () -> c().guiBackground));
        addChannels(() -> c().guiBackground, v -> c().guiBackground = v);
        rows.add(new SliderRow("Krycie", 80, 255, () -> c().guiAlpha, v -> c().guiAlpha = v, 0));
    }

    private void addChannels(IntSupplier get, IntConsumer set) {
        rows.add(new SliderRow("R", 0, 255, () -> (get.getAsInt() >> 16) & 255,
                v -> set.accept((get.getAsInt() & 0x00FFFF) | (v << 16)), RED));
        rows.add(new SliderRow("G", 0, 255, () -> (get.getAsInt() >> 8) & 255,
                v -> set.accept((get.getAsInt() & 0xFF00FF) | (v << 8)), GREEN));
        rows.add(new SliderRow("B", 0, 255, () -> get.getAsInt() & 255,
                v -> set.accept((get.getAsInt() & 0xFFFF00) | v), BLUE));
    }

    // ------------------------------------------------------------------ moduły (karty)

    private boolean hasToggle(int m) {
        return m != M_GUI;
    }

    private boolean moduleOn(int m) {
        if (m == 0) return c().enabled;
        if (m == M_TARGET) return c().target.enabled;
        if (m == M_RING) return c().ring.enabled;
        return el(m - 1).enabled;
    }

    private void toggleModule(int m) {
        if (m == 0) c().enabled = !c().enabled;
        else if (m == M_TARGET) c().target.enabled = !c().target.enabled;
        else if (m == M_RING) c().ring.enabled = !c().ring.enabled;
        else el(m - 1).enabled = !el(m - 1).enabled;
    }

    // ------------------------------------------------------------------ układ

    private double s() {
        return Math.max(0.5, c().guiScale / 100.0);
    }

    private int vw() {
        return (int) Math.floor(this.width / s());
    }

    private int vh() {
        return (int) Math.floor(this.height / s());
    }

    private void layoutPanel() {
        int top = (int) Math.ceil(46 / s());
        py = top;
        ph = Math.max(80, vh() - top - 6);
        pw = Math.min(vw() - 16, 460);
        px = (vw() - pw) / 2;

        int avail = ph - 30 - 8;
        cols = 3;
        while (cols < 5 && ((MODULES + cols - 1) / cols) * (CARD_H + GAP) - GAP > avail) cols++;
        cardW = (pw - 28 - (cols - 1) * GAP) / cols;
    }

    private int cardX(int i) {
        return px + 14 + (i % cols) * (cardW + GAP);
    }

    private int cardY(int i) {
        return py + 30 + (i / cols) * (CARD_H + GAP);
    }

    private boolean inPreviewModule() {
        return (selected >= 1 && selected <= HudConfig.ELEMENTS) || selected == M_TARGET;
    }

    /** Wiersze ustawień płyną w dwóch kolumnach; druga kolumna zaczyna się pod podglądem. */
    private void layoutDetail(TextRenderer tr) {
        int n = rows.size();
        rowX = new int[n];
        rowY = new int[n];
        rowW = new int[n];
        int lw = 0;
        for (Row r : rows) lw = Math.max(lw, r.labelWidth(tr));
        rowLw = lw;

        int colW = (pw - 28 - 14) / 2;
        int top = py + 34;
        int bottom = py + ph - 8;
        int x = px + 14;
        int y = top;
        boolean second = false;
        for (int i = 0; i < n; i++) {
            Row r = rows.get(i);
            if (!second && y + r.h > bottom) {
                second = true;
                x = px + 14 + colW + 14;
                y = top + (inPreviewModule() ? PREVIEW_H + 6 : 0);
            }
            rowX[i] = x;
            rowY[i] = y;
            rowW[i] = colW;
            y += r.h;
        }
    }

    // ------------------------------------------------------------------ rysowanie

    // Bez rozmycia i przyciemnienia tła
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        drawLogo(ctx);

        layoutPanel();
        float sc = (float) s();
        int vmx = (int) Math.round(mouseX / s());
        int vmy = (int) Math.round(mouseY / s());
        TextRenderer tr = this.textRenderer;

        MatrixStack m = ctx.getMatrices();
        m.push();
        m.scale(sc, sc, 1f);

        ctx.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, Ui.border());
        ctx.fillGradient(px, py, px + pw, py + ph, Ui.panelTop(), Ui.bg());

        boolean showField = false;
        if (selected < 0) {
            renderGrid(ctx, tr, vmx, vmy);
        } else {
            showField = renderDetail(ctx, tr, vmx, vmy);
        }
        field.visible = showField;

        super.render(ctx, vmx, vmy, delta);
        m.pop();
    }

    private void renderGrid(DrawContext ctx, TextRenderer tr, int mx, int my) {
        ctx.drawText(tr, "TWOJE MODUŁY", px + 14, py + 12, Ui.text(), false);
        for (int i = 0; i < MODULES; i++) {
            renderCard(ctx, tr, i, cardX(i), cardY(i), mx, my);
        }
    }

    private void renderCard(DrawContext ctx, TextRenderer tr, int i, int x, int y, int mx, int my) {
        boolean hover = mx >= x && mx < x + cardW && my >= y && my < y + CARD_H;
        Ui.roundRect(ctx, x, y, cardW, CARD_H, hover ? Ui.cardHover() : Ui.cardBorder());
        Ui.roundRect(ctx, x + 1, y + 1, cardW - 2, CARD_H - 2, Ui.cardBg());

        String tag = TAGS[i];
        int tw = tr.getWidth(tag);
        Ui.roundRect(ctx, x + 6, y + 6, tw + 10, 12, Ui.chipBg());
        ctx.drawText(tr, tag, x + 11, y + 8, Ui.accent(), false);
        ctx.drawText(tr, ">", x + cardW - 12, y + 8, Ui.muted(), false);

        ctx.drawText(tr, tr.trimToWidth(NAMES[i], cardW - 12), x + 6, y + 23, Ui.text(), false);
        ctx.drawText(tr, tr.trimToWidth(DESCS[i], cardW - 12), x + 6, y + 34, Ui.muted(), false);

        if (hasToggle(i)) {
            boolean on = moduleOn(i);
            ctx.drawText(tr, on ? "WŁĄCZONE" : "WYŁĄCZONE", x + 6, y + 46, on ? Ui.accent() : Ui.muted(), false);
            int sw = 22, sh = 10;
            int sx = x + cardW - 6 - sw;
            int sy = y + 45;
            ctx.fill(sx, sy, sx + sw, sy + sh, on ? Ui.accent() : Ui.off());
            int kx = on ? sx + sw - sh + 1 : sx + 1;
            ctx.fill(kx, sy + 1, kx + sh - 2, sy + sh - 1, 0xFFFFFFFF);
        }
    }

    /** Rysuje widok ustawień modułu. Zwraca true, gdy pole tekstowe ma być widoczne. */
    private boolean renderDetail(DrawContext ctx, TextRenderer tr, int mx, int my) {
        // nagłówek: < WRÓĆ + tag + nazwa
        String back = "< WRÓĆ";
        int bw = tr.getWidth(back);
        boolean backHover = mx >= px + 14 && mx < px + 14 + bw && my >= py + 8 && my < py + 22;
        ctx.drawText(tr, back, px + 14, py + 12, backHover ? Ui.text() : Ui.accent(), false);

        int nx = px + 14 + bw + 12;
        String tag = TAGS[selected];
        int tw = tr.getWidth(tag);
        Ui.roundRect(ctx, nx, py + 9, tw + 10, 12, Ui.chipBg());
        ctx.drawText(tr, tag, nx + 5, py + 11, Ui.accent(), false);
        ctx.drawText(tr, NAMES[selected], nx + tw + 18, py + 12, Ui.text(), false);

        layoutDetail(tr);

        // podgląd na żywo
        if (inPreviewModule()) {
            int colW = (pw - 28 - 14) / 2;
            int x2 = px + 14 + colW + 14;
            int y2 = py + 34;
            Ui.roundRect(ctx, x2, y2, colW, PREVIEW_H, Ui.cardBorder());
            Ui.roundRect(ctx, x2 + 1, y2 + 1, colW - 2, PREVIEW_H - 2, Ui.cardBg());
            ctx.drawText(tr, "PODGLĄD", x2 + 8, y2 + 5, Ui.muted(), false);
            boolean drawn = selected == M_TARGET
                    ? TargetHud.drawPreview(ctx, x2 + 8, y2 + 17, colW - 16, PREVIEW_H - 23)
                    : HudRenderer.drawPreview(ctx, selected - 1, x2 + 8, y2 + 17, colW - 16, PREVIEW_H - 23);
            if (!drawn) {
                ctx.drawText(tr, "Brak podglądu (np. brak", x2 + 8, y2 + 24, Ui.muted(), false);
                ctx.drawText(tr, "założonego pancerza).", x2 + 8, y2 + 35, Ui.muted(), false);
            }
        }

        fieldRow.shown = false;
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).render(ctx, tr, rowX[i], rowY[i], rowW[i], rowLw, mx, my);
        }
        if (fieldRow.shown) {
            field.setX(fieldRow.x);
            field.setY(fieldRow.y + 3);
            field.setWidth(fieldRow.w);
            return true;
        }
        return false;
    }

    /** Napis BOG CODE w stylu graffiti: gruby, równy, z obrysem, cieniem i zaciekami farby. */
    private void drawLogo(DrawContext ctx) {
        TextRenderer tr = this.textRenderer;
        Text bog = Text.literal("BOG").formatted(Formatting.BOLD);
        Text code = Text.literal("CODE").formatted(Formatting.BOLD);
        int wBog = tr.getWidth(bog);
        int wCode = tr.getWidth(code);
        int gap = 5;
        int total = wBog + gap + wCode;
        int xCode = wBog + gap;
        int yellow = 0xFFFFE135;
        int red = 0xFFFF2E2E;

        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(this.width / 2f, 6f, 0f);
        m.scale(2.5f, 2.5f, 1f);
        m.translate(-total / 2f, 0f, 0f);

        drawPair(ctx, tr, bog, code, xCode, 1, 2, 0x99000000, 0x99000000);
        int[][] o = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {1, -1}, {-1, 1}, {1, 1}};
        for (int[] d : o) {
            drawPair(ctx, tr, bog, code, xCode, d[0], d[1], 0xFF000000, 0xFF000000);
        }

        int[][] dripsBog = {{2, 3}, {9, 4}, {wBog - 4, 2}};
        for (int[] d : dripsBog) drip(ctx, d[0], d[1], yellow);
        int[][] dripsCode = {{1, 4}, {8, 2}, {15, 4}, {wCode - 4, 3}};
        for (int[] d : dripsCode) drip(ctx, xCode + d[0], d[1], red);

        drawPair(ctx, tr, bog, code, xCode, 0, 0, yellow, red);

        m.pop();
    }

    private void drawPair(DrawContext ctx, TextRenderer tr, Text a, Text b, int xB, int dx, int dy, int ca, int cb) {
        ctx.drawText(tr, a, dx, dy, ca, false);
        ctx.drawText(tr, b, xB + dx, dy, cb, false);
    }

    private void drip(DrawContext ctx, int x, int len, int color) {
        int top = 8;
        ctx.fill(x, top, x + 1, top + len, color);
        ctx.fill(x - 1, top + len, x + 2, top + len + 1, color);
        ctx.fill(x, top + len + 1, x + 1, top + len + 2, color);
    }

    // ------------------------------------------------------------------ mysz i klawiatura

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        double vx = mx / s(), vy = my / s();
        if (selected >= 0 && field.visible && field.isMouseOver(vx, vy)) {
            return super.mouseClicked(vx, vy, button);
        }
        field.setFocused(false);

        if (button != 0) return super.mouseClicked(vx, vy, button);

        if (selected < 0) {
            for (int i = 0; i < MODULES; i++) {
                int x = cardX(i), y = cardY(i);
                if (vx >= x && vx < x + cardW && vy >= y && vy < y + CARD_H) {
                    boolean onToggle = hasToggle(i) && vx >= x + cardW - 34 && vy >= y + CARD_H - 18;
                    if (onToggle) toggleModule(i);
                    else openModule(i);
                    return true;
                }
            }
            return true;
        }

        // widok ustawień
        TextRenderer tr = this.textRenderer;
        int bw = tr.getWidth("< WRÓĆ");
        if (vx >= px + 14 && vx < px + 14 + bw && vy >= py + 8 && vy < py + 22) {
            selected = -1;
            rows.clear();
            activeRow = null;
            return true;
        }
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            if (vx >= rowX[i] && vx < rowX[i] + rowW[i] && vy >= rowY[i] && vy < rowY[i] + r.h) {
                activeRow = r;
                activeX = rowX[i];
                activeW = rowW[i];
                r.click(vx, vy, rowX[i], rowW[i], rowLw);
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        double vx = mx / s(), vy = my / s();
        if (button == 0 && activeRow != null) {
            activeRow.drag(vx, vy, activeX, activeW, rowLw);
            return true;
        }
        return super.mouseDragged(vx, vy, button, dx / s(), dy / s());
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        activeRow = null;
        return super.mouseReleased(mx / s(), my / s(), button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC: z ustawień modułu wraca do listy, z listy zamyka ekran
        if (keyCode == 256 && selected >= 0 && !field.isFocused()) {
            selected = -1;
            rows.clear();
            activeRow = null;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        c().save();
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }

    // ------------------------------------------------------------------ wiersze

    private abstract static class Row {
        final int h;

        Row(int h) {
            this.h = h;
        }

        int labelWidth(TextRenderer tr) {
            return 0;
        }

        abstract void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int lw, int mx, int my);

        void click(double mx, double my, int x, int w, int lw) {
        }

        void drag(double mx, double my, int x, int w, int lw) {
        }
    }

    private static class ToggleRow extends Row {
        private final String label;
        private final BooleanSupplier get;
        private final Consumer<Boolean> set;

        ToggleRow(String label, BooleanSupplier get, Consumer<Boolean> set) {
            super(16);
            this.label = label;
            this.get = get;
            this.set = set;
        }

        @Override
        void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int lw, int mx, int my) {
            if (mx >= x && mx < x + w && my >= y && my < y + h) {
                ctx.fill(x, y, x + w, y + h, Ui.hover());
            }
            ctx.drawText(tr, label, x + 2, y + 4, Ui.text(), false);
            boolean on = get.getAsBoolean();
            int sw = 22, sh = 10;
            int sx = x + w - sw - 2;
            int sy = y + 3;
            ctx.fill(sx, sy, sx + sw, sy + sh, on ? Ui.accent() : Ui.off());
            int kx = on ? sx + sw - sh + 1 : sx + 1;
            ctx.fill(kx, sy + 1, kx + sh - 2, sy + sh - 1, 0xFFFFFFFF);
        }

        @Override
        void click(double mx, double my, int x, int w, int lw) {
            set.accept(!get.getAsBoolean());
        }
    }

    private static class SliderRow extends Row {
        private final String label;
        private final int min, max, tint;
        private final IntSupplier get;
        private final IntConsumer set;

        SliderRow(String label, int min, int max, IntSupplier get, IntConsumer set, int tint) {
            super(14);
            this.label = label;
            this.min = min;
            this.max = max;
            this.get = get;
            this.set = set;
            this.tint = tint;
        }

        @Override
        int labelWidth(TextRenderer tr) {
            return tr.getWidth(label) + 6;
        }

        private void setFromMouse(double mx, int x, int w, int lw) {
            int tw = w - lw - 26;
            double t = (mx - (x + lw)) / tw;
            t = Math.max(0, Math.min(1, t));
            set.accept((int) Math.round(min + t * (max - min)));
        }

        @Override
        void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int lw, int mx, int my) {
            int cy = y + h / 2;
            ctx.drawText(tr, label, x + 2, cy - 4, Ui.text(), false);
            int tx = x + lw;
            int tw = w - lw - 26;
            int v = Math.max(min, Math.min(max, get.getAsInt()));
            double t = (v - min) / (double) (max - min);
            int kx = tx + (int) Math.round(t * tw);
            ctx.fill(tx, cy - 1, tx + tw, cy + 2, Ui.off());
            ctx.fill(tx, cy - 1, kx, cy + 2, tint == 0 ? Ui.accent() : tint);
            ctx.fill(kx - 2, cy - 5, kx + 2, cy + 6, 0xFFFFFFFF);
            String s = String.valueOf(v);
            ctx.drawText(tr, s, x + w - tr.getWidth(s) - 2, cy - 4, Ui.muted(), false);
        }

        @Override
        void click(double mx, double my, int x, int w, int lw) {
            if (mx >= x + lw - 6) setFromMouse(mx, x, w, lw);
        }

        @Override
        void drag(double mx, double my, int x, int w, int lw) {
            setFromMouse(mx, x, w, lw);
        }
    }

    private static class HeadingRow extends Row {
        private final String text;
        private final IntSupplier swatch;

        HeadingRow(String text, IntSupplier swatch) {
            super(14);
            this.text = text;
            this.swatch = swatch;
        }

        @Override
        void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int lw, int mx, int my) {
            ctx.drawText(tr, text, x + 2, y + 3, Ui.accent(), false);
            if (swatch != null) {
                int sx = x + w - 22;
                ctx.fill(sx - 1, y + 1, sx + 19, y + 13, Ui.border());
                ctx.fill(sx, y + 2, sx + 18, y + 12, 0xFF000000 | swatch.getAsInt());
            }
        }
    }

    private static class TextRow extends Row {
        private final String text;

        TextRow(String text) {
            super(11);
            this.text = text;
        }

        @Override
        void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int lw, int mx, int my) {
            ctx.drawText(tr, text, x + 2, y + 2, Ui.muted(), false);
        }
    }

    private static class ButtonRow extends Row {
        private final String label;
        private final Runnable action;

        ButtonRow(String label, Runnable action) {
            super(20);
            this.label = label;
            this.action = action;
        }

        @Override
        void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int lw, int mx, int my) {
            boolean hover = mx >= x && mx < x + w && my >= y + 2 && my < y + h;
            ctx.fill(x, y + 2, x + w, y + h - 1, hover ? Ui.border() : Ui.off());
            ctx.drawText(tr, label, x + (w - tr.getWidth(label)) / 2, y + 7, Ui.text(), false);
        }

        @Override
        void click(double mx, double my, int x, int w, int lw) {
            action.run();
        }
    }

    private static class PresetRow extends Row {
        private final int[] colors;
        private final IntConsumer set;

        PresetRow(int[] colors, IntConsumer set) {
            super(18);
            this.colors = colors;
            this.set = set;
        }

        @Override
        void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int lw, int mx, int my) {
            for (int i = 0; i < colors.length; i++) {
                int bx = x + 2 + i * 16;
                ctx.fill(bx, y + 3, bx + 12, y + 15, 0xFF000000 | colors[i]);
                if (mx >= bx && mx < bx + 12 && my >= y + 3 && my < y + 15) {
                    Ui.outline(ctx, bx - 1, y + 2, 14, 14, Ui.text());
                }
            }
        }

        @Override
        void click(double mx, double my, int x, int w, int lw) {
            double rel = mx - (x + 2);
            if (rel < 0) return;
            int i = (int) (rel / 16);
            if (i < colors.length && rel - i * 16 < 12) set.accept(colors[i]);
        }
    }

    /** Gotowe motywy: tło + akcent jednym kliknięciem. */
    private static class ThemeRow extends Row {
        /** {akcent, tło} */
        static final int[][] T = {
                {0xFF4D6D, 0x07182E}, {0x5CE1E6, 0x141A22}, {0xB388FF, 0x120E24},
                {0x66E08A, 0x0B1A14}, {0xFFE135, 0x14120A}
        };
        private final IntConsumer pick;

        ThemeRow(IntConsumer pick) {
            super(20);
            this.pick = pick;
        }

        @Override
        void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int lw, int mx, int my) {
            for (int i = 0; i < T.length; i++) {
                int bx = x + 2 + i * 24;
                ctx.fill(bx, y + 3, bx + 20, y + 17, 0xFF000000 | T[i][1]);
                ctx.fill(bx + 4, y + 8, bx + 16, y + 12, 0xFF000000 | T[i][0]);
                Ui.outline(ctx, bx, y + 3, 20, 14, 0xFF000000 | T[i][0]);
                if (mx >= bx && mx < bx + 20 && my >= y + 3 && my < y + 17) {
                    Ui.outline(ctx, bx - 1, y + 2, 22, 16, Ui.text());
                }
            }
        }

        @Override
        void click(double mx, double my, int x, int w, int lw) {
            double rel = mx - (x + 2);
            if (rel < 0) return;
            int i = (int) (rel / 24);
            if (i < T.length && rel - i * 24 < 20) pick.accept(i);
        }
    }

    /** Rząd przycisków wyboru jednej wartości (np. rozmiar GUI). */
    private static class ChoiceRow extends Row {
        private final int[] values;
        private final IntSupplier get;
        private final IntConsumer set;

        ChoiceRow(int[] values, IntSupplier get, IntConsumer set) {
            super(18);
            this.values = values;
            this.get = get;
            this.set = set;
        }

        @Override
        void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int lw, int mx, int my) {
            int cw = w / values.length;
            for (int i = 0; i < values.length; i++) {
                int bx = x + i * cw;
                boolean sel = get.getAsInt() == values[i];
                boolean hover = mx >= bx && mx < bx + cw && my >= y + 2 && my < y + h - 2;
                ctx.fill(bx + 1, y + 2, bx + cw - 1, y + h - 2, sel ? Ui.accent() : (hover ? Ui.border() : Ui.off()));
                String t = String.valueOf(values[i]);
                ctx.drawText(tr, t, bx + (cw - tr.getWidth(t)) / 2, y + 5, sel ? Ui.onAccent() : Ui.text(), false);
            }
        }

        @Override
        void click(double mx, double my, int x, int w, int lw) {
            int cw = w / values.length;
            int i = (int) ((mx - x) / cw);
            if (i >= 0 && i < values.length) set.accept(values[i]);
        }
    }

    /** Miejsce na pole tekstowe (samo pole jest zwykłym widgetem ustawianym w render). */
    private static class FieldRow extends Row {
        boolean shown;
        int x, y, w;

        FieldRow() {
            super(22);
        }

        @Override
        void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int lw, int mx, int my) {
            this.shown = true;
            this.x = x;
            this.y = y;
            this.w = w;
        }
    }
}
