package biobuzz;

final class Geo {
    private Geo() {}

    static double[] corners(double x, double y, double h, double hl, double hw) {
        double c = Math.cos(h), s = Math.sin(h);
        double[] p = new double[8];
        for (int i = 0; i < 4; i++) {
            double lx = (i == 0 || i == 3) ? hl : -hl;
            double ly = (i < 2) ? hw : -hw;
            p[2 * i] = x + lx * c - ly * s;
            p[2 * i + 1] = y + lx * s + ly * c;
        }
        return p;
    }

    /** SAT entre retangulos orientados. Retorna {nx, ny, profundidade} para empurrar A, ou null. */
    static double[] sat(double ax, double ay, double ah, double ahl, double ahw,
                        double bx, double by, double bh, double bhl, double bhw) {
        double[] pa = corners(ax, ay, ah, ahl, ahw), pb = corners(bx, by, bh, bhl, bhw);
        double[][] axes = {
            {Math.cos(ah), Math.sin(ah)}, {-Math.sin(ah), Math.cos(ah)},
            {Math.cos(bh), Math.sin(bh)}, {-Math.sin(bh), Math.cos(bh)}};
        double best = Double.MAX_VALUE, bnx = 0, bny = 0;
        for (double[] axis : axes) {
            double minA = Double.MAX_VALUE, maxA = -Double.MAX_VALUE;
            double minB = Double.MAX_VALUE, maxB = -Double.MAX_VALUE;
            for (int i = 0; i < 4; i++) {
                double da = pa[2 * i] * axis[0] + pa[2 * i + 1] * axis[1];
                double db = pb[2 * i] * axis[0] + pb[2 * i + 1] * axis[1];
                minA = Math.min(minA, da); maxA = Math.max(maxA, da);
                minB = Math.min(minB, db); maxB = Math.max(maxB, db);
            }
            double overlap = Math.min(maxA, maxB) - Math.max(minA, minB);
            if (overlap <= 0) return null;
            if (overlap < best) { best = overlap; bnx = axis[0]; bny = axis[1]; }
        }
        if ((ax - bx) * bnx + (ay - by) * bny < 0) { bnx = -bnx; bny = -bny; }
        return new double[] {bnx, bny, best};
    }

    /** Retangulo alinhado {xmin,ymin,xmax,ymax} contra retangulo orientado. */
    static boolean overlapsRect(double x, double y, double h, double hl, double hw, double[] r) {
        return sat(x, y, h, hl, hw, (r[0] + r[2]) / 2, (r[1] + r[3]) / 2, 0, (r[2] - r[0]) / 2, (r[3] - r[1]) / 2) != null;
    }

    static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    static double wrap(double a) {
        while (a > Math.PI) a -= 2 * Math.PI;
        while (a <= -Math.PI) a += 2 * Math.PI;
        return a;
    }
}
