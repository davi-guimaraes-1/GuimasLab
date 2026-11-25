package biobuzz.sdk;

import com.qualcomm.robotcore.hardware.Gamepad;
import java.awt.event.KeyEvent;
import java.util.Set;

/** Teclado -> gamepad1. WASD: analogico esquerdo; Q/E: analogico direito (giro); setas: dpad; J K L I: A B X Y;
 *  U O: bumpers esquerdo/direito; ENTER: start. */
public final class GamepadMapper {
    private GamepadMapper() {}

    public static final String HELP = "WASD stick esq  Q/E stick dir  setas dpad  J K L I = A B X Y  U/O bumpers  ENTER start";

    public static void apply(Set<Integer> k, Gamepad g) {
        g.left_stick_x = axis(k, KeyEvent.VK_A, KeyEvent.VK_D);
        g.left_stick_y = axis(k, KeyEvent.VK_W, KeyEvent.VK_S);          // FTC: para frente e negativo
        g.right_stick_x = axis(k, KeyEvent.VK_Q, KeyEvent.VK_E);
        g.right_stick_y = 0;
        g.dpad_up = k.contains(KeyEvent.VK_UP);
        g.dpad_down = k.contains(KeyEvent.VK_DOWN);
        g.dpad_left = k.contains(KeyEvent.VK_LEFT);
        g.dpad_right = k.contains(KeyEvent.VK_RIGHT);
        g.a = k.contains(KeyEvent.VK_J);
        g.b = k.contains(KeyEvent.VK_K);
        g.x = k.contains(KeyEvent.VK_L);
        g.y = k.contains(KeyEvent.VK_I);
        g.left_bumper = k.contains(KeyEvent.VK_U);
        g.right_bumper = k.contains(KeyEvent.VK_O);
        g.start = k.contains(KeyEvent.VK_ENTER);
    }

    private static float dz(float v) { return Math.abs(v) < 0.05f ? 0f : v; }

    /** Controle fisico -> Gamepad do SDK (mesma convencao: para cima = negativo). */
    public static void apply(PadState p, Gamepad g) {
        g.left_stick_x = dz(p.lx); g.left_stick_y = dz(p.ly);
        g.right_stick_x = dz(p.rx); g.right_stick_y = dz(p.ry);
        g.left_trigger = p.lt; g.right_trigger = p.rt;
        g.a = p.a; g.b = p.b; g.x = p.x; g.y = p.y;
        g.left_bumper = p.lb; g.right_bumper = p.rb;
        g.back = p.back; g.start = p.start; g.guide = p.guide;
        g.left_stick_button = p.lstick; g.right_stick_button = p.rstick;
        g.dpad_up = p.up; g.dpad_down = p.down; g.dpad_left = p.left; g.dpad_right = p.right;
    }

    private static float axis(Set<Integer> k, int neg, int pos) {
        return (k.contains(pos) ? 1f : 0f) - (k.contains(neg) ? 1f : 0f);
    }
}
