package pl.kingvisuals;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.ActionResult;

/** Cząsteczki przy trafieniu stwora. */
public final class HitFx {
    private HitFx() {}

    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            KvConfig c = KvMod.CONFIG;
            if (world.isClient && c != null && c.hit != null && c.hit.enabled
                    && entity instanceof LivingEntity living && living != player) {
                ParticleEffect type;
                switch (c.hit.type) {
                    case 1:
                        type = ParticleTypes.ENCHANTED_HIT;
                        break;
                    case 2:
                        type = ParticleTypes.FLAME;
                        break;
                    case 3:
                        type = ParticleTypes.END_ROD;
                        break;
                    default:
                        type = ParticleTypes.CRIT;
                        break;
                }
                for (int i = 0; i < c.hit.count; i++) {
                    double px = living.getX() + (Math.random() - 0.5) * living.getWidth();
                    double py = living.getY() + Math.random() * living.getHeight();
                    double pz = living.getZ() + (Math.random() - 0.5) * living.getWidth();
                    world.addParticle(type, px, py, pz,
                            (Math.random() - 0.5) * 0.4, Math.random() * 0.3, (Math.random() - 0.5) * 0.4);
                }
            }
            return ActionResult.PASS;
        });
    }
}
