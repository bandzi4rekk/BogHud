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

    /** Jeden element HUD (Watermark, FPS albo Ping) z własną pozycją i skalą. */
    public static class HudItem {
        public boolean enabled = true;
        /** Odległość od prawej krawędzi i od góry ekranu (element jest wyrównany do prawej). */
        public int offX = 4;
        public int offY = 4;
        public int scalePercent = 100;

        HudItem() {}

        HudItem(int offY) {
            this.offY = offY;
        }
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

    /**
     * Target ESP: efekt wokół celu. Tryby: 0 Ghosts, 1 Circle, 2 Marker, 3 Spiral,
     * 4 Corners, 5 Chevrons, 6 Spinner, 7 Scifi.
     */
    public static class TargetEsp {
        public boolean enabled = true;
        public int mode = 4;
        public int speedPercent = 100;
        public int sizePercent = 85;
        public int glowPercent = 70;
        public boolean themeColor = true;
        public int color = 0x8A4DFF;
        public boolean hurtFlash = true;
        public boolean showOnLook = true;
        public int range = 96;
    }

    public HudItem watermark = new HudItem(4);
    public HudItem fps = new HudItem(14);
    public HudItem ping = new HudItem(24);
    public JumpCircles jump = new JumpCircles();
    public ChinaHat hat = new ChinaHat();
    public HitParticles hit = new HitParticles();
    public Crosshair cross = new Crosshair();
    public TargetEsp esp = new TargetEsp();

    public HudItem item(int i) {
        return i == 0 ? watermark : (i == 1 ? fps : ping);
    }

    public void resetHud() {
        watermark.offX = 4;
        watermark.offY = 4;
        fps.offX = 4;
        fps.offY = 14;
        ping.offX = 4;
        ping.offY = 24;
    }

    public void fixup() {
        if (watermark == null) watermark = new HudItem(4);
        if (fps == null) fps = new HudItem(14);
        if (ping == null) ping = new HudItem(24);
        if (jump == null) jump = new JumpCircles();
        if (hat == null) hat = new ChinaHat();
        if (hit == null) hit = new HitParticles();
        if (cross == null) cross = new Crosshair();
        if (esp == null) esp = new TargetEsp();
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
