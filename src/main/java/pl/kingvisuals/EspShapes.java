package pl.kingvisuals;

import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;

/** Kształty Target ESP: Ghosts, Circle, Marker, Spiral, Corners, Chevrons, Spinner, Scifi. */
public final class EspShapes {
    public static final String[] MODES = {"Ghosts", "Circle", "Marker", "Spiral", "Corners", "Chevrons", "Spinner", "Scifi"};
    private static final double TAU = Math.PI * 2;

    private EspShapes() {}

    private static final class C {
        VertexConsumer vc;
        Matrix4f m;
        double cx, cy, cz, R, H, w, t, k, thick;
        int r, g, b, a;
    }

    /**
     * Rysuje wybrany tryb wokół celu. (cx, cy, cz) to stopy celu względem kamery,
     * w szerokość, h wysokość, t czas w sekundach (po przeskalowaniu szybkością),
     * k rozmiar markerów, thick mnożnik grubości (większy dla warstwy glow).
     */
    public static void draw(VertexConsumer vc, Matrix4f m, double cx, double cy, double cz, double w, double h,
                            int mode, double t, double k, int rgb, int alpha, double thick) {
        C c = new C();
        c.vc = vc;
        c.m = m;
        c.cx = cx;
        c.cy = cy;
        c.cz = cz;
        c.w = w;
        c.H = h;
        c.R = Math.max(0.45, w * 0.85);
        c.t = t;
        c.k = k;
        c.thick = thick;
        c.r = (rgb >> 16) & 255;
        c.g = (rgb >> 8) & 255;
        c.b = rgb & 255;
        c.a = Math.max(0, Math.min(255, alpha));
        switch (mode) {
            case 0:
                ghosts(c);
                break;
            case 1:
                circle(c);
                break;
            case 2:
                marker(c);
                break;
            case 3:
                spiral(c);
                break;
            case 5:
                chevrons(c);
                break;
            case 6:
                spinner(c);
                break;
            case 7:
                scifi(c);
                break;
            default:
                corners(c);
                break;
        }
    }

    // ------------------------------------------------------------------ narzędzia

    private static void box(C c, double x0, double y0, double z0, double x1, double y1, double z1, int a) {
        Draw3D.box(c.vc, c.m, x0, y0, z0, x1, y1, z1, c.r, c.g, c.b, Math.max(0, Math.min(255, a)));
    }

    /** Cienki prostopadłościan wzdłuż odcinka (dx, dy, dz) od punktu (px, py, pz). */
    private static void edge(C c, double px, double py, double pz, double dx, double dy, double dz, double th, int a) {
        box(c, Math.min(px, px + dx) - th, Math.min(py, py + dy) - th, Math.min(pz, pz + dz) - th,
                Math.max(px, px + dx) + th, Math.max(py, py + dy) + th, Math.max(pz, pz + dz) + th, a);
    }

    private static void tri(C c, double x1, double y1, double z1, double x2, double y2, double z2,
                            double x3, double y3, double z3, int a) {
        Draw3D.quad(c.vc, c.m, x1, y1, z1, x2, y2, z2, x3, y3, z3, x3, y3, z3,
                c.r, c.g, c.b, Math.max(0, Math.min(255, a)));
    }

    /** Łuk (albo cały pierścień) o promieniu r na wysokości y: pionowa obręcz + płaska obręcz z góry. */
    private static void arc(C c, double y, double r, double bandH, double w, double a0, double a1, int segs, int a) {
        double bh = bandH * c.thick;
        double ww = w * c.thick;
        for (int i = 0; i < segs; i++) {
            double s0 = a0 + (a1 - a0) * i / segs;
            double s1 = a0 + (a1 - a0) * (i + 1) / segs;
            double c0 = Math.cos(s0), n0 = Math.sin(s0), c1 = Math.cos(s1), n1 = Math.sin(s1);
            Draw3D.quad(c.vc, c.m,
                    c.cx + r * c0, y, c.cz + r * n0,
                    c.cx + r * c1, y, c.cz + r * n1,
                    c.cx + r * c1, y + bh, c.cz + r * n1,
                    c.cx + r * c0, y + bh, c.cz + r * n0,
                    c.r, c.g, c.b, a);
            Draw3D.quad(c.vc, c.m,
                    c.cx + (r - ww) * c0, y + bh, c.cz + (r - ww) * n0,
                    c.cx + (r + ww) * c0, y + bh, c.cz + (r + ww) * n0,
                    c.cx + (r + ww) * c1, y + bh, c.cz + (r + ww) * n1,
                    c.cx + (r - ww) * c1, y + bh, c.cz + (r - ww) * n1,
                    c.r, c.g, c.b, a);
        }
    }

    // ------------------------------------------------------------------ tryby

    /** Trzy duchy krążące wokół celu ze smugą znikających kostek. */
    private static void ghosts(C c) {
        for (int gi = 0; gi < 3; gi++) {
            double phase = c.t * 1.6 + gi * (TAU / 3);
            for (int tr = 0; tr < 7; tr++) {
                double p = phase - tr * 0.18;
                double hy = c.H * (0.5 + 0.4 * Math.sin(p * 1.3 + gi));
                double px = c.cx + c.R * Math.cos(p);
                double pz = c.cz + c.R * Math.sin(p);
                double s = 0.09 * c.k * c.thick * (1 - tr * 0.08);
                int al = (int) (c.a * (1 - tr / 7.0));
                box(c, px - s, c.cy + hy - s, pz - s, px + s, c.cy + hy + s, pz + s, al);
            }
        }
    }

    /** Pulsujący pierścień przy ziemi z mniejszym pierścieniem w środku. */
    private static void circle(C c) {
        double rad = c.R * (1 + 0.04 * Math.sin(c.t * 2));
        arc(c, c.cy + 0.02, rad, 0.06, 0.04, 0, TAU, 48, c.a);
        arc(c, c.cy + 0.04, rad * 0.78, 0.03, 0.025, 0, TAU, 40, (int) (c.a * 0.6));
    }

    /** Obracający się romb nad głową celu. */
    private static void marker(C c) {
        double hy = c.H + 0.55 + 0.1 * Math.sin(c.t * 2.2);
        double s = 0.22 * c.k * c.thick;
        double rot = c.t * 2;
        double top = c.cy + hy + s * 1.3;
        double bot = c.cy + hy - s * 1.3;
        double mid = c.cy + hy;
        for (int i = 0; i < 4; i++) {
            double a0 = rot + i * Math.PI / 2;
            double a1 = rot + (i + 1) * Math.PI / 2;
            double x0 = c.cx + s * Math.cos(a0), z0 = c.cz + s * Math.sin(a0);
            double x1 = c.cx + s * Math.cos(a1), z1 = c.cz + s * Math.sin(a1);
            tri(c, c.cx, top, c.cz, x0, mid, z0, x1, mid, z1, c.a);
            tri(c, c.cx, bot, c.cz, x0, mid, z0, x1, mid, z1, (int) (c.a * 0.8));
        }
    }

    /** Dwie spirale kropek wijące się wokół celu. */
    private static void spiral(C c) {
        int pts = 36;
        for (int strand = 0; strand < 2; strand++) {
            for (int i = 0; i < pts; i++) {
                double f = i / (double) pts;
                double u = (f + c.t * 0.15) % 1.0;
                double ang = f * Math.PI * 6 + c.t * 2 + strand * Math.PI;
                double px = c.cx + c.R * 0.9 * Math.cos(ang);
                double pz = c.cz + c.R * 0.9 * Math.sin(ang);
                double py = c.cy + u * c.H;
                double s = 0.045 * c.k * c.thick;
                int al = (int) (c.a * Math.sin(u * Math.PI));
                box(c, px - s, py - s, pz - s, px + s, py + s, pz + s, al);
            }
        }
    }

    /** Narożniki ramki wokół celu (jak w ESP). */
    private static void corners(C c) {
        double hw = c.w / 2 + 0.12;
        double len = 0.28 * c.k;
        double th = 0.022 * c.thick;
        int al = (int) (c.a * (0.75 + 0.25 * Math.sin(c.t * 3)));
        double[] xs = {c.cx - hw, c.cx + hw};
        double[] zs = {c.cz - hw, c.cz + hw};
        double[] ys = {c.cy - 0.05, c.cy + c.H + 0.05};
        for (int ix = 0; ix < 2; ix++) {
            for (int iz = 0; iz < 2; iz++) {
                for (int iy = 0; iy < 2; iy++) {
                    double px = xs[ix], pz = zs[iz], py = ys[iy];
                    double dx = ix == 0 ? len : -len;
                    double dz = iz == 0 ? len : -len;
                    double dy = iy == 0 ? len : -len;
                    edge(c, px, py, pz, dx, 0, 0, th, al);
                    edge(c, px, py, pz, 0, dy, 0, th, al);
                    edge(c, px, py, pz, 0, 0, dz, th, al);
                }
            }
        }
    }

    /** Cztery strzałki (chevrony) skierowane w dół, jeżdżące góra-dół wokół celu. */
    private static void chevrons(C c) {
        for (int s = 0; s < 4; s++) {
            double ang = s * Math.PI / 2 + c.t * 0.5;
            double hy = c.H * (0.15 + 0.7 * (0.5 + 0.5 * Math.sin(c.t * 2.5 + s * 1.2)));
            double rad = c.R + 0.1;
            double nx = Math.cos(ang), nz = Math.sin(ang);
            double tx = -nz, tz = nx;
            double half = 0.22 * c.k * c.thick;
            double hh = 0.16 * c.k;
            double th = 0.035 * c.thick;
            double bx = c.cx + nx * rad, bz = c.cz + nz * rad;
            double lx = bx - tx * half, lz = bz - tz * half;
            double rx = bx + tx * half, rz = bz + tz * half;
            double y0 = c.cy + hy;
            Draw3D.quad(c.vc, c.m, lx, y0 + hh, lz, bx, y0, bz, bx, y0 + th, bz, lx, y0 + hh + th, lz,
                    c.r, c.g, c.b, c.a);
            Draw3D.quad(c.vc, c.m, bx, y0, bz, rx, y0 + hh, rz, rx, y0 + hh + th, rz, bx, y0 + th, bz,
                    c.r, c.g, c.b, c.a);
        }
    }

    /** Obracające się łuki na dwóch wysokościach, w przeciwne strony. */
    private static void spinner(C c) {
        double rot = c.t * 2.5;
        for (int i = 0; i < 3; i++) {
            double a0 = rot + i * TAU / 3;
            arc(c, c.cy + c.H * 0.55, c.R + 0.05, 0.05, 0.04, a0, a0 + 1.0, 14, c.a);
            double b0 = -rot * 1.3 + i * TAU / 3;
            arc(c, c.cy + c.H * 0.2, c.R, 0.05, 0.04, b0, b0 + 0.8, 12, (int) (c.a * 0.8));
        }
    }

    /** Skaner: pierścień przesuwający się góra-dół, zewnętrzny pierścień z ząbkami i pionowe wiązki. */
    private static void scifi(C c) {
        double u = 0.5 + 0.5 * Math.sin(c.t * 1.8);
        arc(c, c.cy + u * c.H, c.R, 0.05, 0.05, 0, TAU, 48, c.a);
        arc(c, c.cy + 0.02, c.R + 0.25, 0.03, 0.025, 0, TAU, 48, (int) (c.a * 0.7));
        for (int i = 0; i < 24; i++) {
            double ang = i * TAU / 24 + c.t * 0.8;
            double cs = Math.cos(ang), sn = Math.sin(ang);
            edge(c, c.cx + (c.R + 0.18) * cs, c.cy + 0.02, c.cz + (c.R + 0.18) * sn,
                    0.07 * cs, 0, 0.07 * sn, 0.012 * c.thick, (int) (c.a * 0.8));
        }
        for (int i = 0; i < 4; i++) {
            double ang = i * TAU / 4 + c.t * 0.6;
            edge(c, c.cx + c.R * Math.cos(ang), c.cy, c.cz + c.R * Math.sin(ang),
                    0, c.H, 0, 0.01 * c.thick, (int) (c.a * 0.4));
        }
    }
}
