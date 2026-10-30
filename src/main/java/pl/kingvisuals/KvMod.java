package pl.kingvisuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KvMod implements ClientModInitializer {
    public static KvConfig CONFIG = new KvConfig();
    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        CONFIG = KvConfig.load();

        // Domyślnie prawy Shift - można zmienić w Opcje > Sterowanie
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.kingvisuals.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.kingvisuals"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                client.setScreen(new KvScreen(client.currentScreen));
            }
        });

        HudRenderCallback.EVENT.register((ctx, tick) -> {
            Overlay.render(ctx);
            MinecraftClient mc = MinecraftClient.getInstance();
            if (CONFIG != null) CrosshairFx.render(ctx, mc, CONFIG);
        });

        Target.init();
        Visuals3D.init();
        TargetEsp.init();
        HitFx.init();
    }
}
