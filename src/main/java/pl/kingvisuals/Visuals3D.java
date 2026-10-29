package pl.kingvisuals;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Efekty 3D: kółka przy skoku (Jump Circles) i stożkowy kapelusz (China Hat). */
public final class Visuals3D {
    /** {x, y, z, czas startu w ms} */
    private static final List<double[]> CIRCLES = new ArrayList<>();
    private static boolean wasOnGround = true;

    private Visuals3D() {}

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(Visuals3D::render);
    }

    private static void render(WorldRenderContext ctx) {
        KvConfig c = KvMod.CONFIG;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || mc.player == null || mc.world == null) return;

        MatrixStack ms = ctx.matrixStack();
        VertexConsumerProvider consumers = ctx.consumers();
        if (ms == null || consumers == null) return;

        long now = System.currentTimeMillis();
        float delta = mc.getRenderTickCounter().getTickDelta(true);
        Vec3d cam = ctx.camera().getPos();

        // wykrywanie skoku: byliśmy na ziemi, teraz lecimy w górę
        boolean onGround = mc.player.isOnGround();
        if (c.jump.enabled && wasOnGround && !onGround && mc.player.getVelocity().y > 0.1 && CIRCLES.size() < 8) {
            CIRCLES.add(new double[]{mc.player.getX(), mc.player.getY(), mc.player.getZ(), now});
        }
        wasOnGround = onGround;

        VertexConsumer vc = consumers.getBuffer(RenderLayer.getDebugQuads());
        Matrix4f m = ms.peek().getPositionMatrix();

        if (c.jump.enabled) {
            int col = c.jump.color;
            int rr = (col >> 16) & 255, gg = (col >> 8) & 255, bb = col & 255;
            double maxR = 1.6 * c.jump.radiusPercent / 100.0;
            Iterator<double[]> it = CIRCLES.iterator();
            while (it.hasNext()) {
                double[] ci = it.next();
                double t = (now - ci[3]) / (double) Math.max(100, c.jump.durationMs);
                if (t >= 1) {
                    it.remove();
                    continue;
                }
                double ease = 1 - Math.pow(1 - t, 3);
                double r = 0.3 + (maxR - 0.3) * ease;
                int a = Math.max(0, Math.min(255, (int) (c.jump.alpha * (1 - t))));
                Draw3D.flatRing(vc, m, ci[0] - cam.x, ci[1] - cam.y + 0.02, ci[2] - cam.z,
                        Math.max(0.05, r - 0.06), r + 0.06, rr, gg, bb, a);
            }
        } else {
            CIRCLES.clear();
        }

        if (c.hat.enabled) {
            if (ctx.camera().isThirdPerson()) {
                hat(vc, m, mc.player, delta, cam, c.hat);
            }
            if (c.hat.showOthers) {
                for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
                    if (p == mc.player || p.isSpectator() || !p.isAlive()) continue;
                    if (mc.player.squaredDistanceTo(p) > 48 * 48) continue;
                    hat(vc, m, p, delta, cam, c.hat);
                }
            }
        }
    }

    private static void hat(VertexConsumer vc, Matrix4f m, Entity e, float delta, Vec3d cam, KvConfig.ChinaHat h) {
        double x = MathHelper.lerp(delta, e.prevX, e.getX()) - cam.x;
        double y = MathHelper.lerp(delta, e.prevY, e.getY()) - cam.y;
        double z = MathHelper.lerp(delta, e.prevZ, e.getZ()) - cam.z;
        double size = h.sizePercent / 100.0;
        int col = h.color;
        int a = Math.max(0, Math.min(255, h.alpha));
        Draw3D.cone(vc, m, x, y + e.getHeight() - 0.02, z, 0.55 * size, 0.35 * size,
                (col >> 16) & 255, (col >> 8) & 255, col & 255, a);
    }
}
