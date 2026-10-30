package pl.kingvisuals;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Target ESP: efekt wokół celu (tryby w EspShapes), z warstwą glow i mignięciem na czerwono po trafieniu. */
public final class TargetEsp {
    private TargetEsp() {}

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(TargetEsp::render);
    }

    private static void render(WorldRenderContext ctx) {
        KvConfig c = KvMod.CONFIG;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || c.esp == null || !c.esp.enabled || mc.player == null || mc.world == null) return;

        LivingEntity t = Target.get(c.esp.showOnLook, c.esp.range);
        if (t == null) return;

        MatrixStack ms = ctx.matrixStack();
        VertexConsumerProvider consumers = ctx.consumers();
        if (ms == null || consumers == null) return;

        float delta = mc.getRenderTickCounter().getTickDelta(true);
        Vec3d cam = ctx.camera().getPos();
        double x = MathHelper.lerp(delta, t.prevX, t.getX()) - cam.x;
        double y = MathHelper.lerp(delta, t.prevY, t.getY()) - cam.y;
        double z = MathHelper.lerp(delta, t.prevZ, t.getZ()) - cam.z;

        double time = (System.currentTimeMillis() % 1000000L) / 1000.0 * (c.esp.speedPercent / 100.0);
        double k = c.esp.sizePercent / 100.0;
        double glow = c.esp.glowPercent / 100.0;

        int base = c.esp.themeColor
                ? Ui.mix(Ui.PURPLE, Ui.BLUE, 0.5 + 0.5 * Math.sin(time * 1.5))
                : c.esp.color;
        if (c.esp.hurtFlash && t.hurtTime > 0) {
            base = Ui.mix(base, 0xFF3040, Math.min(1.0, t.hurtTime / 6.0));
        }

        VertexConsumer vc = consumers.getBuffer(RenderLayer.getDebugQuads());
        Matrix4f m = ms.peek().getPositionMatrix();
        EspShapes.draw(vc, m, x, y, z, t.getWidth(), t.getHeight(), c.esp.mode, time, k, base, 210, 1.0);
        if (glow > 0.02) {
            EspShapes.draw(vc, m, x, y, z, t.getWidth(), t.getHeight(), c.esp.mode, time, k, base,
                    (int) (210 * glow * 0.35), 2.6);
        }
    }
}
