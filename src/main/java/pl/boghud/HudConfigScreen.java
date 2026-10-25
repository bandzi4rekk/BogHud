package pl.boghud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import pl.boghud.Rows.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Menu moda na cały ekran w stylu "TWOJE MODUŁY": karty modułów (tag, nazwa, opis, przełącznik),
 * strony przewijane kółkiem myszy, ustawienia modułu z podglądem na żywo oraz przycisk Edytora HUD.
 */
public class HudConfigScreen extends Screen {
    private static final int CARD_H = 54;
    private static final int GAP = 6;
    private static final int PREVIEW_H = 86;
    private static final int RED = 0xFFE05555;
    private static final int GREEN = 0xFF55E077;
    private static final int BLUE = 0xFF5599E0;
    private static final int[] PRESETS = {0xFF4D6D, 0x5CE1E6, 0xB388FF, 0x66E08A, 0xFFB45C, 0xFFFFFF};

    /** Moduły: 0 = Ogólne, 1..ELEMENTS = elementy HUD, potem wizualia, opcje Ekran i Motyw GUI. */
    private static final int E = HudConfig.ELEMENTS;
    private static final int M_TARGET = E + 1;
    private static final int M_RING = E + 2;
    private static final int M_JUMP = E + 3;
    private static final int M_HAT = E + 4;
    private static final int M_HIT = E + 5;
    private static final int M_CROSS = E + 6;
    private static final int M_RATIO = E + 7;
    private static final int M_VIEW = E + 8;
    private static final int M_NICKHP = E + 9;
    private static final int M_GUI = E + 10;
    private static final int MODULES = E + 11;
    private static final String[] TAGS = {"HUD", "NICK", "TEXT", "FPS", "PING", "XYZ", "ARMOR", "CPS", "KEYS",
            "TARGET", "RING", "JUMP", "HAT", "HIT", "CROSS", "RATIO", "VIEW", "HP", "GUI"};
    private static final String[] NAMES = {"Ogólne", "Nick", "Tekst", "FPS", "Ping", "Koordynaty", "Pancerz",
            "Licznik CPS", "Klawisze", "Target HUD", "Target Ring", "Jump Circles", "China Hat",
            "Hit Particles", "Ring Crosshair", "Proporcje", "View Model", "HP nad nickiem", "Motyw GUI"};
    private static final String[] DESCS = {"Edytor HUD i reset", "Twój nick z piorunem", "Własny napis",
            "Klatki na sekundę", "Opóźnienie do serwera", "Pozycja X, Y, Z", "Ikony i wytrzymałość",
            "Kliknięcia LPM i PPM", "WASD, spacja i myszka", "Info o celu walki",
            "Pierścień wokół celu", "Kółko przy skoku", "Kapelusz nad głową",
            "Cząsteczki przy trafieniu", "Kółko wokół celownika", "Rozciągnięty ekran",
            "Pozycja przedmiotów", "Zdrowie graczy", "Kolory i rozmiar okna"};

    private final Screen parent;
    private TextFieldWidget field;
    private FieldRow fieldRow = new FieldRow();
    private final List<Row> rows = new ArrayList<>();
    private int[] rowX = new int[0], rowY = new int[0], rowW = new int[0];
    private int rowLw;

    private int selected = -1; // -1 = lista modułów
    private int page = 0;
    private Row activeRow;
    private int activeX, activeW;

    // układ (liczony co klatkę)
    private int cl = 16, ct, cw, gridY, cols, cardW, perPage, pages, ncol, colW;

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

    private void openEditor() {
        if (this.client != null) this.client.setScreen(new HudEditorScreen(this));
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
        else if (m == M_JUMP) buildJump();
        else if (m == M_HAT) buildHat();
        else if (m == M_HIT) buildHit();
        else if (m == M_CROSS) buildCross();
        else if (m == M_RATIO) buildRatio();
        else if (m == M_VIEW) buildView();
        else if (m == M_NICKHP) buildNickHp();
        else buildElement(m - 1);
    }

    private void buildGeneral() {
        rows.add(new ToggleRow("HUD włączony", () -> c().enabled, v -> c().enabled = v));
        rows.add(new ButtonRow("Edytor HUD", this::openEditor));
        rows.add(new ButtonRow("Ułóż elementy pod sobą", () -> c().resetPositions()));
        rows.add(new TextRow("Elementy HUD ustawiasz gdzie"));
        rows.add(new TextRow("chcesz w Edytorze HUD:"));
        rows.add(new TextRow("przeciągasz je myszką po ekranie."));
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
        rows.add(new TextRow("Pozycję ustawiasz w Edytorze HUD."));
    }

    private void buildTarget() {
        rows.add(new ToggleRow("Włączony", () -> c().target.enabled, v -> c().target.enabled = v));
        rows.add(new SliderRow("Skala %", 50, 200, () -> c().target.scalePercent, v -> c().target.scalePercent = v, 0));
        rows.add(new HeadingRow("Kolor tekstu", () -> c().target.textColor));
        addChannels(() -> c().target.textColor, v -> c().target.textColor = v);
        rows.add(new HeadingRow("Kolor akcentu", () -> c().target.accentColor));
        addChannels(() -> c().target.accentColor, v -> c().target.accentColor = v);
        rows.add(new ToggleRow("Tło z piorunami", () -> c().target.background, v -> c().target.background = v));
        rows.add(new SliderRow("Krycie tła", 0, 255, () -> c().target.backgroundAlpha, v -> c().target.backgroundAlpha = v, 0));
        rows.add(new ToggleRow("Cień tekstu", () -> c().target.shadow, v -> c().target.shadow = v));
        rows.add(new TextRow("Pozycję ustawiasz w Edytorze HUD."));
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

    private void buildJump() {
        rows.add(new ToggleRow("Włączone", () -> c().jump.enabled, v -> c().jump.enabled = v));
        rows.add(new SliderRow("Promień %", 50, 200, () -> c().jump.radiusPercent, v -> c().jump.radiusPercent = v, 0));
        rows.add(new SliderRow("Czas (ms)", 300, 1500, () -> c().jump.durationMs, v -> c().jump.durationMs = v, 0));
        rows.add(new SliderRow("Krycie", 20, 255, () -> c().jump.alpha, v -> c().jump.alpha = v, 0));
        rows.add(new HeadingRow("Kolor kółka", () -> c().jump.color));
        addChannels(() -> c().jump.color, v -> c().jump.color = v);
        rows.add(new TextRow("Kółko rozchodzi się spod nóg,"));
        rows.add(new TextRow("gdy podskoczysz."));
    }

    private void buildHat() {
        rows.add(new ToggleRow("Włączony", () -> c().hat.enabled, v -> c().hat.enabled = v));
        rows.add(new ToggleRow("Innym graczom", () -> c().hat.showOthers, v -> c().hat.showOthers = v));
        rows.add(new SliderRow("Rozmiar %", 50, 200, () -> c().hat.sizePercent, v -> c().hat.sizePercent = v, 0));
        rows.add(new SliderRow("Krycie", 20, 255, () -> c().hat.alpha, v -> c().hat.alpha = v, 0));
        rows.add(new HeadingRow("Kolor kapelusza", () -> c().hat.color));
        addChannels(() -> c().hat.color, v -> c().hat.color = v);
        rows.add(new TextRow("Swój kapelusz widzisz w trzeciej"));
        rows.add(new TextRow("osobie (F5)."));
    }

    private void buildHit() {
        rows.add(new ToggleRow("Włączone", () -> c().hit.enabled, v -> c().hit.enabled = v));
        rows.add(new HeadingRow("Rodzaj cząsteczek", null));
        rows.add(new ChoiceRow(new int[]{0, 1, 2, 3}, new String[]{"Crit", "Magia", "Ogień", "Gwiazdy"},
                () -> c().hit.type, v -> c().hit.type = v));
        rows.add(new SliderRow("Ilość", 1, 40, () -> c().hit.count, v -> c().hit.count = v, 0));
    }

    private void buildCross() {
        rows.add(new ToggleRow("Włączony", () -> c().cross.enabled, v -> c().cross.enabled = v));
        rows.add(new SliderRow("Promień", 3, 30, () -> c().cross.radius, v -> c().cross.radius = v, 0));
        rows.add(new SliderRow("Grubość", 1, 4, () -> c().cross.thickness, v -> c().cross.thickness = v, 0));
        rows.add(new ToggleRow("Czerwony na celu", () -> c().cross.reactToTarget, v -> c().cross.reactToTarget = v));
        rows.add(new HeadingRow("Kolor kółka", () -> c().cross.color));
        addChannels(() -> c().cross.color, v -> c().cross.color = v);
    }

    private void buildRatio() {
        rows.add(new ToggleRow("Włączone", () -> c().aspect.enabled, v -> c().aspect.enabled = v));
        rows.add(new HeadingRow("Gotowe proporcje", null));
        rows.add(new ChoiceRow(new int[]{133, 125, 160, 178, 233},
                new String[]{"4:3", "5:4", "16:10", "16:9", "21:9"},
                () -> c().aspect.ratioPercent, v -> c().aspect.ratioPercent = v));
        rows.add(new SliderRow("Własne", 50, 300, () -> c().aspect.ratioPercent, v -> c().aspect.ratioPercent = v, 0));
        rows.add(new TextRow("Wartość 178 to 16:9, 133 to 4:3."));
        rows.add(new TextRow("Mniejsza liczba = bardziej"));
        rows.add(new TextRow("rozciągnięty obraz."));
    }

    private void buildView() {
        rows.add(new ToggleRow("Włączony", () -> c().viewModel.enabled, v -> c().viewModel.enabled = v));
        rows.add(new HeadingRow("Główna ręka", null));
        addHand(true);
        rows.add(new HeadingRow("Druga ręka", null));
        addHand(false);
        rows.add(new ButtonRow("Reset ustawień rąk", () -> {
            boolean en = c().viewModel.enabled;
            c().viewModel = new HudConfig.ViewModel();
            c().viewModel.enabled = en;
            clearAndInit();
        }));
    }

    private HudConfig.HandCfg hand(boolean main) {
        return main ? c().viewModel.main : c().viewModel.off;
    }

    private void addHand(boolean main) {
        rows.add(new SliderRow("X", -100, 100, () -> hand(main).x, v -> hand(main).x = v, 0));
        rows.add(new SliderRow("Y", -100, 100, () -> hand(main).y, v -> hand(main).y = v, 0));
        rows.add(new SliderRow("Z", -100, 100, () -> hand(main).z, v -> hand(main).z = v, 0));
        rows.add(new SliderRow("Obrót X", -180, 180, () -> hand(main).rotX, v -> hand(main).rotX = v, 0));
        rows.add(new SliderRow("Obrót Y", -180, 180, () -> hand(main).rotY, v -> hand(main).rotY = v, 0));
        rows.add(new SliderRow("Obrót Z", -180, 180, () -> hand(main).rotZ, v -> hand(main).rotZ = v, 0));
        rows.add(new SliderRow("Skala %", 10, 200, () -> hand(main).scale, v -> hand(main).scale = v, 0));
    }

    private void buildNickHp() {
        rows.add(new ToggleRow("Pokazuj HP", () -> c().nickHp.enabled, v -> c().nickHp.enabled = v));
        rows.add(new SliderRow("Zasięg (bloki)", 8, 64, () -> c().nickHp.range, v -> c().nickHp.range = v, 0));
        rows.add(new TextRow("HP gracza pojawia się nad jego"));
        rows.add(new TextRow("nickiem. Kolor zależy od ilości"));
        rows.add(new TextRow("zdrowia (zielony, żółty, czerwony)."));
    }

    private void buildGui() {
        rows.add(new HeadingRow("Rozmiar GUI (%)", null));
        rows.add(new ChoiceRow(new int[]{70, 85, 100, 125, 150}, null, () -> c().guiScale, v -> c().guiScale = v));
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
        if (m == M_JUMP) return c().jump.enabled;
        if (m == M_HAT) return c().hat.enabled;
        if (m == M_HIT) return c().hit.enabled;
        if (m == M_CROSS) return c().cross.enabled;
        if (m == M_RATIO) return c().aspect.enabled;
        if (m == M_VIEW) return c().viewModel.enabled;
        if (m == M_NICKHP) return c().nickHp.enabled;
        return el(m - 1).enabled;
    }

    private void toggleModule(int m) {
        if (m == 0) c().enabled = !c().enabled;
        else if (m == M_TARGET) c().target.enabled = !c().target.enabled;
        else if (m == M_RING) c().ring.enabled = !c().ring.enabled;
        else if (m == M_JUMP) c().jump.enabled = !c().jump.enabled;
        else if (m == M_HAT) c().hat.enabled = !c().hat.enabled;
        else if (m == M_HIT) c().hit.enabled = !c().hit.enabled;
        else if (m == M_CROSS) c().cross.enabled = !c().cross.enabled;
        else if (m == M_RATIO) c().aspect.enabled = !c().aspect.enabled;
        else if (m == M_VIEW) c().viewModel.enabled = !c().viewModel.enabled;
        else if (m == M_NICKHP) c().nickHp.enabled = !c().nickHp.enabled;
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

    private int bottom() {
        return vh() - 10;
    }

    private void layoutPanel() {
        ct = (int) Math.ceil(52 / s());
        cw = vw() - 2 * cl;
        gridY = ct + 16;
        cols = Math.max(3, Math.min(6, cw / 104));
        cardW = (cw - (cols - 1) * GAP) / cols;
        int avail = bottom() - gridY - 16;
        int rowsFit = Math.max(1, (avail + GAP) / (CARD_H + GAP));
        perPage = cols * rowsFit;
        pages = (MODULES + perPage - 1) / perPage;
        page = Math.max(0, Math.min(page, pages - 1));
        ncol = Math.max(2, Math.min(3, cw / 190));
        colW = (cw - (ncol - 1) * 14) / ncol;
    }

    private int first() {
        return page * perPage;
    }

    private int last() {
        return Math.min(MODULES, first() + perPage);
    }

    private int cardX(int i) {
        return cl + ((i - first()) % cols) * (cardW + GAP);
    }

    private int cardY(int i) {
        return gridY + ((i - first()) / cols) * (CARD_H + GAP);
    }

    private int navY() {
        return bottom() - 12;
    }

    private int editorX() {
        return vw() - cl - 96;
    }

    private int editorY() {
        return ct - 4;
    }

    private boolean inPreviewModule() {
        return (selected >= 1 && selected <= HudConfig.ELEMENTS) || selected == M_TARGET;
    }

    /** Wiersze ustawień płyną w 2-3 kolumnach; druga kolumna zaczyna się pod podglądem. */
    private void layoutDetail(TextRenderer tr) {
        int n = rows.size();
        rowX = new int[n];
        rowY = new int[n];
        rowW = new int[n];
        int lw = 0;
        for (Row r : rows) lw = Math.max(lw, r.labelWidth(tr));
        rowLw = lw;

        int top = ct + 20;
        int bottom = bottom();
        int col = 0;
        int x = cl;
        int y = top;
        boolean prev = inPreviewModule();
        for (int i = 0; i < n; i++) {
            Row r = rows.get(i);
            if (y + r.h > bottom && col < ncol - 1) {
                col++;
                x = cl + col * (colW + 14);
                y = top + ((col == 1 && prev) ? PREVIEW_H + 6 : 0);
            }
            rowX[i] = x;
            rowY[i] = y;
            rowW[i] = colW;
            y += r.h;
        }
    }

    // ------------------------------------------------------------------ rysowanie

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // tło na cały ekran
        ctx.fillGradient(0, 0, this.width, this.height, Ui.panelTop(), Ui.bg());
        drawLogo(ctx);

        layoutPanel();
        float sc = (float) s();
        int vmx = (int) Math.round(mouseX / s());
        int vmy = (int) Math.round(mouseY / s());
        TextRenderer tr = this.textRenderer;

        MatrixStack m = ctx.getMatrices();
        m.push();
        m.scale(sc, sc, 1f);

        // przycisk edytora HUD (prawy górny róg)
        boolean eh = vmx >= editorX() && vmx < editorX() + 96 && vmy >= editorY() && vmy < editorY() + 14;
        Ui.roundRect(ctx, editorX(), editorY(), 96, 14, eh ? Ui.cardHover() : Ui.cardBorder());
        Ui.roundRect(ctx, editorX() + 1, editorY() + 1, 94, 12, Ui.cardBg());
        String eb = "EDYTOR HUD";
        ctx.drawText(tr, eb, editorX() + (96 - tr.getWidth(eb)) / 2, editorY() + 3, Ui.accent(), false);

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
        ctx.drawText(tr, "TWOJE MODUŁY", cl, ct, Ui.text(), false);
        for (int i = first(); i < last(); i++) {
            renderCard(ctx, tr, i, cardX(i), cardY(i), mx, my);
        }
        if (pages > 1) {
            int cx = vw() / 2;
            String pg = (page + 1) + "/" + pages;
            ctx.drawText(tr, "<", cx - 40, navY() + 2, page > 0 ? Ui.accent() : Ui.muted(), false);
            ctx.drawText(tr, pg, cx - tr.getWidth(pg) / 2, navY() + 2, Ui.text(), false);
            ctx.drawText(tr, ">", cx + 34, navY() + 2, page < pages - 1 ? Ui.accent() : Ui.muted(), false);
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

        ctx.drawText(tr, tr.trimToWidth(NAMES[i], cardW - 12), x + 6, y + 21, Ui.text(), false);
        ctx.drawText(tr, tr.trimToWidth(DESCS[i], cardW - 12), x + 6, y + 31, Ui.muted(), false);

        if (hasToggle(i)) {
            boolean on = moduleOn(i);
            String st = cardW >= 100 ? (on ? "WŁĄCZONE" : "WYŁĄCZONE") : (on ? "ON" : "OFF");
            ctx.drawText(tr, st, x + 6, y + 42, on ? Ui.accent() : Ui.muted(), false);
            int sw = 22, sh = 10;
            int sx = x + cardW - 6 - sw;
            int sy = y + 41;
            ctx.fill(sx, sy, sx + sw, sy + sh, on ? Ui.accent() : Ui.off());
            int kx = on ? sx + sw - sh + 1 : sx + 1;
            ctx.fill(kx, sy + 1, kx + sh - 2, sy + sh - 1, 0xFFFFFFFF);
        }
    }

    /** Rysuje widok ustawień modułu. Zwraca true, gdy pole tekstowe ma być widoczne. */
    private boolean renderDetail(DrawContext ctx, TextRenderer tr, int mx, int my) {
        String back = "< WRÓĆ";
        int bw = tr.getWidth(back);
        boolean backHover = mx >= cl && mx < cl + bw && my >= ct - 4 && my < ct + 10;
        ctx.drawText(tr, back, cl, ct, backHover ? Ui.text() : Ui.accent(), false);

        int nx = cl + bw + 12;
        String tag = TAGS[selected];
        int tw = tr.getWidth(tag);
        Ui.roundRect(ctx, nx, ct - 3, tw + 10, 12, Ui.chipBg());
        ctx.drawText(tr, tag, nx + 5, ct - 1, Ui.accent(), false);
        ctx.drawText(tr, NAMES[selected], nx + tw + 18, ct, Ui.text(), false);

        layoutDetail(tr);

        // podgląd na żywo
        if (inPreviewModule()) {
            int x2 = cl + (colW + 14);
            int y2 = ct + 20;
            Ui.roundRect(ctx, x2, y2, colW, PREVIEW_H, Ui.cardBorder());
            Ui.roundRect(ctx, x2 + 1, y2 + 1, colW - 2, PREVIEW_H - 2, Ui.cardBg());
            ctx.drawText(tr, "PODGLĄD", x2 + 8, y2 + 5, Ui.muted(), false);
            boolean drawn = selected == M_TARGET
                    ? TargetHud.drawPreview(ctx, x2 + 8, y2 + 17, colW - 16, PREVIEW_H - 23)
                    : HudRenderer.drawPreview(ctx, selected - 1, x2 + 8, y2 + 17, colW - 16, PREVIEW_H - 23);
            if (!drawn) {
                ctx.drawText(tr, "Brak podglądu.", x2 + 8, y2 + 24, Ui.muted(), false);
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

        // przycisk Edytor HUD
        if (vx >= editorX() && vx < editorX() + 96 && vy >= editorY() && vy < editorY() + 14) {
            openEditor();
            return true;
        }

        if (selected < 0) {
            // strony
            if (pages > 1 && vy >= navY() && vy < navY() + 12) {
                int cx = vw() / 2;
                if (vx >= cx - 50 && vx < cx - 20) page = Math.max(0, page - 1);
                else if (vx >= cx + 20 && vx < cx + 50) page = Math.min(pages - 1, page + 1);
                return true;
            }
            for (int i = first(); i < last(); i++) {
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
        if (vx >= cl && vx < cl + bw && vy >= ct - 4 && vy < ct + 10) {
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
    public boolean mouseScrolled(double mx, double my, double horizontalAmount, double verticalAmount) {
        if (selected < 0 && pages > 1) {
            page = Math.max(0, Math.min(pages - 1, page + (verticalAmount < 0 ? 1 : -1)));
            return true;
        }
        return super.mouseScrolled(mx / s(), my / s(), horizontalAmount, verticalAmount);
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
}
