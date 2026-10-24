package pl.boghud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class HudMod implements ClientModInitializer {
    public static HudConfig CONFIG = new HudConfig();
    private static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        CONFIG = HudConfig.load();

        // Domyślnie prawy Shift - można zmienić w Opcje > Sterowanie
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.boghud.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.boghud"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                client.setScreen(new HudConfigScreen(client.currentScreen));
            }
        });

        HudRenderCallback.EVENT.register(HudRenderer::render);
    }
}
