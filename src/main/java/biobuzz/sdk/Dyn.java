package biobuzz.sdk;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Utilidades das classes de simulacao do SDK. */
public final class Dyn {
    private Dyn() {}

    private static final Set<String> warned = ConcurrentHashMap.newKeySet();

    /** Metodo do SDK que o simulador ainda nao implementa: avisa uma vez e devolve o valor padrao. */
    public static void unsupported(String what) {
        if (warned.add(what)) System.err.println("[sim] metodo do SDK ainda nao suportado pelo simulador: " + what);
    }

    public static Object defaultFor(Class<?> t) {
        if (t == boolean.class) return false;
        if (t == int.class) return 0;
        if (t == long.class) return 0L;
        if (t == double.class) return 0.0;
        if (t == float.class) return 0f;
        if (t == short.class) return (short) 0;
        if (t == byte.class) return (byte) 0;
        if (t == char.class) return '\0';
        return null;
    }

    /** Proxy que aceita qualquer chamada: devolve padroes e, para interfaces, outro proxy no-op (permite encadeamento). */
    public static Object noop(Class<?> iface) {
        return Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] {iface}, new InvocationHandler() {
            @Override public Object invoke(Object proxy, Method m, Object[] args) {
                Class<?> r = m.getReturnType();
                if (m.getDeclaringClass() == Object.class) return objectMethod(proxy, m, args);
                if (r.isInterface()) return noop(r);
                return defaultFor(r);
            }
        });
    }

    public static Object objectMethod(Object proxy, Method m, Object[] args) {
        switch (m.getName()) {
            case "toString": return "SimDevice@" + Integer.toHexString(System.identityHashCode(proxy));
            case "hashCode": return System.identityHashCode(proxy);
            case "equals": return proxy == args[0];
            default: return null;
        }
    }
}
