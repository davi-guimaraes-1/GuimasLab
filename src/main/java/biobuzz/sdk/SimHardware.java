package biobuzz.sdk;

import biobuzz.Match;
import biobuzz.Robot;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * Hardware virtual do AGGRON: registra no HardwareMap os mesmos nomes do robo real e liga os comandos do codigo
 * (potencias, velocidade do shooter, torre, servos) ao Robot do simulador, e as leituras (Pinpoint, encoders) de volta.
 */
public final class SimHardware {
    public final Match match;
    public final Robot robot;
    public final SdkParams p;
    public final HardwareMap hardwareMap = new HardwareMap();
    public final SimMotor fl, fr, bl, br, shooter, intake, transfer, torre;
    public final SimServo servo, servo1, servo2;

    public SimHardware(Match match, Robot robot, SdkParams p) {
        this.match = match; this.robot = robot; this.p = p;
        fl = motor(p.leftFront, 28); fr = motor(p.rightFront, 28); bl = motor(p.leftRear, 28); br = motor(p.rightRear, 28);
        shooter = motor("shooter", p.shooterTicksPerRev);
        intake = motor("intake", 28); transfer = motor("transfer", 28);
        torre = motor("torre", p.turretTicksPerRev);
        servo = servo("servo"); servo1 = servo("servo1"); servo2 = servo("servo2");
        hardwareMap.put("pinpoint", new GoBildaPinpointDriver(null, false));
        robot.maxForward = p.driveMaxForward;
        robot.maxStrafe = p.driveMaxStrafe;
        robot.turretOpenLoop = true;
        robot.turretFullRate = Math.toRadians(p.turretFullPowerDegPerSec);
        SimTelemetry.reset();
        SimPinpoint.publish(robot.x, robot.y, robot.h, 0, 0, 0);
        SimPinpoint.init();
    }

    private SimMotor motor(String name, double ticksPerRev) {
        SimMotor m = new SimMotor(name, ticksPerRev);
        hardwareMap.put(name, m.device());
        return m;
    }

    private SimServo servo(String name) {
        SimServo s = new SimServo(name);
        hardwareMap.put(name, s.device());
        return s;
    }

    /** Antes de cada passo do simulador: comandos do codigo -> robo. */
    public void applyToRobot() {
        double a = fl.power, b = fr.power, c = bl.power, d = br.power;      // potencia positiva = roda para frente (direcoes ja ajustadas pela equipe)
        double y = (a + b + c + d) / 4, x = (a - b - c + d) / 4, rx = (a - b + c - d) / 4;   // frente, direita, giro horario
        robot.drive(y, -x, -rx);
        robot.intakeOn = intake.power > p.intakeThreshold;
        if (transfer.power > p.intakeThreshold) robot.shoot();             // o transfer empurra o Pollen para o shooter
        double ticks = shooter.velocityMode ? Math.abs(shooter.targetVelocity) : Math.abs(shooter.power) * p.shooterMaxTicksPerSec;
        robot.shooterTarget = p.exitSpeedFromTicks(ticks);
        robot.turretPower = torre.power * (torre.reversed ? -1 : 1);
        robot.setAttach(servo.effective(), servo2.effective());           // servos Taura do anexo (0 = levantado, 1 = projetado) [suposicao]
    }

    /** Depois de cada passo: robo -> leituras do codigo. */
    public void publish() {
        SimPinpoint.publish(robot.x, robot.y, robot.h, robot.vx, robot.vy, robot.w);
        shooter.velocity = p.ticksFromExitSpeed(robot.shooterSpeed);
        shooter.position += shooter.velocity * 0.015;
        torre.position = robot.turret / (2 * Math.PI) * p.turretTicksPerRev * (torre.reversed ? -1 : 1);
    }
}
