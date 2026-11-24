package biobuzz.sdk;

import biobuzz.Match;
import biobuzz.Robot;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Uma execucao de OpMode da equipe ligada ao robo AGGRON da partida. */
public final class CodeSession {
    public final Match match;
    public final Robot robot;
    public final SimHardware hw;
    public final OpModeRunner runner;
    public final String name;

    public CodeSession(Match match, Robot robot, OpMode op, SdkParams params) {
        this.match = match; this.robot = robot;
        this.name = op.getClass().getSimpleName();
        this.hw = new SimHardware(match, robot, params);
        this.runner = new OpModeRunner(op, hw.hardwareMap, SimTelemetry.proxy());
    }

    public void begin() { runner.begin(); }

    public void pressStart() { runner.pressStart(); }

    public void stop() { runner.requestStop(); runner.join(500); }

    /** Chame antes de Match.run(): teclado -> gamepad, comandos do codigo -> robo. */
    public void beforeTick(Set<Integer> keys) {
        ControllerInput.poll();
        PadState p1 = ControllerInput.pad(0), p2 = ControllerInput.pad(1);
        if (p1 != null) GamepadMapper.apply(p1, runner.op.gamepad1);          // controle fisico tem prioridade
        else if (keys != null) GamepadMapper.apply(keys, runner.op.gamepad1);
        if (p2 != null) GamepadMapper.apply(p2, runner.op.gamepad2);
        if (p1 != null && p1.start && !runner.isStarted()) pressStart();         // START do controle inicia o OpMode
        hw.applyToRobot();
    }

    /** Chame depois de Match.run(): robo -> leituras do codigo. */
    public void afterTick() { hw.publish(); }

    /** Avanca a simulacao em tempo real (usado nos testes). */
    public void runRealtime(double seconds, Set<Integer> keys) {
        long end = System.nanoTime() + (long) (seconds * 1e9);
        while (System.nanoTime() < end) {
            beforeTick(keys);
            match.run(0.015);
            afterTick();
            try { Thread.sleep(15); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }
    }

    public List<String> hudLines() {
        List<String> l = new ArrayList<>();
        l.add("OpMode: " + name + (runner.isStarted() ? " (rodando)" : " (INIT: ENTER ou START do controle)"));
        l.add(ControllerInput.status());
        if (runner.error != null) {
            boolean pedro = false;
            Throwable root = runner.error;
            for (Throwable t = runner.error; t != null; t = t.getCause()) {
                root = t;
                if (t instanceof UnsupportedOperationException)
                    for (StackTraceElement e : t.getStackTrace()) if (e.getClassName().startsWith("com.pedropathing")) pedro = true;
            }
            if (pedro) {
                l.add("ERRO: o Pedro Pathing real nao esta no");
                l.add("classpath (so ha stubs). Veja README: libs/real");
            } else l.add("ERRO: " + root);
        }
        if (runner.finished && runner.error == null) l.add("OpMode terminou");
        l.addAll(SimTelemetry.lines());
        return l;
    }
}
