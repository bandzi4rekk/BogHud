package pl.boghud;

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

/** Efekt 3D: świecący pierścień wokół celu (z ruchem góra-dół i smugą). */
public final class TargetRing {
    private TargetRing() {}

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(TargetRing::render);
    }

    private static void render(WorldRenderContext ctx) {
        HudConfig c = HudMod.CONFIG;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || !c.enabled || !c.ring.enabled || mc.player == null) return;

        LivingEntity t = Target.get();
        if (t == null || t == mc.player) return;

        MatrixStack matrices = ctx.matrixStack();
        VertexConsumerProvider consumers = ctx.consumers();
        if (matrices == null || consumers == null) return;

        float delta = mc.getRenderTickCounter().getTickDelta(true);
        Vec3d cam = ctx.camera().getPos();
        double x = MathHelper.lerp(delta, t.prevX, t.getX()) - cam.x;
        double y = MathHelper.lerp(delta, t.prevY, t.getY()) - cam.y;
        double z = MathHelper.lerp(delta, t.prevZ, t.getZ()) - cam.z;

        double radius = Math.max(0.45, t.getWidth() * 0.85) * c.ring.radiusPercent / 100.0;
        double height = t.getHeight();

        int col = c.ring.color;
        int rr = (col >> 16) & 255, gg = (col >> 8) & 255, bb = col & 255;
        int alpha = Math.max(0, Math.min(255, c.ring.alpha));

        VertexConsumer vc = consumers.getBuffer(RenderLayer.getDebugQuads());
        Matrix4f m = matrices.peek().getPositionMatrix();

        if (c.ring.animate) {
            double phase = Math.sin(System.currentTimeMillis() / 400.0) * 0.5 + 0.5;
            double ringY = phase * height;
            // główny pierścień + smuga za nim
            for (int k = 0; k < 4; k++) {
                double yy = Math.max(0.02, ringY - k * 0.12);
                int a = (int) (alpha * (1.0 - k * 0.28));
                ring(vc, m, x, y + yy, z, radius, rr, gg, bb, a);
            }
        } else {
            ring(vc, m, x, y + 0.02, z, radius, rr, gg, bb, alpha);
        }
    }

    private static void ring(VertexConsumer vc, Matrix4f m, double cx, double cy, double cz, double r,
                             int rr, int gg, int bb, int aa) {
        int seg = 48;
        double band = 0.05;
        double w = 0.035;
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg;
            double a1 = Math.PI * 2 * (i + 1) / seg;
            double c0 = Math.cos(a0), s0 = Math.sin(a0);
            double c1 = Math.cos(a1), s1 = Math.sin(a1);

            // pionowa obręcz
            quad(vc, m,
                    cx + r * c0, cy, cz + r * s0,
                    cx + r * c1, cy, cz + r * s1,
                    cx + r * c1, cy + band, cz + r * s1,
                    cx + r * c0, cy + band, cz + r * s0,
                    rr, gg, bb, aa);
            // pozioma obręcz (widoczna z góry)
            quad(vc, m,
                    cx + (r - w) * c0, cy + band, cz + (r - w) * s0,
                    cx + (r + w) * c0, cy + band, cz + (r + w) * s0,
                    cx + (r + w) * c1, cy + band, cz + (r + w) * s1,
                    cx + (r - w) * c1, cy + band, cz + (r - w) * s1,
                    rr, gg, bb, aa);
        }
    }

    /** Czworokąt rysowany z obu stron (żeby był widoczny niezależnie od odcinania ścian). */
    private static void quad(VertexConsumer vc, Matrix4f m,
                             double x1, double y1, double z1, double x2, double y2, double z2,
                             double x3, double y3, double z3, double x4, double y4, double z4,
                             int r, int g, int b, int a) {
        v(vc, m, x1, y1, z1, r, g, b, a);
        v(vc, m, x2, y2, z2, r, g, b, a);
        v(vc, m, x3, y3, z3, r, g, b, a);
        v(vc, m, x4, y4, z4, r, g, b, a);
        v(vc, m, x4, y4, z4, r, g, b, a);
        v(vc, m, x3, y3, z3, r, g, b, a);
        v(vc, m, x2, y2, z2, r, g, b, a);
        v(vc, m, x1, y1, z1, r, g, b, a);
    }

    private static void v(VertexConsumer vc, Matrix4f m, double x, double y, double z, int r, int g, int b, int a) {
        vc.vertex(m, (float) x, (float) y, (float) z).color(r, g, b, a);
    }
}
