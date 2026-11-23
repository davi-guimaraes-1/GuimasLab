package biobuzz.sdk;

/** Parametros que ligam o codigo da equipe (potencias, ticks) ao robo virtual. [EST] = suposicao a calibrar. */
public final class SdkParams {
    // base: velocidades a potencia 1, iguais a MecanumConstants.xVelocity/yVelocity da Atlas
    public double driveMaxForward = 75.17, driveMaxStrafe = 59.33;
    // nomes dos motores de roda (iguais aos do Constants da Atlas)
    public String leftFront = "superiorEsquerdo", rightFront = "superiorDireito", leftRear = "inferiorEsquerdo", rightRear = "inferiorDireito";
    // shooter sem reducao, motor 6000 rpm: 28 ticks/volta. Diametro do flywheel e eficiencia sao estimativas [EST]
    public double shooterTicksPerRev = 28, shooterMaxTicksPerSec = 2800, flywheelDiameter = 4.0, exitEfficiency = 0.5;
    // torre: 720 ticks/volta (TICKS_PER_TURRET_REV da Atlas); velocidade a potencia 1 [EST]
    public double turretTicksPerRev = 720, turretFullPowerDegPerSec = 240;
    public double intakeThreshold = 0.05;

    public double exitSpeedFromTicks(double ticksPerSec) {
        return exitEfficiency * Math.PI * flywheelDiameter * (ticksPerSec / shooterTicksPerRev);
    }

    public double ticksFromExitSpeed(double speed) {
        return speed / (exitEfficiency * Math.PI * flywheelDiameter) * shooterTicksPerRev;
    }
}
