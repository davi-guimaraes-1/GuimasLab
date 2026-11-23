package biobuzz;

/** Logica da escolha visual da posicao e orientacao do AGGRON (sem Swing, para poder ser testada). */
public final class StartPicker {
    public final SimConfig cfg;

    public StartPicker(SimConfig cfg) { this.cfg = cfg; }

    private double len() { return StartRules.len(); }
    private double wid() { return StartRules.wid(); }

    /** Define o centro do robo (em polegadas). No Round, encosta na parede mais proxima. */
    public void place(double x, double y) {
        cfg.startX = x; cfg.startY = y;
        normalize();
    }

    public void setHeading(double h) {
        cfg.startH = Geo.wrap(h);
        normalize();
    }

    public void rotate(double delta) { setHeading(cfg.startH + delta); }

    private void normalize() {
        double[] p = Geo.corners(cfg.startX, cfg.startY, cfg.startH, len() / 2, wid() / 2);
        double minX = 1e9, maxX = -1e9, minY = 1e9, maxY = -1e9;
        for (int i = 0; i < 4; i++) {
            minX = Math.min(minX, p[2 * i]); maxX = Math.max(maxX, p[2 * i]);
            minY = Math.min(minY, p[2 * i + 1]); maxY = Math.max(maxY, p[2 * i + 1]);
        }
        if (minX < 0) cfg.startX -= minX;                          // sempre dentro do campo
        if (maxX > Layout.FIELD) cfg.startX -= maxX - Layout.FIELD;
        if (minY < 0) cfg.startY -= minY;
        if (maxY > Layout.FIELD) cfg.startY -= maxY - Layout.FIELD;
        if (cfg.mode == SimConfig.Mode.ROUND) {
            double[] s = StartRules.snapToWall(cfg.startX, cfg.startY, cfg.startH, len(), wid());
            cfg.startX = s[0]; cfg.startY = s[1];
        }
    }

    /** Motivo da violacao no Round, ou null. No teste de codigo qualquer posicao e aceita. */
    public String status() {
        return cfg.mode == SimConfig.Mode.ROUND ? StartRules.check(cfg) : null;
    }

    public boolean canStart() { return status() == null; }

    /** Chame ao trocar modo ou alianca. */
    public void refresh(boolean allianceChanged) {
        if (allianceChanged) cfg.resetStart();
        normalize();
    }
}
