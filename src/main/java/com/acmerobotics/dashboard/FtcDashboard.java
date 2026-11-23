package com.acmerobotics.dashboard;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import org.firstinspires.ftc.robotcore.external.Telemetry;

/** FTC Dashboard de simulacao: aceita os pacotes e a telemetria e mostra no simulador. */
public class FtcDashboard {
    private static final FtcDashboard INSTANCE = new FtcDashboard();

    private FtcDashboard() {}

    public static FtcDashboard getInstance() { return INSTANCE; }

    public Telemetry getTelemetry() { return biobuzz.sdk.SimTelemetry.proxy(); }

    public void sendTelemetryPacket(TelemetryPacket packet) { biobuzz.sdk.SimTelemetry.packet(packet); }

    public void startCameraStream(Object source, double maxFps) {}

    public void stopCameraStream() {}

    public void updateConfig() {}

    public void clearTelemetry() {}
}
