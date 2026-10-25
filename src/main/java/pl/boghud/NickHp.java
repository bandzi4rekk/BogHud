package pl.boghud;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Locale;

/** Pokazuje HP innych graczy nad ich nickami (napis w świecie, zawsze zwrócony do kamery). */
public final class NickHp {
    private NickHp() {}

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(NickHp::render);
    }

    private static void render(WorldRenderContext ctx) {
        HudConfig c = HudMod.CONFIG;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || c.nickHp == null || !c.nickHp.enabled || mc.world == null || mc.player == null) return;

        MatrixStack ms = ctx.matrixStack();
        VertexConsumerProvider vp = ctx.consumers();
        if (ms == null || vp == null) return;

        float delta = mc.getRenderTickCounter().getTickDelta(true);
        Vec3d cam = ctx.camera().getPos();
        TextRenderer tr = mc.textRenderer;
        double range = c.nickHp.range;

        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || p.isSpectator() || !p.isAlive()) continue;
            if (mc.player.squaredDistanceTo(p) > range * range) continue;

            float hp = p.getHealth() + p.getAbsorptionAmount();
            float max = Math.max(1f, p.getMaxHealth());
            float frac = p.getHealth() / max;
            Formatting color = frac >= 0.7f ? Formatting.GREEN : (frac >= 0.35f ? Formatting.YELLOW : Formatting.RED);
            Text text = Text.literal(String.format(Locale.ROOT, "%.1f HP", hp)).formatted(color);

            double x = MathHelper.lerp(delta, p.prevX, p.getX()) - cam.x;
            double y = MathHelper.lerp(delta, p.prevY, p.getY()) - cam.y + p.getHeight() + 0.8;
            double z = MathHelper.lerp(delta, p.prevZ, p.getZ()) - cam.z;

            ms.push();
            ms.translate(x, y, z);
            ms.multiply(ctx.camera().getRotation());
            ms.scale(-0.025f, -0.025f, 0.025f);
            Matrix4f mat = ms.peek().getPositionMatrix();
            float tx = -tr.getWidth(text) / 2f;
            tr.draw(text, tx, 0f, 0xFFFFFFFF, false, mat, vp, TextRenderer.TextLayerType.NORMAL, 0x40000000, 0xF000F0);
            ms.pop();
        }
    }
}
