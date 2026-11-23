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
    // Dimensoes reais do AGGRON (Layout.AGGRON_*): Y = comprimento (frente), X = largura, Z = altura
    public double length = Layout.AGGRON_Y_MM * Layout.MM, width = Layout.AGGRON_X_MM * Layout.MM;
    public double height = Layout.AGGRON_Z_MM * Layout.MM;
    public double mass = 10.75; // kg, igual a FollowerConstants.mass da equipe Atlas

    // base
    public double maxForward = 50, maxStrafe = 40;                 // in/s
    public double maxOmega = Math.toRadians(300);                  // rad/s
    public double accel = 110, angAccel = Math.toRadians(1200);    // in/s^2, rad/s^2

    // intake
    public int capacity = 4;
    public boolean intakesNectar = false; // AGGRON: intake coleta somente Pollen
    public double intakeReach = 2.5;

    // anexo coletor de Pollen (2 servos Taura): posicao 0 = levantado (recolhido), 1 = projetado para a frente
    public double attachServoL = 0, attachServoR = 0;   // posicoes comandadas
    public double attachExt = 0, attachPrevExt = 0;      // extensao real (0..1) agora e no passo anterior
    public double attachReach = Layout.ATTACH_REACH, attachWidth = Layout.ATTACH_WIDTH, attachServoRate = Layout.ATTACH_SERVO_RATE;
    /** O intake encostado na FLOWER retira Pollen? No AGGRON nao: so o anexo retira. */
    public boolean intakeRetrievesFlower = false;

    // torreta / shooter
    public double turretOffX = -2.05, turretOffY = 0, muzzleZ = 9.5; // torreta ~2 in atras do centro da caixa (CAD: torreta a 6,7 in da traseira); saida ~9,5 in do chao [EST]
    public double shootElev = Math.toRadians(55);
    public double shooterTau = 0.4, turretRate = Math.toRadians(360);
    public double minShotInterval = 0.15, speedNoise = 0.01, shotSpeedDrop = 0.03;

    // estado
    public double x, y, h, vx, vy, w;
    public double turret, shooterSpeed;
    /** Torreta controlada em malha aberta por potencia (motor RUN_WITHOUT_ENCODER do codigo da equipe). */
    public boolean turretOpenLoop = false;
    public double turretPower = 0, turretFullRate = Math.toRadians(240);   // rad/s com potencia 1 [EST]
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

    /** Comanda os dois servos do anexo (0 a 1). Eles precisam girar juntos para projetar o anexo por completo. */
    public void setAttach(double left, double right) {
        attachServoL = Geo.clamp(left, 0, 1);
        attachServoR = Geo.clamp(right, 0, 1);
    }

    public void deployAttach(boolean deploy) { setAttach(deploy ? 1 : 0, deploy ? 1 : 0); }

    public boolean attachDeployed() { return attachExt >= Layout.ATTACH_FULL; }

    public double[] toLocal(double px, double py) {
        double dx = px - x, dy = py - y, c = Math.cos(h), s = Math.sin(h);
        return new double[] {dx * c + dy * s, -dx * s + dy * c};
    }

    /** O intake so aceita o que o robo consegue coletar (Nectar so se intakesNectar). */
    public boolean canIntake(Ball b) { return !b.isNectar() || intakesNectar; }

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
        // anexo: os servos giram juntos; se estiverem desalinhados o anexo fica no meio do caminho
        if (enabled) {
            double tgt = (attachServoL + attachServoR) / 2;
            if (Math.abs(attachServoL - attachServoR) > Layout.ATTACH_MAX_MISMATCH) tgt = Math.min(tgt, 0.5);
            double dStep = attachServoRate * dt;
            attachPrevExt = attachExt;
            attachExt += Geo.clamp(tgt - attachExt, -dStep, dStep);
        } else {
            attachPrevExt = attachExt;
        }
        double target = enabled ? shooterTarget : 0;
        shooterSpeed += (target - shooterSpeed) * Math.min(1, dt / shooterTau);
        if (turretOpenLoop) {
            turret += (enabled ? turretPower : 0) * turretFullRate * dt;
        } else {
            double mt = turretRate * dt;
            turret += Geo.clamp(turretTarget - turret, -mt, mt);
        }
    }
}
