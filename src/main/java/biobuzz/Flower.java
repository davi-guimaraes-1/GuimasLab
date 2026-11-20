package biobuzz;

import java.util.ArrayList;
import java.util.List;

public final class Flower {
    public final String name;
    public final double wallX, wallY, nx, ny;   // ponto na parede e normal para dentro do campo
    public final double ox, oy;                 // centro da abertura superior (em planta)
    public final List<Ball> stack = new ArrayList<>(); // 0 = fundo
    public double cooldown;

    public Flower(String name, double wallX, double wallY, double nx, double ny) {
        this.name = name;
        this.wallX = wallX;
        this.wallY = wallY;
        this.nx = nx;
        this.ny = ny;
        this.ox = wallX + nx * Layout.FLOWER_OFFSET;
        this.oy = wallY + ny * Layout.FLOWER_OFFSET;
    }

    /** Recalcula a altura de cada bola empilhada. */
    public void relayout() {
        double zc = Layout.FLOWER_BASE_Z;
        for (Ball b : stack) {
            b.z = zc + b.r;
            zc += 2 * b.r;
        }
    }

    public double stackHeight() {
        double h = Layout.FLOWER_BASE_Z;
        for (Ball b : stack) h += 2 * b.r;
        return h;
    }

    public boolean fits() { return stackHeight() < Layout.FLOWER_OPEN_Z - 1.0; }

    /** Esta parcialmente dentro do volume de pontuacao (entre anel do meio e anel de cima)? */
    public boolean scoring(Ball b) {
        return b.z + b.r > Layout.FLOWER_MIDDLE_Z && b.z - b.r < Layout.FLOWER_OPEN_Z;
    }
}
