package biobuzz.sdk;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.firstinspires.ftc.robotcore.external.Func;
import org.firstinspires.ftc.robotcore.external.Telemetry;

/** Telemetria do OpMode e do Dashboard: acumula as linhas e publica no update(). */
public final class SimTelemetry {
    private SimTelemetry() {}

    private static final List<String> frame = new ArrayList<>();
    private static volatile List<String> last = new ArrayList<>();
    private static volatile List<String> lastPacket = new ArrayList<>();
    private static Telemetry proxy;

    public static synchronized Telemetry.Item addData(String caption, Object value) {
        if (value instanceof Func) value = ((Func<?>) value).value();
        frame.add(caption + ": " + value);
        return null;
    }

    public static synchronized Telemetry.Item addDataFormat(String caption, String fmt, Object... args) {
        if (args != null && args.length == 1 && args[0] instanceof Func) args = new Object[] {((Func<?>) args[0]).value()};
        String v;
        try { v = String.format(fmt, args == null ? new Object[0] : args); } catch (Exception e) { v = fmt; }
        frame.add(caption + ": " + v);
        return null;
    }

    public static synchronized Telemetry.Line addLine(String s) {
        frame.add(s);
        return null;
    }

    public static synchronized boolean update() {
        last = new ArrayList<>(frame);
        frame.clear();
        return true;
    }

    public static synchronized void clear() { frame.clear(); }

    public static List<String> lines() { return last; }

    public static void packet(TelemetryPacket p) {
        List<String> l = new ArrayList<>();
        for (java.util.Map.Entry<String, String> e : p.data.entrySet()) l.add(e.getKey() + ": " + e.getValue());
        lastPacket = l;
    }

    public static List<String> packetLines() { return lastPacket; }

    public static synchronized Telemetry proxy() {
        if (proxy == null) {
            proxy = (Telemetry) Proxy.newProxyInstance(Telemetry.class.getClassLoader(), new Class<?>[] {Telemetry.class}, new InvocationHandler() {
                @Override public Object invoke(Object p, Method m, Object[] a) {
                    switch (m.getName()) {
                        case "addData":
                            if (a.length == 2) return addData((String) a[0], a[1]);
                            if (a.length == 3 && a[2] instanceof Object[]) return addDataFormat((String) a[0], (String) a[1], (Object[]) a[2]);
                            return addDataFormat((String) a[0], (String) a[1], a[2]);
                        case "addLine": return addLine(a == null || a.length == 0 ? "" : (String) a[0]);
                        case "update": return update();
                        case "clear": case "clearAll": clear(); return null;
                        default:
                            if (m.getDeclaringClass() == Object.class) return Dyn.objectMethod(p, m, a);
                            return Dyn.defaultFor(m.getReturnType());
                    }
                }
            });
        }
        return proxy;
    }

    public static synchronized void reset() { frame.clear(); last = new ArrayList<>(); lastPacket = new ArrayList<>(); }
}
