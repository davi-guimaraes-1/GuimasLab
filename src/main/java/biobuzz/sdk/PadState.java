package biobuzz.sdk;

/** Estado de um controle (layout padrao estilo Xbox): sticks -1..1 (para cima = negativo), gatilhos 0..1. */
public final class PadState {
    public float lx, ly, rx, ry, lt, rt;
    public boolean a, b, x, y, lb, rb, back, start, guide, lstick, rstick, up, down, left, right;
}
