package pl.kingvisuals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Ustawienia moda, zapisywane w config/kingvisuals.json */
public class KvConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("kingvisuals.json");

    /** Napis w rogu ekranu: KingVisuals + FPS + ping (każdy do włączenia osobno). */
    public static class Overlay {
        public boolean watermark = true;
        public boolean fps = true;
        public boolean ping = true;
        /** Odległość od prawej krawędzi i od góry ekranu. */
        public int offX = 4;
        public int offY = 4;
        public int scalePercent = 100;
    }

    /** Kółko rozchodzące się przy skoku. */
    public static class JumpCircles {
        public boolean enabled = true;
        public int color = 0x8A4DFF;
        public int alpha = 200;
        public int radiusPercent = 100;
        public int durationMs = 700;
    }

    /** Stożkowy kapelusz nad głową. */
    public static class ChinaHat {
        public boolean enabled = true;
        public boolean showOthers = false;
        public int color = 0x8A4DFF;
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
        public boolean enabled = true;
        public int color = 0xB07CFF;
        public int radius = 8;
        public int thickness = 1;
        public boolean reactToTarget = true;
    }

    public Overlay overlay = new Overlay();
    public JumpCircles jump = new JumpCircles();
    public ChinaHat hat = new ChinaHat();
    public HitParticles hit = new HitParticles();
    public Crosshair cross = new Crosshair();

    public void fixup() {
        if (overlay == null) overlay = new Overlay();
        if (jump == null) jump = new JumpCircles();
        if (hat == null) hat = new ChinaHat();
        if (hit == null) hit = new HitParticles();
        if (cross == null) cross = new Crosshair();
    }

    public static KvConfig load() {
        if (Files.exists(PATH)) {
            try (Reader r = Files.newBufferedReader(PATH)) {
                KvConfig c = GSON.fromJson(r, KvConfig.class);
                if (c != null) {
                    c.fixup();
                    return c;
                }
            } catch (Exception e) {
                System.err.println("[KingVisuals] Nie udało się wczytać configu: " + e);
            }
        }
        KvConfig c = new KvConfig();
        c.save();
        return c;
    }

    public void save() {
        try (Writer w = Files.newBufferedWriter(PATH)) {
            GSON.toJson(this, w);
        } catch (Exception e) {
            System.err.println("[KingVisuals] Nie udało się zapisać configu: " + e);
        }
    }
}
