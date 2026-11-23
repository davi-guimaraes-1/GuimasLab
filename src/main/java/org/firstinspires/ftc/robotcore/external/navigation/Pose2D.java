package org.firstinspires.ftc.robotcore.external.navigation;

/** Pose2D de simulacao (posicao + heading com unidades). */
public class Pose2D {
    private final double xInches, yInches, headingRad;

    public Pose2D(DistanceUnit distanceUnit, double x, double y, AngleUnit angleUnit, double heading) {
        this.xInches = toInches(distanceUnit, x);
        this.yInches = toInches(distanceUnit, y);
        this.headingRad = angleUnit.name().equals("DEGREES") ? Math.toRadians(heading) : heading;
    }

    static double toInches(DistanceUnit u, double v) {
        switch (u.name()) {
            case "MM": return v / 25.4;
            case "CM": return v / 2.54;
            case "METER": return v / 0.0254;
            default: return v;
        }
    }

    private static double fromInches(DistanceUnit u, double v) {
        switch (u.name()) {
            case "MM": return v * 25.4;
            case "CM": return v * 2.54;
            case "METER": return v * 0.0254;
            default: return v;
        }
    }

    public double getX(DistanceUnit unit) { return fromInches(unit, xInches); }
    public double getY(DistanceUnit unit) { return fromInches(unit, yInches); }
    public double getHeading(AngleUnit unit) { return unit.name().equals("DEGREES") ? Math.toDegrees(headingRad) : headingRad; }
}
