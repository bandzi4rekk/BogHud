package pl.kingvisuals;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;

/** Wybór celu dla efektów: stwór, na którego patrzysz, albo ostatni uderzony (zostaje na 3 sekundy). */
public final class Target {
    private static LivingEntity last;
    private static long lastTime;

    private Target() {}

    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient && entity instanceof LivingEntity living) {
                last = living;
                lastTime = System.currentTimeMillis();
            }
            return ActionResult.PASS;
        });
    }

    /** Stwór najbliżej środka celownika w zasięgu (kąt zależy od jego rozmiaru i odległości). */
    public static LivingEntity lookedAt(double range) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return null;
        Vec3d eye = mc.player.getCameraPosVec(1f);
        Vec3d look = mc.player.getRotationVec(1f);
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le) || e == mc.player || !le.isAlive()) continue;
            Vec3d center = new Vec3d(e.getX(), e.getY() + e.getHeight() / 2.0, e.getZ());
            Vec3d to = center.subtract(eye);
            double d = to.length();
            if (d < 0.1 || d > range) continue;
            double dot = to.normalize().dotProduct(look);
            double angle = Math.acos(Math.max(-1.0, Math.min(1.0, dot)));
            double tolerance = Math.atan((Math.max(e.getWidth() / 2.0, e.getHeight() / 2.0) + 0.1) / d);
            if (angle <= tolerance && angle < bestScore) {
                bestScore = angle;
                best = le;
            }
        }
        return best;
    }

    /**
     * Aktualny cel. Gdy onlyLook = true, tylko stwór, na którego patrzysz;
     * w przeciwnym razie także ostatnio uderzony przez 3 sekundy.
     */
    public static LivingEntity get(boolean onlyLook, double range) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return null;
        long now = System.currentTimeMillis();

        LivingEntity looked = lookedAt(range);
        if (looked != null) {
            last = looked;
            lastTime = now;
            return looked;
        }
        if (onlyLook) return null;
        if (last != null && last.isAlive() && !last.isRemoved() && now - lastTime < 3000
                && mc.player.squaredDistanceTo(last) < range * range) {
            return last;
        }
        return null;
    }
}
