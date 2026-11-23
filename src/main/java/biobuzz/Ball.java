package biobuzz;

public final class Ball {
    public enum Type { POLLEN, NECTAR_RED, NECTAR_BLUE }
    public enum State { FIELD, HELD, FLOWER, CELL, AREA }

    public final int id;
    public final Type type;
    public final double r, weight, massKg;
    /** Robo que acabou de lancar esta bola: sem colisao com ele ate a bola sair da pegada. */
    public Robot ignoreRobot;
    public double x, y, z, vx, vy, vz;
    public State state = State.FIELD;

    public Ball(int id, Type type) {
        this.id = id;
        this.type = type;
        this.r = type == Type.POLLEN ? Layout.POLLEN_R : Layout.NECTAR_R;
        this.weight = type == Type.POLLEN ? Layout.POLLEN_WEIGHT : Layout.NECTAR_WEIGHT;
        this.massKg = type == Type.POLLEN ? Layout.POLLEN_MASS_KG : Layout.NECTAR_MASS_KG;
        this.z = r;
    }

    public boolean isNectar() { return type != Type.POLLEN; }

    public Alliance nectarAlliance() {
        return type == Type.NECTAR_RED ? Alliance.RED : type == Type.NECTAR_BLUE ? Alliance.BLUE : null;
    }

    public double speed() { return Math.sqrt(vx * vx + vy * vy + vz * vz); }
}
