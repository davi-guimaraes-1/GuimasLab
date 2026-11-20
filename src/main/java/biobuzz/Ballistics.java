package biobuzz;

public final class Ballistics {
    private Ballistics() {}

    /**
     * Velocidade de saida (in/s) para a bola passar por um ponto a distancia horizontal d e
     * desnivel dz do ponto de saida, com elevacao theta (rad). NaN se impossivel.
     */
    public static double speedFor(double d, double dz, double theta) {
        double den = 2 * Math.pow(Math.cos(theta), 2) * (d * Math.tan(theta) - dz);
        if (den <= 0) return Double.NaN;
        return Math.sqrt(Layout.G * d * d / den);
    }
}
