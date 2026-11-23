package biobuzz.sdk;

/**
 * Pinpoint simulado. Como no robo real, a origem e a pose onde o computador foi zerado (resetPosAndIMU),
 * X aponta para a frente do robo naquele instante, Y para a esquerda, e o heading cresce no sentido anti-horario.
 * As leituras so mudam quando o codigo chama update().
 */
public final class SimPinpoint {
    private SimPinpoint() {}

    private static volatile double[] world = new double[6];   // x, y, heading, vx, vy, omega no campo (publicado pelo simulador)
    private static volatile double[] cur = new double[6];     // ultimo update()
    private static volatile double x0, y0, h0, hOff;

    /** O simulador publica a pose real do robo a cada passo. */
    public static void publish(double x, double y, double h, double vx, double vy, double w) {
        world = new double[] {x, y, h, vx, vy, w};
    }

    /** Define a origem do Pinpoint na pose atual (ligar o robo). */
    public static void init() {
        double[] w = world;
        x0 = w[0]; y0 = w[1]; h0 = w[2]; hOff = 0; cur = w;
    }

    public static void update() { cur = world; }

    public static void reset() { init(); }

    public static void recalibrate() {}

    private static double wrap(double a) {
        while (a > Math.PI) a -= 2 * Math.PI;
        while (a <= -Math.PI) a += 2 * Math.PI;
        return a;
    }

    private static double dist(String unit, double inches) {
        switch (unit) {
            case "MM": return inches * 25.4;
            case "CM": return inches * 2.54;
            case "METER": return inches * 0.0254;
            default: return inches;
        }
    }

    private static double undist(String unit, double v) {
        switch (unit) {
            case "MM": return v / 25.4;
            case "CM": return v / 2.54;
            case "METER": return v / 0.0254;
            default: return v;
        }
    }

    private static double localX(double[] w) { return (w[0] - x0) * Math.cos(h0) + (w[1] - y0) * Math.sin(h0); }
    private static double localY(double[] w) { return -(w[0] - x0) * Math.sin(h0) + (w[1] - y0) * Math.cos(h0); }

    public static double posX(String unit) { return dist(unit, localX(cur)); }

    public static double posY(String unit) { return dist(unit, localY(cur)); }

    public static double heading(String unit) {
        double r = wrap(cur[2] - h0 + hOff);
        return unit.equals("DEGREES") ? Math.toDegrees(r) : r;
    }

    public static double velX(String unit) {
        double[] w = cur;
        return dist(unit, w[3] * Math.cos(h0) + w[4] * Math.sin(h0));
    }

    public static double velY(String unit) {
        double[] w = cur;
        return dist(unit, -w[3] * Math.sin(h0) + w[4] * Math.cos(h0));
    }

    public static double headingVelocity(String unit) {
        return unit.equals("DEGREES") ? Math.toDegrees(cur[5]) : cur[5];
    }

    public static void setHeading(double angle, String unit) {
        double target = unit.equals("DEGREES") ? Math.toRadians(angle) : angle;
        hOff = wrap(target - wrap(cur[2] - h0));
    }

    public static void setPosX(double v, String unit) {
        double now = localX(cur), want = undist(unit, v);
        x0 += (now - want) * Math.cos(h0); y0 += (now - want) * Math.sin(h0);
    }

    public static void setPosY(double v, String unit) {
        double now = localY(cur), want = undist(unit, v);
        x0 += -(now - want) * Math.sin(h0); y0 += (now - want) * Math.cos(h0);
    }

    public static void setOffsets(double xOffset, double yOffset, String unit) {}
}
