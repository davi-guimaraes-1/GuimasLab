package biobuzz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Geometria de COLISAO da estrutura HIVE, separada do desenho (o Viewer desenha o frame por conta propria).
 * O frame e um cavalete em A: so as duas barras de base no chao impedem um robo baixo de passar.
 * A parte movel (CELLs e travessa) fica acima de HIVE_OVERHEAD_Z e so bloqueia robos mais altos que isso.
 */
public final class HiveGeometry {
    private HiveGeometry() {}

    public static final class Obstacle {
        public final String name;
        public final double xmin, ymin, xmax, ymax, zBottom, zTop;

        Obstacle(String name, double xmin, double ymin, double xmax, double ymax, double zBottom, double zTop) {
            this.name = name; this.xmin = xmin; this.ymin = ymin; this.xmax = xmax; this.ymax = ymax;
            this.zBottom = zBottom; this.zTop = zTop;
        }

        public double cx() { return (xmin + xmax) / 2; }
        public double cy() { return (ymin + ymax) / 2; }
        public double hw() { return (xmax - xmin) / 2; }
        public double hh() { return (ymax - ymin) / 2; }

        /** Um robo que ocupa de 0 ate robotHeight colide se o obstaculo comeca abaixo do topo do robo. */
        public boolean blocks(double robotHeight) { return zBottom < robotHeight; }

        public boolean contains(double x, double y) { return x >= xmin && x <= xmax && y >= ymin && y <= ymax; }
    }

    public static final List<Obstacle> OBSTACLES;

    static {
        double x0 = Layout.FRAME_CX - Layout.FRAME_W / 2, x1 = Layout.FRAME_CX + Layout.FRAME_W / 2;
        double y0 = Layout.FRAME_CY - Layout.FRAME_D / 2, y1 = Layout.FRAME_CY + Layout.FRAME_D / 2;
        List<Obstacle> l = new ArrayList<>();
        l.add(new Obstacle("barra de base (lado x menor)", x0, y0, x0 + Layout.HIVE_BASE_BAR_W, y1, 0, Layout.HIVE_BASE_BAR_H));
        l.add(new Obstacle("barra de base (lado x maior)", x1 - Layout.HIVE_BASE_BAR_W, y0, x1, y1, 0, Layout.HIVE_BASE_BAR_H));
        l.add(new Obstacle("parte movel e travessa", x0, y0, x1, y1, Layout.HIVE_OVERHEAD_Z, Layout.HIVE_OVERHEAD_TOP));
        OBSTACLES = Collections.unmodifiableList(l);
    }

    /** O ponto (x, y) esta bloqueado para um robo dessa altura? */
    public static boolean blocked(double x, double y, double robotHeight) {
        for (Obstacle o : OBSTACLES) if (o.blocks(robotHeight) && o.contains(x, y)) return true;
        return false;
    }
}
