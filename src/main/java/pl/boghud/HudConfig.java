package pl.boghud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Ustawienia HUD-a, zapisywane w config/boghud.json */
public class HudConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("boghud.json");

    public boolean enabled = true;
    public boolean showFps = true;
    public boolean showPing = true;
    public boolean showCoords = true;
    public boolean showArmor = true;
    public boolean showText = true;
    public boolean shadow = true;
    public boolean background = true;

    public String customText = "Bog HUD";

    public int x = 4;
    public int y = 4;
    public int scalePercent = 100;

    /** Kolory w formacie RGB (0xRRGGBB) */
    public int textColor = 0xFFFFFF;
    public int accentColor = 0x55FFFF;
    /** Przezroczystość tła 0-255 */
    public int backgroundAlpha = 120;

    public static HudConfig load() {
        if (Files.exists(PATH)) {
            try (Reader r = Files.newBufferedReader(PATH)) {
                HudConfig c = GSON.fromJson(r, HudConfig.class);
                if (c != null) {
                    if (c.customText == null) c.customText = "";
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
