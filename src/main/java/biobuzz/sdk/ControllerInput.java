package biobuzz.sdk;

import java.lang.reflect.Method;

/**
 * Controles ligados ao PC, lidos pela API de gamepad do GLFW (LWJGL). A biblioteca e carregada por reflexao: sem ela o
 * simulador continua funcionando so com o teclado. Usa o mapeamento padrao do GLFW (Xbox, PlayStation e similares).
 */
public final class ControllerInput {
    private ControllerInput() {}

    private static boolean tried, ok;
    private static String status = "controle: nao verificado";
    private static Method mIsGamepad, mGetState, mButtons, mAxes;
    private static Object state;
    private static final PadState[] pads = new PadState[2];
    private static final PadState[] injected = new PadState[2];   // teste: simula um controle

    private static void init() {
        tried = true;
        try {
            Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
            Class<?> st = Class.forName("org.lwjgl.glfw.GLFWGamepadState");
            if (!(Boolean) glfw.getMethod("glfwInit").invoke(null)) { status = "controle: GLFW nao iniciou"; return; }
            mIsGamepad = glfw.getMethod("glfwJoystickIsGamepad", int.class);
            mGetState = glfw.getMethod("glfwGetGamepadState", int.class, st);
            state = st.getMethod("create").invoke(null);
            mButtons = st.getMethod("buttons", int.class);
            mAxes = st.getMethod("axes", int.class);
            ok = true;
            status = "controle: procurando (conecte e aperte um botao)";
        } catch (ClassNotFoundException e) {
            status = "controle: biblioteca LWJGL ausente (so teclado)";
        } catch (Throwable t) {
            status = "controle: indisponivel (" + t.getClass().getSimpleName() + ")";
        }
    }

    /** Le os controles (chame uma vez por passo). */
    public static synchronized void poll() {
        if (!tried) init();
        pads[0] = injected[0]; pads[1] = injected[1];
        if (!ok) return;
        int found = 0;
        try {
            for (int jid = 0; jid < 16 && found < 2; jid++) {
                if (!(Boolean) mIsGamepad.invoke(null, jid)) continue;
                if (!(Boolean) mGetState.invoke(null, jid, state)) continue;
                PadState p = read();
                if (injected[found] == null) pads[found] = p;
                found++;
            }
            status = found == 0 ? "controle: nenhum encontrado (usando teclado)" : "controle: " + found + " conectado(s)";
        } catch (Throwable t) {
            ok = false;
            status = "controle: erro (" + t.getClass().getSimpleName() + ")";
        }
    }

    private static boolean btn(int i) throws Exception { return ((Byte) mButtons.invoke(state, i)) != 0; }

    private static float axis(int i) throws Exception { return (Float) mAxes.invoke(state, i); }

    private static PadState read() throws Exception {
        PadState p = new PadState();
        p.a = btn(0); p.b = btn(1); p.x = btn(2); p.y = btn(3); p.lb = btn(4); p.rb = btn(5);
        p.back = btn(6); p.start = btn(7); p.guide = btn(8); p.lstick = btn(9); p.rstick = btn(10);
        p.up = btn(11); p.right = btn(12); p.down = btn(13); p.left = btn(14);
        p.lx = axis(0); p.ly = axis(1); p.rx = axis(2); p.ry = axis(3);
        p.lt = (axis(4) + 1f) / 2f; p.rt = (axis(5) + 1f) / 2f;
        return p;
    }

    /** O controle indice 0 (gamepad1) ou 1 (gamepad2), ou null se nao houver. */
    public static synchronized PadState pad(int i) { return pads[i]; }

    public static synchronized String status() { return status; }

    /** Teste: simula um controle (null remove). */
    public static synchronized void inject(int i, PadState p) { injected[i] = p; }
}
