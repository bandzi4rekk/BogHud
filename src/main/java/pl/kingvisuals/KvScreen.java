package pl.kingvisuals;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import pl.kingvisuals.Rows.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Menu KingVisuals w stylu LeafVisuals (przezroczysty panel, zaokrąglone rogi), w fioletowo-niebieskich kolorach.
 * Zakładki HUD / Visuals / Client / FPS, wyszukiwarka modułów, wiersze z przełącznikiem.
 * LPM na wierszu włącza lub wyłącza moduł, PPM (albo ⋮) otwiera jego ustawienia.
 */
public class KvScreen extends Screen {
    private static final String[] TABS = {"HUD", "Visuals", "Client", "FPS"};
    private static final String[] HIT_TYPES = {"Crit", "Magia", "Ogień", "Gwiazdy"};
    private static final int ROW_H = 24;
    private static final int ROW_GAP = 4;
    private static final int RED = 0xFFE05555;
    private static final int GREEN = 0xFF55E077;
    private static final int BLUE_CH = 0xFF5599E0;
    private static final int[] PRESETS = {0x8A4DFF, 0x3D8BFF, 0xB07CFF, 0xFF4DD2, 0x4DFFD2, 0xFFFFFF};

    private static int lastTab = 0;

    private final Screen parent;
    private TextFieldWidget search;
    private String query = "";
    private int scroll = 0;

    private final List<Module> modules = new ArrayList<>();
    private Module open = null;
    private String openName = null;
    private final List<Row> rows = new ArrayList<>();
    private int[] rowX = new int[0], rowY = new int[0], rowW = new int[0];
    private int rowLw;
    private Row activeRow;
    private int activeX, activeW;

    private int px, py, pw, ph;

    public KvScreen(Screen parent) {
        super(Text.literal("KingVisuals"));
        this.parent = parent;
    }

    private KvConfig c() {
        return KvMod.CONFIG;
    }

    private class Module {
        final int tab;
        final String name;
        final Supplier<String> desc;
        final BooleanSupplier on;
        final Consumer<Boolean> set;
        final Runnable build;

        Module(int tab, String name, Supplier<String> desc, BooleanSupplier on, Consumer<Boolean> set, Runnable build) {
            this.tab = tab;
            this.name = name;
            this.desc = desc;
            this.on = on;
            this.set = set;
            this.build = build;
        }
    }

    // ------------------------------------------------------------------ init i moduły

    @Override
    protected void init() {
        search = new TextFieldWidget(this.textRenderer, 0, 0, 100, 12, Text.literal("Szukaj"));
        search.setDrawsBackground(false);
        search.setMaxLength(32);
        search.setText(query);
        search.setChangedListener(s -> {
            query = s;
            scroll = 0;
        });
        addDrawableChild(search);

        buildModules();
        activeRow = null;
        open = null;
        rows.clear();
        if (openName != null) {
            for (Module m : modules) {
                if (m.name.equals(openName)) {
                    openModule(m);
                    break;
                }
            }
        }
    }

    private void buildModules() {
        modules.clear();

        // zakładka HUD
        modules.add(new Module(0, "Watermark", () -> "Napis KingVisuals w prawym górnym rogu",
                () -> c().overlay.watermark, v -> c().overlay.watermark = v, this::buildOverlay));
        modules.add(new Module(0, "FPS", () -> "Pokazuje klatki na sekundę",
                () -> c().overlay.fps, v -> c().overlay.fps = v, this::buildOverlay));
        modules.add(new Module(0, "Ping", () -> "Pokazuje opóźnienie do serwera",
                () -> c().overlay.ping, v -> c().overlay.ping = v, this::buildOverlay));

        // zakładka Visuals
        modules.add(new Module(1, "Jump Circles",
                () -> "Promień: " + c().jump.radiusPercent + "%, czas: " + c().jump.durationMs + " ms",
                () -> c().jump.enabled, v -> c().jump.enabled = v, this::buildJump));
        modules.add(new Module(1, "China Hat",
                () -> "Rozmiar: " + c().hat.sizePercent + "%, innym graczom: " + (c().hat.showOthers ? "ON" : "OFF"),
                () -> c().hat.enabled, v -> c().hat.enabled = v, this::buildHat));
        modules.add(new Module(1, "Hit Particles",
                () -> "Rodzaj: " + HIT_TYPES[Math.max(0, Math.min(3, c().hit.type))] + ", ilość: " + c().hit.count,
                () -> c().hit.enabled, v -> c().hit.enabled = v, this::buildHit));
        modules.add(new Module(1, "Ring Crosshair",
                () -> "Promień: " + c().cross.radius + ", grubość: " + c().cross.thickness,
                () -> c().cross.enabled, v -> c().cross.enabled = v, this::buildCross));
    }

    private void openModule(Module m) {
        open = m;
        openName = m.name;
        rows.clear();
        activeRow = null;
        m.build.run();
    }

    private List<Module> visible() {
        List<Module> out = new ArrayList<>();
        String q = query.trim().toLowerCase(Locale.ROOT);
        for (Module m : modules) {
            if (q.isEmpty() ? m.tab == lastTab : m.name.toLowerCase(Locale.ROOT).contains(q)) out.add(m);
        }
        return out;
    }

    // ------------------------------------------------------------------ ustawienia modułów

    private void buildOverlay() {
        rows.add(new ToggleRow("Watermark", () -> c().overlay.watermark, v -> c().overlay.watermark = v));
        rows.add(new ToggleRow("FPS", () -> c().overlay.fps, v -> c().overlay.fps = v));
        rows.add(new ToggleRow("Ping", () -> c().overlay.ping, v -> c().overlay.ping = v));
        rows.add(new SliderRow("Skala %", 50, 200, () -> c().overlay.scalePercent, v -> c().overlay.scalePercent = v, 0));
        rows.add(new TextRow("Pozycję zmienisz przyciskiem"));
        rows.add(new TextRow("Edytuj HUD (prawy górny róg)."));
    }

    private void buildJump() {
        rows.add(new ToggleRow("Włączone", () -> c().jump.enabled, v -> c().jump.enabled = v));
        rows.add(new SliderRow("Promień %", 50, 200, () -> c().jump.radiusPercent, v -> c().jump.radiusPercent = v, 0));
        rows.add(new SliderRow("Czas (ms)", 300, 1500, () -> c().jump.durationMs, v -> c().jump.durationMs = v, 0));
        rows.add(new SliderRow("Krycie", 20, 255, () -> c().jump.alpha, v -> c().jump.alpha = v, 0));
        rows.add(new HeadingRow("Kolor kółka", () -> c().jump.color));
        rows.add(new PresetRow(PRESETS, v -> c().jump.color = v));
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
        rows.add(new PresetRow(PRESETS, v -> c().hat.color = v));
        addChannels(() -> c().hat.color, v -> c().hat.color = v);
        rows.add(new TextRow("Swój kapelusz widzisz w trzeciej"));
        rows.add(new TextRow("osobie (F5)."));
    }

    private void buildHit() {
        rows.add(new ToggleRow("Włączone", () -> c().hit.enabled, v -> c().hit.enabled = v));
        rows.add(new HeadingRow("Rodzaj cząsteczek", null));
        rows.add(new ChoiceRow(new int[]{0, 1, 2, 3}, HIT_TYPES, () -> c().hit.type, v -> c().hit.type = v));
        rows.add(new SliderRow("Ilość", 1, 40, () -> c().hit.count, v -> c().hit.count = v, 0));
        rows.add(new TextRow("Cząsteczki pojawiają się,"));
        rows.add(new TextRow("gdy uderzysz stwora."));
    }

    private void buildCross() {
        rows.add(new ToggleRow("Włączony", () -> c().cross.enabled, v -> c().cross.enabled = v));
        rows.add(new SliderRow("Promień", 3, 30, () -> c().cross.radius, v -> c().cross.radius = v, 0));
        rows.add(new SliderRow("Grubość", 1, 4, () -> c().cross.thickness, v -> c().cross.thickness = v, 0));
        rows.add(new ToggleRow("Czerwony na celu", () -> c().cross.reactToTarget, v -> c().cross.reactToTarget = v));
        rows.add(new HeadingRow("Kolor kółka", () -> c().cross.color));
        rows.add(new PresetRow(PRESETS, v -> c().cross.color = v));
        addChannels(() -> c().cross.color, v -> c().cross.color = v);
    }

    private void addChannels(IntSupplier get, IntConsumer set) {
        rows.add(new SliderRow("R", 0, 255, () -> (get.getAsInt() >> 16) & 255,
                v -> set.accept((get.getAsInt() & 0x00FFFF) | (v << 16)), RED));
        rows.add(new SliderRow("G", 0, 255, () -> (get.getAsInt() >> 8) & 255,
                v -> set.accept((get.getAsInt() & 0xFF00FF) | (v << 8)), GREEN));
        rows.add(new SliderRow("B", 0, 255, () -> get.getAsInt() & 255,
                v -> set.accept((get.getAsInt() & 0xFFFF00) | v), BLUE_CH));
    }

    // ------------------------------------------------------------------ układ

    private void layout() {
        pw = Math.max(340, Math.min(520, this.width - 40));
        ph = Math.max(190, Math.min(300, this.height - 40));
        px = (this.width - pw) / 2;
        py = (this.height - ph) / 2;
    }

    private int listTop() {
        return py + 58;
    }

    private int visibleRows() {
        int avail = (py + ph - 26) - listTop();
        return Math.max(1, (avail + ROW_GAP) / (ROW_H + ROW_GAP));
    }

    private int tabY() {
        return py + 36;
    }

    private int editX() {
        return px + pw - 14 - 76;
    }

    private int editY() {
        return py + 9;
    }

    private int searchX() {
        return px + pw - 14 - 132;
    }

    private int searchY() {
        return py + 35;
    }

    private static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void layoutSettings(TextRenderer tr) {
        int n = rows.size();
        rowX = new int[n];
        rowY = new int[n];
        rowW = new int[n];
        int lw = 0;
        for (Row r : rows) lw = Math.max(lw, r.labelWidth(tr));
        rowLw = lw;

        boolean two = pw >= 420;
        int colW = two ? (pw - 28 - 14) / 2 : pw - 28;
        int top = py + 58;
        int bottom = py + ph - 26;
        int col = 0;
        int x = px + 14;
        int y = top;
        for (int i = 0; i < n; i++) {
            Row r = rows.get(i);
            if (two && col == 0 && y + r.h > bottom) {
                col = 1;
                x = px + 14 + colW + 14;
                y = top;
            }
            rowX[i] = x;
            rowY[i] = y;
            rowW[i] = colW;
            y += r.h;
        }
    }

    // ------------------------------------------------------------------ rysowanie

    // Bez rozmycia tła (rysujemy własne przyciemnienie w render)
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        TextRenderer tr = this.textRenderer;
        layout();

        ctx.fill(0, 0, this.width, this.height, 0x66080618);
        Ui.roundRect(ctx, px, py, pw, ph, 12, 0xC0140E30);
        Ui.roundOutline(ctx, px, py, pw, ph, 12, 0x40FFFFFF);

        // tytuł: King (biały) + Visuals (gradient fiolet-niebieski) + wersja
        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(px + 16, py + 8, 0f);
        m.scale(1.6f, 1.6f, 1f);
        int wK = tr.getWidth("King");
        ctx.drawText(tr, "King", 0, 0, 0xFFFFFFFF, false);
        int wV = Ui.gradText(ctx, tr, "Visuals", wK, 0, Ui.PURPLE, Ui.BLUE, false);
        m.pop();
        ctx.drawText(tr, "v0.1.0", px + 16 + Math.round((wK + wV) * 1.6f) + 6, py + 15, Ui.MUTED, false);

        // przycisk Edytuj HUD
        boolean eh = hit(mx, my, editX(), editY(), 76, 14);
        Ui.roundRect(ctx, editX(), editY(), 76, 14, 7, eh ? 0x55FFFFFF : 0x2EFFFFFF);
        Ui.roundOutline(ctx, editX(), editY(), 76, 14, 7, 0x55FFFFFF);
        String eb = "Edytuj HUD";
        ctx.drawText(tr, eb, editX() + (76 - tr.getWidth(eb)) / 2, editY() + 3, Ui.TEXT, false);

        ctx.fill(px + 14, py + 30, px + pw - 14, py + 31, 0x28FFFFFF);

        if (open == null) {
            renderTabsAndSearch(ctx, tr, mx, my);
            renderList(ctx, tr, mx, my);
            search.visible = true;
            search.setX(searchX() + 20);
            search.setY(searchY() + 2);
            search.setWidth(108);
        } else {
            search.visible = false;
            renderSettings(ctx, tr, mx, my);
        }

        String hint = open == null ? "LPM: włącz/wyłącz    PPM: ustawienia" : "Esc: wróć";
        ctx.drawText(tr, hint, px + (pw - tr.getWidth(hint)) / 2, py + ph - 16, Ui.MUTED, false);

        super.render(ctx, mx, my, delta);
    }

    private void renderTabsAndSearch(DrawContext ctx, TextRenderer tr, int mx, int my) {
        boolean searching = !query.trim().isEmpty();
        int tx = px + 16;
        for (int i = 0; i < TABS.length; i++) {
            int tw = tr.getWidth(TABS[i]) + 18;
            boolean sel = i == lastTab && !searching;
            boolean hover = hit(mx, my, tx, tabY(), tw, 14);
            if (sel) {
                Ui.gradPill(ctx, tx, tabY(), tw, 14, Ui.PURPLE, Ui.BLUE, 0xFF);
            } else if (hover) {
                Ui.roundRect(ctx, tx, tabY(), tw, 14, 7, 0x22FFFFFF);
            }
            ctx.drawText(tr, TABS[i], tx + 9, tabY() + 3, sel ? 0xFFFFFFFF : Ui.MUTED, false);
            tx += tw + 6;
        }

        // pole wyszukiwania
        int sx = searchX(), sy = searchY();
        Ui.roundRect(ctx, sx, sy, 132, 16, 8, 0x22FFFFFF);
        Ui.roundOutline(ctx, sx, sy, 132, 16, 8, 0x44FFFFFF);
        Ui.roundOutline(ctx, sx + 7, sy + 4, 7, 7, 3, Ui.MUTED);
        ctx.fill(sx + 13, sy + 11, sx + 15, sy + 13, Ui.MUTED);
        if (query.isEmpty() && !search.isFocused()) {
            ctx.drawText(tr, "Szukaj modułów...", sx + 20, sy + 4, Ui.MUTED, false);
        }
    }

    private void renderList(DrawContext ctx, TextRenderer tr, int mx, int my) {
        List<Module> list = visible();
        int vis = visibleRows();
        int maxScroll = Math.max(0, list.size() - vis);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        for (int i = scroll; i < Math.min(list.size(), scroll + vis); i++) {
            int y = listTop() + (i - scroll) * (ROW_H + ROW_GAP);
            renderModuleRow(ctx, tr, list.get(i), px + 14, y, pw - 28, mx, my);
        }
        if (list.isEmpty()) {
            String t = query.trim().isEmpty() ? "Wkrótce..." : "Brak wyników.";
            ctx.drawText(tr, t, px + 20, listTop() + 6, Ui.MUTED, false);
        }
        if (maxScroll > 0) {
            int trackH = vis * (ROW_H + ROW_GAP) - ROW_GAP;
            int thumbH = Math.max(12, trackH * vis / list.size());
            int thumbY = listTop() + (trackH - thumbH) * scroll / maxScroll;
            ctx.fill(px + pw - 9, listTop(), px + pw - 7, listTop() + trackH, 0x22FFFFFF);
            Ui.vGradient(ctx, px + pw - 9, thumbY, 2, thumbH, Ui.PURPLE, Ui.BLUE);
        }
    }

    private void renderModuleRow(DrawContext ctx, TextRenderer tr, Module m, int x, int y, int w, int mx, int my) {
        boolean on = m.on.getAsBoolean();
        boolean hover = hit(mx, my, x, y, w, ROW_H);

        Ui.roundRect(ctx, x, y, w, ROW_H, 7, hover ? 0x3AA08CFF : 0x22A08CFF);
        int border = on ? ((0xB0 << 24) | Ui.mix(Ui.PURPLE, Ui.BLUE, 0.5)) : 0x30FFFFFF;
        Ui.roundOutline(ctx, x, y, w, ROW_H, 7, border);
        if (on) Ui.vGradient(ctx, x + 3, y + 5, 2, ROW_H - 10, Ui.PURPLE, Ui.BLUE);

        Ui.roundOutline(ctx, x + 11, y + 6, 12, 12, 6, 0x66FFFFFF);
        ctx.drawText(tr, "?", x + 15, y + 8, Ui.MUTED, false);

        ctx.drawText(tr, m.name, x + 30, y + 8, on ? Ui.TEXT : Ui.MUTED, false);
        int descX = x + 30 + 86;
        int availW = w - (30 + 86) - 62;
        if (availW > 20) {
            ctx.drawText(tr, tr.trimToWidth(m.desc.get(), availW), descX, y + 8, Ui.MUTED, false);
        }

        Rows.toggle(ctx, x + w - 58, y + 6, on);
        for (int k = 0; k < 3; k++) {
            ctx.fill(x + w - 20, y + 6 + k * 5, x + w - 18, y + 8 + k * 5, Ui.MUTED);
        }
    }

    private void renderSettings(DrawContext ctx, TextRenderer tr, int mx, int my) {
        String back = "< Wróć";
        int bw = tr.getWidth(back);
        boolean bh = hit(mx, my, px + 14, py + 34, bw + 4, 14);
        ctx.drawText(tr, back, px + 16, py + 38, bh ? Ui.TEXT : Ui.LABEL, false);
        ctx.drawText(tr, open.name, px + 16 + bw + 12, py + 38, Ui.TEXT, false);

        layoutSettings(tr);
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).render(ctx, tr, rowX[i], rowY[i], rowW[i], rowLw, mx, my);
        }
    }

    // ------------------------------------------------------------------ mysz i klawiatura

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (open == null && search.visible && search.isMouseOver(mx, my)) {
            return super.mouseClicked(mx, my, button);
        }
        search.setFocused(false);

        if (button == 0 && hit(mx, my, editX(), editY(), 76, 14)) {
            if (this.client != null) this.client.setScreen(new KvHudEditor(this));
            return true;
        }

        TextRenderer tr = this.textRenderer;
        if (open == null) {
            if (button == 0 && my >= tabY() && my < tabY() + 14) {
                int tx = px + 16;
                for (int i = 0; i < TABS.length; i++) {
                    int tw = tr.getWidth(TABS[i]) + 18;
                    if (mx >= tx && mx < tx + tw) {
                        lastTab = i;
                        scroll = 0;
                        search.setText("");
                        return true;
                    }
                    tx += tw + 6;
                }
            }
            List<Module> list = visible();
            int vis = visibleRows();
            for (int i = scroll; i < Math.min(list.size(), scroll + vis); i++) {
                int y = listTop() + (i - scroll) * (ROW_H + ROW_GAP);
                int x = px + 14, w = pw - 28;
                if (hit(mx, my, x, y, w, ROW_H)) {
                    Module m = list.get(i);
                    if (button == 1 || mx >= x + w - 28) openModule(m);
                    else if (button == 0) m.set.accept(!m.on.getAsBoolean());
                    return true;
                }
            }
            return true;
        }

        // ustawienia modułu
        if (button != 0) return true;
        int bw = tr.getWidth("< Wróć");
        if (hit(mx, my, px + 14, py + 34, bw + 4, 14)) {
            open = null;
            openName = null;
            rows.clear();
            activeRow = null;
            return true;
        }
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            if (hit(mx, my, rowX[i], rowY[i], rowW[i], r.h)) {
                activeRow = r;
                activeX = rowX[i];
                activeW = rowW[i];
                r.click(mx, my, rowX[i], rowW[i], rowLw);
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0 && activeRow != null) {
            activeRow.drag(mx, my, activeX, activeW, rowLw);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        activeRow = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontalAmount, double verticalAmount) {
        if (open == null) {
            scroll += verticalAmount < 0 ? 1 : -1;
            return true;
        }
        return super.mouseScrolled(mx, my, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC: z ustawień modułu wraca do listy, z listy zamyka menu
        if (keyCode == 256 && open != null) {
            open = null;
            openName = null;
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
