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
    /** Nowe elementy tylko na końcu! */
    public static final String[] NAMES = {"Nick", "Tekst", "FPS", "Ping", "Koordynaty", "Pancerz", "CPS", "Klawisze"};
    /** Domyślne pozycje Y (układ "pod sobą" po lewej stronie ekranu). */
    private static final int[] DEFAULT_Y = {4, 20, 36, 52, 68, 84, 170, 186};

    /** Jeden element HUD-a (osobna ramka z własnymi ustawieniami i pozycją). */
    public static class Element {
        public boolean enabled = true;
        public int scalePercent = 100;
        public int textColor = 0xFFFFFF;
        public int accentColor = 0x55FFFF;
        public boolean background = true;
        public int backgroundAlpha = 170;
        public boolean shadow = true;
        /** Lewy górny róg na ekranie (piksele GUI); -1 = jeszcze nieustawione. */
        public int x = -1;
        public int y = -1;
    }

    /** Pierścień wokół celu (efekt 3D). */
    public static class Ring {
        public boolean enabled = true;
        public int color = 0xFF4D6D;
        public int alpha = 200;
        public int radiusPercent = 100;
        public boolean animate = true;
    }

    /** Ustawienia jednej ręki w View Model (przesunięcie w setnych częściach bloku, obrót w stopniach). */
    public static class HandCfg {
        public int x, y, z;
        public int rotX, rotY, rotZ;
        public int scale = 100;
    }

    public static class ViewModel {
        public boolean enabled = false;
        public HandCfg main = new HandCfg();
        public HandCfg off = new HandCfg();
    }

    /** Proporcje ekranu (ratioPercent: 178 = 16:9, 133 = 4:3). */
    public static class Aspect {
        public boolean enabled = false;
        public int ratioPercent = 133;
    }

    /** HP graczy nad ich nickami. */
    public static class NickHp {
        public boolean enabled = true;
        public int range = 32;
    }

    /** Kółko rozchodzące się przy skoku. */
    public static class JumpCircles {
        public boolean enabled = true;
        public int color = 0xFF4D6D;
        public int alpha = 200;
        public int radiusPercent = 100;
        public int durationMs = 700;
    }

    /** Stożkowy kapelusz nad głową. */
    public static class ChinaHat {
        public boolean enabled = true;
        public boolean showOthers = false;
        public int color = 0xFF4D6D;
        public int alpha = 170;
        public int sizePercent = 100;
    }

    /** Cząsteczki przy trafieniu: 0 = crit, 1 = magia, 2 = ogień, 3 = gwiazdy. */
    public static class HitParticles {
        public boolean enabled = true;
        public int type = 0;
        public int count = 12;
    }

    /** Kółko wokół celownika. */
    public static class Crosshair {
        public boolean enabled = false;
        public int color = 0xFFFFFF;
        public int radius = 8;
        public int thickness = 1;
        public boolean reactToTarget = true;
    }

    public boolean enabled = true;
    public String customText = "Bog HUD";
    public Element[] elements = defaultElements();

    /** Target HUD: osobna ramka przy celowniku (przesunięcie od środka ekranu). */
    public Element target = defaultTarget();
    public int targetX = 24;
    public int targetY = 24;
    public Ring ring = new Ring();

    // Wizualia
    public JumpCircles jump = new JumpCircles();
    public ChinaHat hat = new ChinaHat();
    public HitParticles hit = new HitParticles();
    public Crosshair cross = new Crosshair();

    // Opcje "Ekran"
    public Aspect aspect = new Aspect();
    public ViewModel viewModel = new ViewModel();
    public NickHp nickHp = new NickHp();

    // Ustawienia GUI (kolory i rozmiar okna ustawień). Domyślnie granatowy motyw z różowym akcentem.
    public int guiAccent = 0xFF4D6D;
    public int guiBackground = 0x07182E;
    public int guiAlpha = 245;
    public int guiScale = 100;
    /** 0 = config sprzed nowego wyglądu; przy wczytaniu ustawiamy nowy motyw jeden raz. */
    public int guiVersion = 0;

    private static Element[] defaultElements() {
        Element[] a = new Element[ELEMENTS];
        for (int i = 0; i < ELEMENTS; i++) {
            a[i] = new Element();
            a[i].x = 4;
            a[i].y = DEFAULT_Y[i];
        }
        // Nick domyślnie w żółtym akcencie (pasuje do ikony pioruna)
        a[0].accentColor = 0xFFE135;
        return a;
    }

    private static Element defaultTarget() {
        Element e = new Element();
        e.accentColor = 0xFF4D6D;
        return e;
    }

    /** Ustawia wszystkie elementy HUD z powrotem w kolumnie "pod sobą". */
    public void resetPositions() {
        for (int i = 0; i < ELEMENTS; i++) {
            elements[i].x = 4;
            elements[i].y = DEFAULT_Y[i];
        }
        targetX = 24;
        targetY = 24;
    }

    /** Naprawia braki (np. po wczytaniu configu ze starszej wersji moda). */
    public void fixup() {
        if (customText == null) customText = "";
        if (target == null) target = defaultTarget();
        if (ring == null) ring = new Ring();
        if (jump == null) jump = new JumpCircles();
        if (hat == null) hat = new ChinaHat();
        if (hit == null) hit = new HitParticles();
        if (cross == null) cross = new Crosshair();
        if (aspect == null) aspect = new Aspect();
        if (viewModel == null) viewModel = new ViewModel();
        if (viewModel.main == null) viewModel.main = new HandCfg();
        if (viewModel.off == null) viewModel.off = new HandCfg();
        if (nickHp == null) nickHp = new NickHp();

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
        // elementy bez pozycji dostają domyślną (kolumna pod sobą)
        for (int i = 0; i < ELEMENTS; i++) {
            if (elements[i].x < 0 || elements[i].y < 0) {
                elements[i].x = 4;
                elements[i].y = DEFAULT_Y[i];
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
