package pl.kingvisuals;

import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;

/** Proste rysowanie czworokątów w świecie 3D (używane przez efekty wizualne). */
public final class Draw3D {
    private Draw3D() {}

    /** Czworokąt rysowany z obu stron (widoczny niezależnie od odcinania ścian). */
    public static void quad(VertexConsumer vc, Matrix4f m,
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

    /** Płaski pierścień (między promieniem wewnętrznym a zewnętrznym) na wysokości cy. */
    public static void flatRing(VertexConsumer vc, Matrix4f m, double cx, double cy, double cz,
                                double rIn, double rOut, int r, int g, int b, int a) {
        int seg = 48;
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg;
            double a1 = Math.PI * 2 * (i + 1) / seg;
            double c0 = Math.cos(a0), s0 = Math.sin(a0);
            double c1 = Math.cos(a1), s1 = Math.sin(a1);
            quad(vc, m,
                    cx + rIn * c0, cy, cz + rIn * s0,
                    cx + rOut * c0, cy, cz + rOut * s0,
                    cx + rOut * c1, cy, cz + rOut * s1,
                    cx + rIn * c1, cy, cz + rIn * s1,
                    r, g, b, a);
        }
    }

    /** Stożek (np. kapelusz): podstawa o promieniu radius na wysokości cy, czubek wyżej o h. */
    public static void cone(VertexConsumer vc, Matrix4f m, double cx, double cy, double cz,
                            double radius, double h, int r, int g, int b, int a) {
        int seg = 40;
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg;
            double a1 = Math.PI * 2 * (i + 1) / seg;
            int alpha = (i % 2 == 0) ? a : (int) (a * 0.8);
            quad(vc, m,
                    cx + radius * Math.cos(a0), cy, cz + radius * Math.sin(a0),
                    cx + radius * Math.cos(a1), cy, cz + radius * Math.sin(a1),
                    cx, cy + h, cz,
                    cx, cy + h, cz,
                    r, g, b, alpha);
        }
    }
}
