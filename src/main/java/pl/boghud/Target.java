package pl.boghud;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;

/**
 * Aktualny cel: stwór, na którego patrzysz, albo ostatni, którego uderzyłeś (zostaje na 3 sekundy).
 */
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

    /** Czy celownik wskazuje teraz jakiegoś żywego stwora. */
    public static boolean looking() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player != null && mc.targetedEntity instanceof LivingEntity;
    }

    public static LivingEntity get() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return null;
        long now = System.currentTimeMillis();

        Entity looked = mc.targetedEntity;
        if (looked instanceof LivingEntity living && living.isAlive() && living != mc.player) {
            last = living;
            lastTime = now;
            return living;
        }
        if (last != null && last.isAlive() && !last.isRemoved() && now - lastTime < 3000
                && mc.player.squaredDistanceTo(last) < 24 * 24) {
            return last;
        }
        return null;
    }
}
