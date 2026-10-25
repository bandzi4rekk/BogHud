package pl.boghud;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;

/** Przekształcenia przedmiotu w ręce (używane przez mixin HeldItemRendererMixin). */
public final class ViewModel {
    private ViewModel() {}

    public static void apply(MatrixStack m, AbstractClientPlayerEntity player, Hand hand) {
        HudConfig c = HudMod.CONFIG;
        if (c == null || c.viewModel == null || !c.viewModel.enabled) return;

        boolean main = hand == Hand.MAIN_HAND;
        HudConfig.HandCfg h = main ? c.viewModel.main : c.viewModel.off;
        Arm arm = main ? player.getMainArm() : player.getMainArm().getOpposite();
        float side = arm == Arm.RIGHT ? 1f : -1f;

        // przesunięcie (setne części bloku): X w prawo, Y w górę, Z do tyłu
        m.translate(h.x / 100f, h.y / 100f, h.z / 100f);

        // obrót i skala wokół przybliżonego miejsca przedmiotu w ręce
        float px = 0.56f * side, py = -0.52f, pz = -0.72f;
        m.translate(px, py, pz);
        if (h.rotX != 0) m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(h.rotX));
        if (h.rotY != 0) m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(h.rotY));
        if (h.rotZ != 0) m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(h.rotZ));
        float s = Math.max(0.1f, h.scale / 100f);
        m.scale(s, s, s);
        m.translate(-px, -py, -pz);
    }
}
