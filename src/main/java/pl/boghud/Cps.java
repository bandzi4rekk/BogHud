package pl.boghud;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;

/** Licznik kliknięć na sekundę (LPM i PPM). Sprawdzany co klatkę, żeby nie gubić szybkich kliknięć. */
public final class Cps {
    private static final ArrayDeque<Long> LEFT = new ArrayDeque<>();
    private static final ArrayDeque<Long> RIGHT = new ArrayDeque<>();
    private static boolean lastLeft, lastRight;

    private Cps() {}

    public static void poll(MinecraftClient mc) {
        if (mc.getWindow() == null) return;
        long now = System.currentTimeMillis();
        long handle = mc.getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        // liczymy tylko kliknięcia w grze, nie w menu
        if (mc.currentScreen == null) {
            if (l && !lastLeft) LEFT.addLast(now);
            if (r && !lastRight) RIGHT.addLast(now);
        }
        lastLeft = l;
        lastRight = r;

        trim(LEFT, now);
        trim(RIGHT, now);
    }

    private static void trim(ArrayDeque<Long> q, long now) {
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
    }

    public static int left() {
        return LEFT.size();
    }

    public static int right() {
        return RIGHT.size();
    }
}
