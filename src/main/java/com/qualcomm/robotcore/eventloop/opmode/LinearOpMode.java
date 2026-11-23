package com.qualcomm.robotcore.eventloop.opmode;

/** Versao de simulacao do LinearOpMode: o executor chama runOpMode() numa thread propria. */
public abstract class LinearOpMode extends OpMode {
    private volatile boolean started = false;

    public LinearOpMode() {}

    public abstract void runOpMode() throws InterruptedException;

    public void waitForStart() {
        while (!isStarted() && !isStopRequested()) {
            try { Thread.sleep(5); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }
    }

    public final void idle() { Thread.yield(); }

    public final void sleep(long milliseconds) {
        try { Thread.sleep(milliseconds); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public final boolean opModeIsActive() {
        idle();
        return isStarted() && !isStopRequested();
    }

    public final boolean opModeInInit() { return !isStarted() && !isStopRequested(); }

    public final boolean isStarted() { return started; }

    public final boolean isStopRequested() { return simStopRequested() || Thread.currentThread().isInterrupted(); }

    @Override public final void init() {}
    @Override public final void init_loop() {}
    @Override public final void start() {}
    @Override public final void loop() {}
    @Override public final void stop() {}

    /** Uso do simulador: aperta START. */
    public final void simStart() { started = true; resetRuntime(); }
}
