package biobuzz;

import java.util.ArrayList;
import java.util.List;

/**
 * Posicoes iniciais. O manual (G304) nao fixa pontos de partida: exige robo dentro do campo, no lado da propria alianca,
 * encostado na parede, fora da LOADING ZONE e sem tocar a area de uma FLOWER. Aqui isso e validado, e ha posicoes
 * legais padrao usadas pelos robos com IA.
 */
public final class StartRules {
    private StartRules() {}

    public static final double TOUCH_TOL = 0.3, SNAP_RANGE = 12.0;

    static double len() { return Layout.AGGRON_Y_MM * Layout.MM; }
    static double wid() { return Layout.AGGRON_X_MM * Layout.MM; }

    /** Retorna null se a pose e legal, ou o motivo da violacao. */
    public static String check(double x, double y, double h, double length, double width, Alliance a) {
        double[] p = Geo.corners(x, y, h, length / 2, width / 2);
        double minX = 1e9, maxX = -1e9, minY = 1e9, maxY = -1e9;
        for (int i = 0; i < 4; i++) {
            minX = Math.min(minX, p[2 * i]); maxX = Math.max(maxX, p[2 * i]);
            minY = Math.min(minY, p[2 * i + 1]); maxY = Math.max(maxY, p[2 * i + 1]);
        }
        double e = 0.01;
        if (minX < -e || maxX > Layout.FIELD + e || minY < -e || maxY > Layout.FIELD + e) return "robo fora do campo";
        boolean side = a == Alliance.RED ? maxX <= Layout.FIELD / 2 + e : minX >= Layout.FIELD / 2 - e;
        if (!side) return "fora do lado da propria alianca (G304.A)";
        if (!(minX <= TOUCH_TOL || maxX >= Layout.FIELD - TOUCH_TOL || minY <= TOUCH_TOL || maxY >= Layout.FIELD - TOUCH_TOL))
            return "precisa encostar na parede do campo (G304.C)";
        if (Geo.overlapsRect(x, y, h, length / 2, width / 2, Layout.RED_LZ)
            || Geo.overlapsRect(x, y, h, length / 2, width / 2, Layout.BLUE_LZ)) return "dentro da LOADING ZONE (G304.E)";
        for (double[] w : Layout.FLOWER_WALL) {
            boolean alongX = Math.abs(w[2]) < 0.5;   // flower na parede de baixo/cima: a area se estende em x
            double cx = w[0] + w[2] * Layout.FLOWER_FOOT_DEPTH / 2, cy = w[1] + w[3] * Layout.FLOWER_FOOT_DEPTH / 2;
            double hw = alongX ? Layout.FLOWER_FOOT_HALF_W : Layout.FLOWER_FOOT_DEPTH / 2;
            double hh = alongX ? Layout.FLOWER_FOOT_DEPTH / 2 : Layout.FLOWER_FOOT_HALF_W;
            if (Geo.sat(x, y, h, length / 2, width / 2, cx, cy, 0, hw, hh) != null) return "tocando a FLOWER (G304.D)";
        }
        for (HiveGeometry.Obstacle o : HiveGeometry.OBSTACLES)
            if (o.zBottom == 0 && Geo.sat(x, y, h, length / 2, width / 2, o.cx(), o.cy(), 0, o.hw(), o.hh()) != null)
                return "sobre a estrutura da HIVE";
        return null;
    }

    public static String check(SimConfig c) {
        return check(c.startX, c.startY, c.startH, len(), width(), c.alliance);
    }

    private static double width() { return wid(); }

    /** Desloca o robo ate encostar na parede mais proxima (se estiver a menos de SNAP_RANGE dela). Retorna {x, y}. */
    public static double[] snapToWall(double x, double y, double h, double length, double width) {
        double[] p = Geo.corners(x, y, h, length / 2, width / 2);
        double minX = 1e9, maxX = -1e9, minY = 1e9, maxY = -1e9;
        for (int i = 0; i < 4; i++) {
            minX = Math.min(minX, p[2 * i]); maxX = Math.max(maxX, p[2 * i]);
            minY = Math.min(minY, p[2 * i + 1]); maxY = Math.max(maxY, p[2 * i + 1]);
        }
        double[] d = {minX, Layout.FIELD - maxX, minY, Layout.FIELD - maxY};
        int k = 0;
        for (int i = 1; i < 4; i++) if (d[i] < d[k]) k = i;
        if (d[k] > SNAP_RANGE) return new double[] {x, y};
        switch (k) {
            case 0: return new double[] {x - minX, y};
            case 1: return new double[] {x + d[1], y};
            case 2: return new double[] {x, y - minY};
            default: return new double[] {x, y + d[3]};
        }
    }

    /** Posicoes iniciais legais de uma alianca, em ordem de preferencia: {x, y, heading}. */
    public static List<double[]> candidates(Alliance a) {
        double l = len();
        double[][] red = {{l / 2, 75, 0}, {45, l / 2, Math.PI / 2}, {30, Layout.FIELD - l / 2, -Math.PI / 2}};
        List<double[]> out = new ArrayList<>();
        for (double[] c : red) {
            if (a == Alliance.RED) out.add(c);
            else out.add(new double[] {Layout.FIELD - c[0], Layout.FIELD - c[1], Geo.wrap(c[2] + Math.PI)});
        }
        return out;
    }

    /** Partida com 4 robos: posicoes dos 3 robos com IA {alianca(0=vermelha,1=azul), x, y, heading}, sem sobrepor o AGGRON. */
    public static List<double[]> aiStarts(SimConfig cfg) {
        List<double[]> placed = new ArrayList<>();
        placed.add(new double[] {cfg.alliance.ordinal(), cfg.startX, cfg.startY, cfg.startH});
        List<double[]> out = new ArrayList<>();
        Alliance[] who = {cfg.alliance, cfg.alliance.opponent(), cfg.alliance.opponent()};
        for (Alliance a : who) {
            double[] chosen = null;
            for (double[] c : candidates(a)) {
                boolean free = true;
                for (double[] o : placed)
                    if (Geo.sat(c[0], c[1], c[2], len() / 2, wid() / 2, o[1], o[2], o[3], len() / 2, wid() / 2) != null) free = false;
                if (free) { chosen = c; break; }
            }
            if (chosen == null) chosen = candidates(a).get(0);
            double[] e = {a.ordinal(), chosen[0], chosen[1], chosen[2]};
            placed.add(e); out.add(e);
        }
        return out;
    }
}
