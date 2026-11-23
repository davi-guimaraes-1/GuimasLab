package biobuzz.sdk;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/** Motor virtual (DcMotorEx): guarda os comandos do codigo e as leituras (encoder, velocidade) que o simulador atualiza. */
public final class SimMotor implements InvocationHandler {
    public final String name;
    public volatile double power;                  // setPower
    public volatile double targetVelocity;         // setVelocity (ticks/s)
    public volatile boolean velocityMode;
    public volatile boolean reversed;
    public volatile double position;               // ticks (simulador -> codigo)
    public volatile double velocity;               // ticks/s (simulador -> codigo)
    private volatile DcMotor.RunMode mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER;
    private volatile DcMotor.ZeroPowerBehavior zpb = DcMotor.ZeroPowerBehavior.BRAKE;
    private volatile double zeroPosition;
    public final double ticksPerRev;
    private final DcMotorEx proxy;

    public SimMotor(String name, double ticksPerRev) {
        this.name = name;
        this.ticksPerRev = ticksPerRev;
        this.proxy = (DcMotorEx) Proxy.newProxyInstance(DcMotorEx.class.getClassLoader(), new Class<?>[] {DcMotorEx.class}, this);
    }

    public DcMotorEx device() { return proxy; }

    /** Potencia comandada, com o efeito da direcao quando ela vale para o mecanismo. */
    public double commandedPower() { return velocityMode ? 0 : power; }

    @Override
    public Object invoke(Object p, Method m, Object[] a) {
        switch (m.getName()) {
            case "setPower": power = (Double) a[0]; velocityMode = false; return null;
            case "getPower": return power;
            case "setVelocity":
                if (a.length == 1) targetVelocity = (Double) a[0];
                else {
                    String u = a[1].toString();
                    targetVelocity = (Double) a[0] * ticksPerRev / (u.equals("DEGREES") ? 360.0 : 2 * Math.PI);
                }
                velocityMode = true;
                return null;
            case "getVelocity":
                if (a == null || a.length == 0) return velocity;
                return velocity * (a[0].toString().equals("DEGREES") ? 360.0 : 2 * Math.PI) / ticksPerRev;
            case "setDirection": reversed = a[0] == DcMotorSimple.Direction.REVERSE; return null;
            case "getDirection": return reversed ? DcMotorSimple.Direction.REVERSE : DcMotorSimple.Direction.FORWARD;
            case "setMode":
                mode = (DcMotor.RunMode) a[0];
                if (mode == DcMotor.RunMode.STOP_AND_RESET_ENCODER) zeroPosition = position;
                return null;
            case "getMode": return mode;
            case "setZeroPowerBehavior": zpb = (DcMotor.ZeroPowerBehavior) a[0]; return null;
            case "getZeroPowerBehavior": return zpb;
            case "getCurrentPosition": return (int) Math.round(position - zeroPosition);
            case "isBusy": return false;
            case "getDeviceName": return "Motor " + name;
            case "getConnectionInfo": return "sim";
            default:
                if (m.getDeclaringClass() == Object.class) return Dyn.objectMethod(p, m, a);
                return Dyn.defaultFor(m.getReturnType());
        }
    }
}
