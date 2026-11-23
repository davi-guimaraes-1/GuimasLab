package com.qualcomm.robotcore.hardware;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** HardwareMap de simulacao: dispositivos registrados pelo simulador, acessados pelo nome como no robo real. */
public class HardwareMap {
    private final Map<String, Object> devices = new LinkedHashMap<>();

    public HardwareMap() {}

    public void put(String name, Object device) { devices.put(name, device); }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<? extends T> classOrInterface, String deviceName) {
        Object d = devices.get(deviceName);
        if (d == null)
            throw new IllegalArgumentException("Unable to find a hardware device with name \"" + deviceName + "\" and type " + classOrInterface.getSimpleName());
        if (!classOrInterface.isInstance(d))
            throw new IllegalArgumentException("Hardware device \"" + deviceName + "\" is not a " + classOrInterface.getSimpleName());
        return (T) d;
    }

    public <T> T tryGet(Class<? extends T> classOrInterface, String deviceName) {
        Object d = devices.get(deviceName);
        return d != null && classOrInterface.isInstance(d) ? classOrInterface.cast(d) : null;
    }

    public <T> List<T> getAll(Class<T> classOrInterface) {
        List<T> out = new ArrayList<>();
        for (Object d : devices.values()) if (classOrInterface.isInstance(d)) out.add(classOrInterface.cast(d));
        return out;
    }

    public java.util.Set<String> names() { return devices.keySet(); }
}
