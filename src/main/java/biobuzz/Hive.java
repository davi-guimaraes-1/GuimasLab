package biobuzz;

import java.util.ArrayList;
import java.util.List;

/** Uma HIVE biestavel com duas CELLS (indice 0 = y menor, 1 = y maior). */
public final class Hive {
    public final Alliance alliance;
    public final double axisX, centerY;
    public int up;                       // indice da CELL voltada para cima
    public double energy;                // impulso acumulado desde o ultimo tip
    public int tipsAuto, tipsTele;
    private final List<Ball> cell0 = new ArrayList<>(), cell1 = new ArrayList<>();

    public Hive(Alliance a, double axisX, double centerY, int upInitial) {
        this.alliance = a;
        this.axisX = axisX;
        this.centerY = centerY;
        this.up = upInitial;
    }

    public List<Ball> cell(int i) { return i == 0 ? cell0 : cell1; }

    public double cellY(int i) { return centerY + (i == 0 ? -1 : 1) * Layout.CELL_SPACING / 2; }

    /** y do plano da abertura (face externa) da CELL i. */
    public double mouthY(int i) { return cellY(i) + (i == 0 ? -1 : 1) * Layout.CELL_DEPTH / 2; }

    /** Sentido em y que a bola precisa ter para entrar na CELL i. */
    public int entryDir(int i) { return i == 0 ? +1 : -1; }

    public int tips() { return tipsAuto + tipsTele; }
}
