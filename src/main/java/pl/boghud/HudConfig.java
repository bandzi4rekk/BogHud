package pl.boghud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Ustawienia moda, zapisywane w config/boghud.json */
public class HudConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("boghud.json");

    public static final int ELEMENTS = 8;
    /** Kolejność = kolejność układania HUD-a od góry. Nowe elementy tylko na końcu! */
    public static final String[] NAMES = {"Nick", "Tekst", "FPS", "Ping", "Koordynaty", "Pancerz", "CPS", "Klawisze"};

    /** Jeden element HUD-a (osobna ramka z własnymi ustawieniami). */
    public static class Element {
        public boolean enabled = true;
        public int scalePercent = 100;
        public int textColor = 0xFFFFFF;
        public int accentColor = 0x55FFFF;
        public boolean background = true;
        public int backgroundAlpha = 170;
        public boolean shadow = true;
    }

    /** Pierścień wokół celu (efekt 3D). */
    public static class Ring {
        public boolean enabled = true;
        public int color = 0xFF4D6D;
        public int alpha = 200;
        public int radiusPercent = 100;
        public boolean animate = true;
    }

    public boolean enabled = true;
    public String customText = "Bog HUD";
    public Element[] elements = defaultElements();

    /** Pozycja stosu HUD-a (lewy górny róg, piksele GUI). */
    public int stackX = 4;
    public int stackY = 4;

    /** Target HUD: osobna ramka przy celowniku (przesunięcie od środka ekranu). */
    public Element target = defaultTarget();
    public int targetX = 24;
    public int targetY = 24;
    public Ring ring = new Ring();

    // Ustawienia GUI (kolory i rozmiar okna ustawień). Domyślnie granatowy motyw z różowym akcentem.
    public int guiAccent = 0xFF4D6D;
    public int guiBackground = 0x07182E;
    public int guiAlpha = 245;
    public int guiScale = 100;
    /** 0 = config sprzed nowego wyglądu; przy wczytaniu ustawiamy nowy motyw jeden raz. */
    public int guiVersion = 0;

    private static Element[] defaultElements() {
        Element[] a = new Element[ELEMENTS];
        for (int i = 0; i < ELEMENTS; i++) a[i] = new Element();
        // Nick domyślnie w żółtym akcencie (pasuje do ikony pioruna)
        a[0].accentColor = 0xFFE135;
        return a;
    }

    private static Element defaultTarget() {
        Element e = new Element();
        e.accentColor = 0xFF4D6D;
        return e;
    }

    /** Naprawia braki (np. po wczytaniu configu ze starszej wersji moda). */
    public void fixup() {
        if (customText == null) customText = "";
        if (target == null) target = defaultTarget();
        if (ring == null) ring = new Ring();
        Element[] d = defaultElements();
        if (elements == null) {
            elements = d;
        } else if (elements.length != ELEMENTS) {
            // zachowujemy stare ustawienia, nowe elementy dostają domyślne
            for (int i = 0; i < Math.min(elements.length, ELEMENTS); i++) {
                if (elements[i] != null) d[i] = elements[i];
            }
            elements = d;
        } else {
            for (int i = 0; i < ELEMENTS; i++) {
                if (elements[i] == null) elements[i] = d[i];
            }
        }
        if (guiVersion < 2) {
            guiAccent = 0xFF4D6D;
            guiBackground = 0x07182E;
            guiAlpha = 245;
            guiVersion = 2;
        }
    }

    public static HudConfig load() {
        if (Files.exists(PATH)) {
            try (Reader r = Files.newBufferedReader(PATH)) {
                HudConfig c = GSON.fromJson(r, HudConfig.class);
                if (c != null) {
                    c.fixup();
                    return c;
                }
            } catch (Exception e) {
                System.err.println("[BogHUD] Nie udało się wczytać configu: " + e);
            }
        }
        HudConfig c = new HudConfig();
        c.fixup();
        c.save();
        return c;
    }

    public void save() {
        try (Writer w = Files.newBufferedWriter(PATH)) {
            GSON.toJson(this, w);
        } catch (Exception e) {
            System.err.println("[BogHUD] Nie udało się zapisać configu: " + e);
        }
    }
}
