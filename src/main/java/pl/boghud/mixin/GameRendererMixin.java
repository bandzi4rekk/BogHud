package pl.boghud.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.Window;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.boghud.HudConfig;
import pl.boghud.HudMod;

/** Zmiana proporcji obrazu (rozciągnięty ekran): skalujemy poziomo macierz projekcji świata. */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getBasicProjectionMatrix", at = @At("RETURN"), cancellable = true)
    private void boghud$aspect(CallbackInfoReturnable<Matrix4f> cir) {
        HudConfig c = HudMod.CONFIG;
        if (c == null || c.aspect == null || !c.aspect.enabled) return;

        Window w = MinecraftClient.getInstance().getWindow();
        if (w.getFramebufferHeight() <= 0) return;
        float current = (float) w.getFramebufferWidth() / (float) w.getFramebufferHeight();
        float target = c.aspect.ratioPercent / 100f;
        if (target < 0.3f) return;

        Matrix4f m = cir.getReturnValue();
        cir.setReturnValue(m.scale(current / target, 1f, 1f));
    }
}
