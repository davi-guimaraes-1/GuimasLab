package biobuzz.sdk;

import biobuzz.*;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.*;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/** Testes da camada SDK: roda OpModes (de exemplo e, se informado, o TeleOp original da equipe) no simulador. */
public final class SdkSelfTest {
    static int fails = 0;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
        if (!ok) fails++;
    }

    static double wrap(double a) {
        while (a > Math.PI) a -= 2 * Math.PI;
        while (a <= -Math.PI) a += 2 * Math.PI;
        return a;
    }

    static Set<Integer> keys(int... k) { Set<Integer> s = new HashSet<>(); for (int x : k) s.add(x); return s; }

    static Match freeMatch(double x, double y, double h) {
        SimConfig c = new SimConfig();
        c.mode = SimConfig.Mode.CODE_TEST; c.startX = x; c.startY = y; c.startH = h;
        return Viewer.newMatch(c);
    }

    public static void main(String[] args) throws Exception {
        // ---------- OpMode de exemplo (mesmos nomes de hardware da Atlas) ----------
        Match m = freeMatch(30, 30, 0);
        Robot r = m.robots.get(0);
        CodeSession s = new CodeSession(m, r, new biobuzz.sdk.demo.DemoTeleOp(), new SdkParams());
        s.begin();
        s.runRealtime(0.3, keys());
        check("OpMode entra em INIT e espera o START", !s.runner.isStarted() && !s.runner.finished && s.runner.error == null);
        s.pressStart();
        s.runRealtime(0.3, keys());
        double x0 = r.x;
        s.runRealtime(1.0, keys(KeyEvent.VK_W));
        check("analogico para frente move o robo (potencia nas 4 rodas)", r.x > x0 + 20 && Math.abs(r.y - 30) < 3);
        double pinX = SimPinpoint.posX("INCH");
        check("Pinpoint reflete o deslocamento (origem na largada, X para frente)", Math.abs(pinX - (r.x - 30)) < 4);
        s.runRealtime(0.5, keys());
        double h0 = r.h;
        s.runRealtime(0.6, keys(KeyEvent.VK_E));
        check("analogico direito gira o robo (E = horario)", wrap(r.h - h0) < -0.5);
        double heading = SimPinpoint.heading("DEGREES");
        check("heading do Pinpoint acompanha o giro", Math.abs(wrap(Math.toRadians(heading)) - wrap(r.h)) < 0.3);

        double torre0 = r.turret;
        s.runRealtime(0.5, keys(KeyEvent.VK_RIGHT));
        check("torre gira em malha aberta com setPower e o encoder acompanha", r.turret > torre0 + 0.3 && Math.abs(s.hw.torre.position) > 10);

        s.runRealtime(2.5, keys(KeyEvent.VK_J));
        double expect = new SdkParams().exitSpeedFromTicks(1210);
        check("shooter setVelocity(1210) chega perto da velocidade alvo", r.shooterSpeed > 0.9 * expect && s.hw.shooter.velocity > 0.9 * 1210);

        int held0 = r.held.size();
        s.runRealtime(1.0, keys(KeyEvent.VK_J, KeyEvent.VK_K));
        check("transfer empurra o Pollen para o shooter (bolas lancadas)", r.held.size() < held0);

        // intake coleta Pollen e nao coleta Nectar
        Match m2 = freeMatch(30, 30, 0);
        Robot r2 = m2.robots.get(0);
        r2.held.clear();
        Ball p = m2.spawn(Ball.Type.POLLEN); p.x = 30 + r2.length / 2 + 14; p.y = 30;
        Ball n = m2.spawn(Ball.Type.NECTAR_RED); n.x = 30 + r2.length / 2 + 14; n.y = 40;
        CodeSession s2 = new CodeSession(m2, r2, new biobuzz.sdk.demo.DemoTeleOp(), new SdkParams());
        s2.begin(); s2.pressStart();
        s2.runRealtime(0.4, keys());
        s2.runRealtime(1.0, keys(KeyEvent.VK_W, KeyEvent.VK_L));
        check("intake (potencia > 0) coleta o Pollen pelo codigo", r2.held.contains(p));
        check("o Nectar nao e coletado", !r2.held.contains(n));
        s2.stop();

        // servos do anexo
        Match m3 = freeMatch(96, 8.78 + Layout.ATTACH_REACH + 2, -Math.PI / 2);
        Robot r3 = m3.robots.get(0);
        CodeSession s3 = new CodeSession(m3, r3, new biobuzz.sdk.demo.DemoTeleOp(), new SdkParams());
        s3.begin(); s3.pressStart();
        s3.runRealtime(0.4, keys());
        s3.runRealtime(1.0, keys(KeyEvent.VK_UP));
        check("os dois servos (setPosition 1) projetam o anexo", r3.attachDeployed());
        int st = m3.flowers[0].stack.size();
        s3.runRealtime(1.5, keys(KeyEvent.VK_UP));
        check("com os servos em 1 o anexo derruba o Pollen da FLOWER (sem precisar recolher)", m3.flowers[0].stack.size() < st);

        // controle fisico (simulado por injecao): prioridade sobre o teclado e mapeamento FTC
        Match mc = freeMatch(30, 30, 0);
        Robot rcc = mc.robots.get(0);
        CodeSession sc = new CodeSession(mc, rcc, new biobuzz.sdk.demo.DemoTeleOp(), new SdkParams());
        sc.begin();
        PadState pad = new PadState();
        ControllerInput.inject(0, pad);
        sc.runRealtime(0.3, keys());
        pad.start = true;
        sc.runRealtime(0.4, keys());
        check("START do controle inicia o OpMode", sc.runner.isStarted());
        pad.start = false;
        double cx0 = rcc.x;
        pad.ly = -1f;                                      // stick para cima = frente (convencao do FTC)
        sc.runRealtime(1.0, keys());
        check("controle: stick esquerdo para cima move o robo para frente", rcc.x > cx0 + 20);
        pad.ly = 0f; pad.a = true;
        sc.runRealtime(2.5, keys());
        check("controle: botao A liga o shooter", rcc.shooterSpeed > 0.9 * new SdkParams().exitSpeedFromTicks(1210));
        pad.a = false;
        double cx1 = rcc.x;
        sc.runRealtime(1.0, keys(KeyEvent.VK_W));
        check("com controle conectado ele tem prioridade sobre o teclado", Math.abs(rcc.x - cx1) < 5);
        biobuzz.sdk.PadState tiny = new PadState(); tiny.lx = 0.03f;
        com.qualcomm.robotcore.hardware.Gamepad gg = new com.qualcomm.robotcore.hardware.Gamepad();
        GamepadMapper.apply(tiny, gg);
        check("zona morta de 5% nos sticks", gg.left_stick_x == 0f);
        ControllerInput.inject(0, null);
        sc.stop();
        ControllerInput.poll();
        check("sem LWJGL o controle fica indisponivel sem quebrar (status: " + ControllerInput.status() + ")", ControllerInput.status() != null);
        s3.stop();
        s.stop();
        check("OpMode de exemplo terminou sem erro", s.runner.error == null);

        // ---------- TeleOp original da equipe (se houver o caminho do TeamCode) ----------
        String tc = System.getProperty("biobuzz.teamcode");
        if (tc != null) {
            File src = new File(tc);
            File out = File.createTempFile("teamcode", "");
            out.delete(); out.mkdirs();
            List<String> files = new ArrayList<>();
            try (java.util.stream.Stream<java.nio.file.Path> w = java.nio.file.Files.walk(src.toPath())) {
                w.filter(x -> x.toString().endsWith(".java")).forEach(x -> files.add(x.toString()));
            }
            JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
            List<String> cargs = new ArrayList<>(Arrays.asList("-proc:none", "-nowarn", "-encoding", "UTF-8", "-source", "11", "-target", "11",
                "-cp", System.getProperty("java.class.path"), "-d", out.getAbsolutePath()));
            cargs.addAll(files);
            int rc = jc.run(null, null, null, cargs.toArray(new String[0]));
            check("TeamCode original compila sem alteracoes (" + files.size() + " arquivos)", rc == 0);
            List<String> ops = TeamCodeLoader.list(out);
            System.out.println("OpModes encontrados: " + ops);
            String teleop = ops.stream().filter(x -> x.endsWith(".TeleOpAzul")).findFirst().orElse(null);
            check("TeleOpAzul esta entre os OpModes", teleop != null);
            if (teleop != null) {
                Match mt = freeMatch(60, 20, Math.PI / 2);
                Robot rt = mt.robots.get(0);
                OpMode op = TeamCodeLoader.instantiate(out, teleop);
                CodeSession st2 = new CodeSession(mt, rt, op, new SdkParams());
                st2.begin();
                st2.runRealtime(0.6, keys());
                st2.pressStart();
                st2.runRealtime(0.5, keys());
                check("TeleOpAzul original roda no simulador sem excecao (init + start)", st2.runner.error == null && !st2.runner.finished);
                double rx0 = rt.x, ry0 = rt.y;
                st2.runRealtime(1.2, keys(KeyEvent.VK_W));
                double moved = Math.hypot(rt.x - rx0, rt.y - ry0);
                System.out.printf("TeleOpAzul: com W o robo andou %.1f in%n", moved);
                check("TeleOpAzul reage ao analogico (robo se move)", moved > 5);
                System.out.println("telemetria do TeleOpAzul:");
                for (String l : SimTelemetry.lines()) System.out.println("   " + l);
                check("TeleOpAzul publica telemetria", !SimTelemetry.lines().isEmpty());
                for (int k : new int[] {KeyEvent.VK_J, KeyEvent.VK_K, KeyEvent.VK_L, KeyEvent.VK_I}) {
                    st2.runRealtime(0.8, keys(k));
                    System.out.printf("  tecla %s: shooter=%.0f ticks/s intake=%.2f transfer=%.2f torre=%.2f%n", KeyEvent.getKeyText(k),
                        st2.hw.shooter.targetVelocity, st2.hw.intake.power, st2.hw.transfer.power, st2.hw.torre.power);
                }
                st2.runRealtime(0.3, keys());
                st2.stop();
                check("TeleOpAzul terminou sem erro", st2.runner.error == null);

                // giro (Q/E), dpad (heading hold) e field centric do TeleOpAzul original
                Match mg = freeMatch(30, 30, 0);
                Robot rg = mg.robots.get(0);
                CodeSession sg = new CodeSession(mg, rg, TeamCodeLoader.instantiate(out, teleop), new SdkParams());
                sg.begin(); sg.runRealtime(0.6, keys()); sg.pressStart(); sg.runRealtime(0.5, keys());
                double g0 = rg.h;
                sg.runRealtime(0.8, keys(KeyEvent.VK_E));
                check("TeleOpAzul: E (stick direito para a direita) gira o robo no sentido horario", wrap(rg.h - g0) < -0.4);
                g0 = rg.h;
                sg.runRealtime(0.8, keys(KeyEvent.VK_Q));
                check("TeleOpAzul: Q gira no sentido anti-horario", wrap(rg.h - g0) > 0.4);
                sg.runRealtime(3.0, keys(KeyEvent.VK_LEFT));
                check("TeleOpAzul: dpad esquerda leva o heading a +90 graus", Math.abs(wrap(rg.h - Math.PI / 2)) < 0.2);
                sg.runRealtime(3.0, keys(KeyEvent.VK_RIGHT));
                check("TeleOpAzul: dpad direita leva o heading a -90 graus", Math.abs(wrap(rg.h + Math.PI / 2)) < 0.2);
                sg.runRealtime(3.0, keys(KeyEvent.VK_DOWN));
                check("TeleOpAzul: dpad baixo leva o heading a 180 graus", Math.abs(Math.abs(wrap(rg.h)) - Math.PI) < 0.2);
                sg.runRealtime(3.0, keys(KeyEvent.VK_UP));
                check("TeleOpAzul: dpad cima leva o heading a 0 grau", Math.abs(wrap(rg.h)) < 0.2);
                sg.stop();
                for (int variant = 0; variant < 2; variant++) {
                    Match mf = freeMatch(30, 30, 0);
                    Robot rf2 = mf.robots.get(0);
                    CodeSession sf = new CodeSession(mf, rf2, TeamCodeLoader.instantiate(out, teleop), new SdkParams());
                    sf.begin(); sf.runRealtime(0.6, keys()); sf.pressStart(); sf.runRealtime(0.5, keys());
                    if (variant == 1) sf.runRealtime(3.0, keys(KeyEvent.VK_LEFT));     // robo virado para +90 graus
                    double fx = rf2.x, fy = rf2.y;
                    sf.runRealtime(0.5, keys(KeyEvent.VK_D));
                    double dxD = rf2.x - fx, dyD = rf2.y - fy;
                    sf.runRealtime(0.5, keys());
                    fx = rf2.x; fy = rf2.y;
                    sf.runRealtime(0.5, keys(KeyEvent.VK_W));
                    double dxW = rf2.x - fx, dyW = rf2.y - fy;
                    String tag = variant == 0 ? "heading 0" : "heading 90";
                    check("TeleOpAzul field centric (" + tag + "): D move o robo para +X do campo", dxD > 10 && Math.abs(dyD) < 0.4 * dxD);
                    check("TeleOpAzul field centric (" + tag + "): W move o robo para +Y do campo", dyW > 10 && Math.abs(dxW) < 0.4 * dyW);
                    sf.stop();
                }
                // com a disposicao padrao (superior = frente) a mistura de giro do TeleOp nao gira o robo
                Match ms2 = freeMatch(30, 30, 0);
                Robot rs2 = ms2.robots.get(0);
                SdkParams std = new SdkParams();
                std.wheelLayout = SdkParams.WheelLayout.STANDARD;
                CodeSession ss2 = new CodeSession(ms2, rs2, TeamCodeLoader.instantiate(out, teleop), std);
                ss2.begin(); ss2.runRealtime(0.6, keys()); ss2.pressStart(); ss2.runRealtime(0.5, keys());
                double s0 = rs2.h;
                ss2.runRealtime(1.0, keys(KeyEvent.VK_E));
                check("disposicao padrao das rodas: o giro do TeleOp nao faz o robo girar (por isso o padrao e ATLAS_SIDES)", Math.abs(wrap(rs2.h - s0)) < 0.05);
                ss2.stop();
            }
        } else {
            System.out.println("(TeleOp original ignorado: informe -Dbiobuzz.teamcode=<TeamCode/src/main/java>)");
        }
        System.out.println(fails == 0 ? "\nTodos os testes da camada SDK passaram." : "\n" + fails + " teste(s) falharam.");
        System.exit(fails == 0 ? 0 : 1);
    }
}
