package com.acmerobotics.dashboard.telemetry;

import com.acmerobotics.dashboard.canvas.Canvas;
import java.util.LinkedHashMap;
import java.util.Map;

public class TelemetryPacket {
    public final Map<String, String> data = new LinkedHashMap<>();
    private final Canvas fieldOverlay = new Canvas();

    public TelemetryPacket() {}

    public TelemetryPacket(boolean fieldOverlayUnused) {}

    public void put(String key, Object value) { data.put(key, String.valueOf(value)); }

    public void addLine(String line) { data.put(line, ""); }

    public void clearLines() {}

    public Canvas fieldOverlay() { return fieldOverlay; }
}
