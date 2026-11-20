package biobuzz;

import java.util.ArrayDeque;

/**
 * Robo virtual: base mecanum (modelo cinematico com limite de aceleracao), intake frontal,
 * torreta giratoria com shooter de flywheel. Todos os parametros sao publicos para serem
 * ajustados conforme o robo real.
 */
public final class Robot {
    public final String name;
    public final Alliance alliance;

    // geometria (frame do robo: +x frente, +y esquerda)
    public double length = 22.0, width = 16.7; // medidas do CAD (AGGRON), com o intake estendido

    // base
    public double maxForward = 50, maxStrafe = 40;                 // in/s
    public double maxOmega = Math.toRadians(300);                  // rad/s
    public double accel = 110, angAccel = Math.toRadians(1200);    // in/s^2, rad/s^2

    // intake
    public int capacity = 4;
    public boolean intakesNectar = true;
    public double intakeReach = 2.5;

    // torreta / shooter
    public double turretOffX = -4.27, turretOffY = 0, muzzleZ = 9.5; // torreta ~4,3 in atras do centro da caixa; saida ~9,5 in do chao [EST]
    public double shootElev = Math.toRadians(55);
    public double shooterTau = 0.4, turretRate = Math.toRadians(360);
    public double minShotInterval = 0.15, speedNoise = 0.01, shotSpeedDrop = 0.03;

    // estado
    public double x, y, h, vx, vy, w;
    public double turret, shooterSpeed;
    public final ArrayDeque<Ball> held = new ArrayDeque<>();

    // comandos
    private double cmdF, cmdS, cmdT;
    public boolean intakeOn, shootRequest;
    public double turretTarget, shooterTarget;

    // bookkeeping de pontuacao
    public boolean leftInAuto, parkedInAuto;
    public double lastShotT = -99;

    public Robot(String name, Alliance alliance) {
        this.name = name;
        this.alliance = alliance;
    }

    public void setPose(double x, double y, double h) {
        this.x = x; this.y = y; this.h = h;
        vx = vy = w = 0;
    }

    /** Comando estilo gamepad, valores de -1 a 1. fwd: frente, strafe: esquerda positiva, turn: anti-horario positivo. */
    public void drive(double fwd, double strafe, double turn) {
        cmdF = Geo.clamp(fwd, -1, 1);
        cmdS = Geo.clamp(strafe, -1, 1);
        cmdT = Geo.clamp(turn, -1, 1);
    }

    public void shoot() { shootRequest = true; }

    public double[] toLocal(double px, double py) {
        double dx = px - x, dy = py - y, c = Math.cos(h), s = Math.sin(h);
        return new double[] {dx * c + dy * s, -dx * s + dy * c};
    }

    public boolean shooterReady() { return shooterTarget > 0 && shooterSpeed >= 0.97 * shooterTarget; }

    void update(double dt, boolean enabled) {
        double f = enabled ? cmdF : 0, s = enabled ? cmdS : 0, t = enabled ? cmdT : 0;
        double c = Math.cos(h), sn = Math.sin(h);
        double vf = vx * c + vy * sn, vs = -vx * sn + vy * c;
        double ef = f * maxForward - vf, es = s * maxStrafe - vs;
        double mag = Math.hypot(ef, es), step = accel * dt;
        if (mag > step) { ef *= step / mag; es *= step / mag; }
        vf += ef; vs += es;
        double mw = angAccel * dt;
        w += Geo.clamp(t * maxOmega - w, -mw, mw);
        vx = vf * c - vs * sn;
        vy = vf * sn + vs * c;
        x += vx * dt; y += vy * dt;
        h = Geo.wrap(h + w * dt);
        double target = enabled ? shooterTarget : 0;
        shooterSpeed += (target - shooterSpeed) * Math.min(1, dt / shooterTau);
        double mt = turretRate * dt;
        turret += Geo.clamp(turretTarget - turret, -mt, mt);
    }
}
