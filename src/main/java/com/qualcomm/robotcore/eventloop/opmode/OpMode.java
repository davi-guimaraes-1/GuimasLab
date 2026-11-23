package com.qualcomm.robotcore.eventloop.opmode;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.Telemetry;

/** Versao de simulacao do OpMode do SDK 12 (mesma API usada pelo codigo da equipe). */
public abstract class OpMode {
    public static final java.util.HashMap<String, Object> blackboard = new java.util.HashMap<>();

    public HardwareMap hardwareMap = null;
    public Telemetry telemetry = null;
    public Gamepad gamepad1 = new Gamepad();
    public Gamepad gamepad2 = new Gamepad();
    public double time = 0.0;
    public int msStuckDetectInit, msStuckDetectInitLoop, msStuckDetectLoop, msStuckDetectStart;

    private volatile long startNs = System.nanoTime();
    private volatile boolean stopRequested = false;

    public OpMode() {}

    public abstract void init();

    public void init_loop() {}

    public void start() {}

    public abstract void loop();

    public void stop() {}

    public double getRuntime() { return (System.nanoTime() - startNs) / 1e9; }

    public void resetRuntime() { startNs = System.nanoTime(); }

    public final void terminateOpModeNow() { stopRequested = true; }

    public void requestOpModeStop() { stopRequested = true; }

    public void updateTelemetry(Telemetry t) { if (t != null) t.update(); }

    /** Uso do simulador. */
    public final boolean simStopRequested() { return stopRequested; }

    public final void simRequestStop() { stopRequested = true; }
}
