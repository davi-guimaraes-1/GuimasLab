package biobuzz.sdk;

import com.qualcomm.robotcore.hardware.Servo;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/** Servo virtual. A posicao efetiva (0 a 1) ja considera a direcao REVERSE (espelhada). */
public final class SimServo implements InvocationHandler {
    public final String name;
    public volatile double position = 0.0;
    public volatile boolean reversed;
    private volatile double scaleMin = 0, scaleMax = 1;
    private final Servo proxy;

    public SimServo(String name) {
        this.name = name;
        this.proxy = (Servo) Proxy.newProxyInstance(Servo.class.getClassLoader(), new Class<?>[] {Servo.class}, this);
    }

    public Servo device() { return proxy; }

    /**
     * Posicao geometrica do eixo (0 a 1). Suposicao: a direcao REVERSE so compensa a montagem espelhada dos dois servos
     * do anexo, entao o mesmo comando move os dois para o mesmo lado.
     */
    public double effective() {
        return scaleMin + position * (scaleMax - scaleMin);
    }

    @Override
    public Object invoke(Object p, Method m, Object[] a) {
        switch (m.getName()) {
            case "setPosition": position = Math.max(0, Math.min(1, (Double) a[0])); return null;
            case "getPosition": return position;
            case "setDirection": reversed = a[0] == Servo.Direction.REVERSE; return null;
            case "getDirection": return reversed ? Servo.Direction.REVERSE : Servo.Direction.FORWARD;
            case "scaleRange": scaleMin = (Double) a[0]; scaleMax = (Double) a[1]; return null;
            case "getDeviceName": return "Servo " + name;
            default:
                if (m.getDeclaringClass() == Object.class) return Dyn.objectMethod(p, m, a);
                return Dyn.defaultFor(m.getReturnType());
        }
    }
}
