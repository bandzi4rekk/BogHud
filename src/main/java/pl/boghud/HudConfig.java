package pl.boghud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Ustawienia moda, zapisywane w config/boghud.json */
public class HudConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("boghud.json");

    public static final int ELEMENTS = 6;
    public static final int WINDOWS = 8; // Ogólne + 6 elementów + Ustawienia GUI
    /** Kolejność = kolejność układania HUD-a od góry. */
    public static final String[] NAMES = {"Nick", "Tekst", "FPS", "Ping", "Koordynaty", "Pancerz"};

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

    public boolean enabled = true;
    public String customText = "Bog HUD";
    public Element[] elements = defaultElements();

    /** Pozycja stosu HUD-a (lewy górny róg, piksele GUI). */
    public int stackX = 4;
    public int stackY = 4;

    // Ustawienia GUI (kolory i rozmiar okien)
    public int guiAccent = 0x5CE1E6;
    public int guiBackground = 0x141A22;
    public int guiAlpha = 235;
    public int guiScale = 100;

    /** Które okna ustawień są zwinięte. */
    public boolean[] winCollapsed = collapsedAll();

    private static Element[] defaultElements() {
        Element[] a = new Element[ELEMENTS];
        for (int i = 0; i < ELEMENTS; i++) a[i] = new Element();
        // Nick domyślnie w żółtym akcencie (pasuje do ikony pioruna)
        a[0].accentColor = 0xFFE135;
        return a;
    }

    private static boolean[] collapsedAll() {
        boolean[] a = new boolean[WINDOWS];
        Arrays.fill(a, true);
        return a;
    }

    /** Naprawia braki (np. po wczytaniu configu ze starszej wersji moda). */
    public void fixup() {
        if (customText == null) customText = "";
        if (elements == null || elements.length != ELEMENTS) {
            elements = defaultElements();
        } else {
            Element[] d = defaultElements();
            for (int i = 0; i < ELEMENTS; i++) {
                if (elements[i] == null) elements[i] = d[i];
            }
        }
        if (winCollapsed == null || winCollapsed.length != WINDOWS) winCollapsed = collapsedAll();
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
