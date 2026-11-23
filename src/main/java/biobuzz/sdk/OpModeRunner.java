package biobuzz.sdk;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.Telemetry;

/** Executa um OpMode (LinearOpMode ou iterativo) numa thread propria, em tempo real, como o app do Robot Controller. */
public final class OpModeRunner {
    public final OpMode op;
    public volatile Throwable error;
    public volatile boolean finished;
    private volatile boolean started;
    private Thread thread;

    public OpModeRunner(OpMode op, HardwareMap hw, Telemetry telemetry) {
        this.op = op;
        op.hardwareMap = hw;
        op.telemetry = telemetry;
    }

    /** Entra em INIT. */
    public void begin() {
        thread = new Thread(this::run, "OpMode-" + op.getClass().getSimpleName());
        thread.setDaemon(true);
        thread.start();
    }

    public void pressStart() {
        started = true;
        if (op instanceof LinearOpMode) ((LinearOpMode) op).simStart();
    }

    public boolean isStarted() { return started; }

    public void requestStop() { op.simRequestStop(); }

    public void join(long ms) {
        try { if (thread != null) thread.join(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void run() {
        try {
            if (op instanceof LinearOpMode) {
                ((LinearOpMode) op).runOpMode();
            } else {
                op.init();
                while (!started && !op.simStopRequested()) { op.init_loop(); Thread.sleep(10); }
                if (!op.simStopRequested()) {
                    op.resetRuntime();
                    op.start();
                    while (!op.simStopRequested()) { op.time = op.getRuntime(); op.loop(); Thread.sleep(10); }
                }
                op.stop();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            error = t;
            System.err.println("[sim] erro no OpMode " + op.getClass().getName() + ": " + t);
            t.printStackTrace();
        } finally {
            finished = true;
        }
    }
}
