package biobuzz;

import java.io.File;

public final class SelfTest {
    static int fails = 0;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
        if (!ok) fails++;
    }

    static Robot plain(Robot r) {
        r.length = 18; r.width = 18; r.turretOffX = 0; r.muzzleZ = 14;
        return r;
    }

    static Match idleMatch() {
        Match m = new Match();
        m.addRobot("R1", Alliance.RED, 0, 70, 0).x = Layout.AGGRON_Y_MM * Layout.MM / 2;
        m.addRobot("R2", Alliance.RED, 0, 30, 0).x = Layout.AGGRON_Y_MM * Layout.MM / 2;
        m.addRobot("B1", Alliance.BLUE, 0, 100, Math.PI).x = Layout.FIELD - Layout.AGGRON_Y_MM * Layout.MM / 2;
        m.addRobot("B2", Alliance.BLUE, 0, 70, Math.PI).x = Layout.FIELD - Layout.AGGRON_Y_MM * Layout.MM / 2;
        m.stage();
        return m;
    }


    static boolean inside(Ball b, Robot r) {
        double[] l = r.toLocal(b.x, b.y);
        return Math.abs(l[0]) < r.length / 2 && Math.abs(l[1]) < r.width / 2 && b.z - b.r < r.height;
    }

    /** Partida com um unico robo vermelho (so elementos do campo), sem bolas seguradas. */
    static Match solo(Robot[] out, double x, double y, double h) {
        Match m = new Match();
        out[0] = m.addRobot("S", Alliance.RED, x, y, h);
        m.stage();
        out[0].held.clear();
        return out[0] == null ? m : m;
    }


    /** Deposita diretamente elementos na CELL voltada para cima da HIVE vermelha e diz se ela virou. */
    static boolean tipsWith(int pollen, int nectar) {
        boolean old = Layout.TIP_COUNT_STAGED_NECTAR;
        Layout.TIP_COUNT_STAGED_NECTAR = false;       // limiares exatos: so os elementos lancados contam
        try { return tipsWithMatch(pollen, nectar, idleMatch()); } finally { Layout.TIP_COUNT_STAGED_NECTAR = old; }
    }

    /** Igual, mas contando os 3 Nectar iniciais da CELL (configuracao padrao). */
    static boolean tipsWithStaged(int pollen, int nectar) {
        boolean old = Layout.TIP_COUNT_STAGED_NECTAR;
        Layout.TIP_COUNT_STAGED_NECTAR = true;
        try { return tipsWithMatch(pollen, nectar, idleMatch()); } finally { Layout.TIP_COUNT_STAGED_NECTAR = old; }
    }

    static boolean tipsWithMatch(int pollen, int nectar, Match m) {
        Hive h = m.redHive;
        for (int i = 0; i < pollen; i++) m.deposit(h, m.spawn(Ball.Type.POLLEN));
        for (int i = 0; i < nectar; i++) m.deposit(h, m.spawn(Ball.Type.NECTAR_RED));
        return h.tips() > 0;
    }

    static void finish(Match m) {
        m.runUntil(Layout.MATCH_END);
        m.settle();
    }

    public static void main(String[] args) {
        // 1. Partida parada: so pontuam GARDEN (4) e Nectar restante na CELL (3 x 2)
        Match m = idleMatch();
        long pollen = m.balls.stream().filter(b -> b.type == Ball.Type.POLLEN).count();
        long nectar = m.balls.stream().filter(Ball::isNectar).count();
        check("40 POLLEN e 16 NECTAR em jogo", pollen == 40 && nectar == 16);
        finish(m);
        Match.Score[] s = m.score();
        check("partida parada: 10 x 10 e empate (RP 1)", s[0].total() == 10 && s[1].total() == 10 && s[0].rp() == 1);

        // 2. LEAVE + PARK no AUTO
        m = idleMatch();
        Robot r1 = m.robots.get(0);
        r1.setPose(r1.length / 2, 105, 0);
        m.setController(r1, (mm, r) -> r.drive(Geo.clamp((r.length / 2 + 3.5 - r.x) * 0.5, 0, 0.3), 0, 0));
        finish(m);
        s = m.score();
        check("AUTO: LEAVE (3) + PARK (5) = 8", s[0].auto() == 8);
        check("PARK no fim do TELEOP conta (+5)", s[0].parkTele == 1);

        // 3. Lancamento na CELL tomba a HIVE (+20 no AUTO) e libera Nectar
        m = idleMatch();
        r1 = plain(m.robots.get(0));
        r1.setPose(54, 36, Math.PI / 2);
        r1.shootElev = Math.toRadians(60);
        r1.speedNoise = 0; r1.shotSpeedDrop = 0;
        Hive red = m.redHive;
        double v = Ballistics.speedFor(red.mouthY(red.up) - r1.y, Layout.CELL_Z - r1.muzzleZ, r1.shootElev);
        r1.shooterTarget = v;
        for (int i = 0; i < 4; i++) { Ball extra = m.spawn(Ball.Type.POLLEN); extra.state = Ball.State.HELD; r1.held.add(extra); }
        m.setController(r1, (mm, r) -> { if (r.shooterReady()) r.shoot(); });
        m.runUntil(12);
        check("HIVE vermelha tombou no AUTO com 8 Pollen lancados", red.tipsAuto >= 1 && red.up == 1);
        check("TIP libera um NECTAR para a alianca", m.canEnterNectar(Alliance.RED) && m.enterNectar(Alliance.RED));
        check("AUTO vale pelo menos 20 pontos", m.score()[0].auto() >= 20);

        // 4. Lob na FLOWER de baixo
        m = idleMatch();
        r1 = plain(m.robots.get(0));
        r1.setPose(96, 30, -Math.PI / 2);
        r1.shootElev = Math.toRadians(70);
        r1.speedNoise = 0; r1.shotSpeedDrop = 0;
        Flower fl = m.flowers[0];
        r1.shooterTarget = Ballistics.speedFor(Math.hypot(fl.ox - r1.x, fl.oy - r1.y),
            Layout.FLOWER_OPEN_Z - r1.muzzleZ, r1.shootElev);
        m.setController(r1, (mm, r) -> { if (r.shooterReady() && r.held.size() == 4) r.shoot(); });
        m.runUntil(6);
        check("bola lancada entrou na FLOWER (5 elementos)", fl.stack.size() == 5);

        // 5. Dono da FLOWER e bonus de Nectar mais baixo
        m = idleMatch();
        Ball n = m.spawn(Ball.Type.NECTAR_BLUE);
        n.state = Ball.State.FLOWER;
        m.flowers[0].stack.add(n);
        m.flowers[0].relayout();
        finish(m);
        s = m.score();
        check("FLOWER: azul ganha 4 x 2 + 5 = 13 (total 23)", s[1].total() == 23 && s[0].total() == 10);

        // 6. Colisoes: parede e entre robos
        m = idleMatch();
        r1 = m.robots.get(0);
        m.setController(r1, (mm, r) -> r.drive(0, 0, 0));
        m.robots.get(0).drive(-1, 0, 0);
        m.setController(r1, (mm, r) -> r.drive(-1, 0, 0));
        m.runUntil(5);
        check("robo nao atravessa a parede", r1.x - r1.length / 2 >= -1e-6);
        m = new Match();
        Robot a = m.addRobot("A", Alliance.RED, 20, 100, 0), b = m.addRobot("B", Alliance.BLUE, 70, 100, Math.PI);
        m.stage();
        m.setController(a, (mm, r) -> r.drive(1, 0, 0));
        m.setController(b, (mm, r) -> r.drive(1, 0, 0));
        m.runUntil(4);
        check("robos nao se sobrepoem",
            Geo.sat(a.x, a.y, a.h, a.length / 2, a.width / 2, b.x, b.y, b.h, b.length / 2, b.width / 2) == null);


        // ---------- Melhorias: dimensoes, robo x elementos, HIVE, intake ----------
        Robot[] h = new Robot[1];
        Robot ag = new Robot("A", Alliance.RED);
        check("AGGRON: largura 425 mm, comprimento 446 mm, altura 280 mm",
            Math.abs(ag.width / Layout.MM - 425) < 1e-6 && Math.abs(ag.length / Layout.MM - 446) < 1e-6
                && Math.abs(ag.height / Layout.MM - 280) < 1e-6);

        // regressao: fisica elemento x elemento identica a versao anterior (valores gerados pelo codigo antigo)
        m = new Match(); m.stage();
        Ball[] g = new Ball[5];
        for (int i = 0; i < 5; i++) g[i] = m.spawn(i == 3 ? Ball.Type.NECTAR_RED : Ball.Type.POLLEN);
        g[0].x = 20; g[0].y = 100; g[0].vx = 70; g[0].vy = 5;
        g[1].x = 50; g[1].y = 101; g[2].x = 52; g[2].y = 100;
        g[3].x = 100; g[3].y = 110; g[3].vx = -40; g[3].vy = -10; g[3].vz = 60; g[3].z = 20;
        g[4].x = 70; g[4].y = 108; g[4].vy = -30;
        m.runUntil(4.0);
        double[][] gold = {{64.339660267, 118.159249495}, {43.471315833, 115.288217767}, {84.955315767, 83.475625539},
            {27.816558373, 78.043623758}, {70.0, 78.075}};
        boolean same = true;
        for (int i = 0; i < 5; i++) same &= Math.abs(g[i].x - gold[i][0]) < 1e-6 && Math.abs(g[i].y - gold[i][1]) < 1e-6;
        check("regressao: fisica entre elementos preservada (5 bolas, 4 s)", same);

        // robo empurra Pollen sem atravessar
        m = solo(h, 20, 30, 0);
        Ball pl = m.spawn(Ball.Type.POLLEN); pl.x = 20 + h[0].length / 2 + 6; pl.y = 30;
        double px0 = pl.x;
        boolean[] overlap = {false};
        m.setController(h[0], (mm, r) -> { r.drive(0.6, 0, 0); if (inside(pl, r)) overlap[0] = true; });
        m.runUntil(1.2);
        check("robo empurra o Pollen para frente", pl.x > px0 + 10 && pl.state == Ball.State.FIELD);
        check("Pollen nunca fica dentro do robo", !overlap[0]);

        // robo empurra Nectar com intake ligado: nao coleta, so empurra
        m = solo(h, 20, 30, 0);
        Ball ne = m.spawn(Ball.Type.NECTAR_RED); ne.x = 20 + h[0].length / 2 + 6; ne.y = 30;
        double nx0 = ne.x;
        boolean[] ov2 = {false};
        h[0].intakeOn = true;
        m.setController(h[0], (mm, r) -> { r.drive(0.6, 0, 0); if (inside(ne, r)) ov2[0] = true; });
        m.runUntil(1.2);
        check("Nectar nao e coletado pelo intake", h[0].held.isEmpty() && ne.state == Ball.State.FIELD);
        check("Nectar e empurrado e nunca atravessa o robo", ne.x > nx0 + 10 && !ov2[0]);

        // intake continua coletando Pollen
        m = solo(h, 20, 30, 0);
        Ball pc = m.spawn(Ball.Type.POLLEN); pc.x = 20 + h[0].length / 2 + 4; pc.y = 30;
        h[0].intakeOn = true;
        m.setController(h[0], (mm, r) -> r.drive(0.3, 0, 0));
        m.runUntil(1.5);
        check("intake coleta Pollen", h[0].held.contains(pc));

        // o intake encostado na FLOWER nao retira Pollen (so o anexo retira)
        m = solo(h, 96, 0, -Math.PI / 2);
        h[0].y = h[0].length / 2;
        h[0].intakeOn = true;
        m.runUntil(1.5);
        check("intake sozinho nao retira Pollen da FLOWER", h[0].held.isEmpty() && m.flowers[0].stack.size() == 4);

        // robo prensa a bola contra a parede: robo e empurrado de volta, bola nao atravessa
        m = solo(h, 110, 30, 0);
        Ball wb = m.spawn(Ball.Type.POLLEN); wb.x = Layout.FIELD - wb.r; wb.y = 30;
        m.setController(h[0], (mm, r) -> r.drive(0.6, 0, 0));
        m.runUntil(2.5);
        check("bola prensada na parede: robo para antes de atravessa-la",
            h[0].x + h[0].length / 2 <= wb.x - wb.r + 0.2 && !inside(wb, h[0]));

        // reacao sobre o robo: robo leve atingido por uma bola
        m = solo(h, 60, 20, 0);
        h[0].mass = 0.05;
        Ball hit = m.spawn(Ball.Type.POLLEN); hit.x = 60 + h[0].length / 2 + 2.0; hit.y = 20; hit.vx = -100;
        m.run(0.05);
        check("robo sofre a reacao do impacto (robo leve recua)", h[0].vx < -10);
        m = solo(h, 60, 20, 0);
        Ball hit2 = m.spawn(Ball.Type.POLLEN); hit2.x = 60 + h[0].length / 2 + 2.0; hit2.y = 20; hit2.vx = -100;
        m.run(0.05);
        check("robo com a massa real quase nao sente uma bola", Math.abs(h[0].vx) < 1.0);

        // a bola lancada nao colide com o proprio robo
        m = solo(h, 30, 30, 0);
        Robot sh = h[0];
        Ball shot = m.spawn(Ball.Type.POLLEN); shot.state = Ball.State.HELD; sh.held.add(shot);
        sh.speedNoise = 0; sh.shooterTarget = 150; sh.shootElev = Math.toRadians(55);
        m.setController(sh, (mm, r) -> { if (r.shooterReady()) r.shoot(); });
        m.runUntil(2.5);   // o shooter leva ~1,4 s para chegar a 97% da velocidade
        check("bola lancada sai do robo sem colidir com ele", shot.state == Ball.State.FIELD && shot.x > 45 && shot.ignoreRobot == null);

        // HIVE: geometria de colisao separada e regioes livres
        long baixos = HiveGeometry.OBSTACLES.stream().filter(o -> o.blocks(ag.height)).count();
        check("HIVE: so 2 barras de base bloqueiam um robo baixo", baixos == 2);
        check("HIVE: centro e livre, barra bloqueia, parte alta bloqueia so robo alto",
            !HiveGeometry.blocked(72, 72, ag.height) && HiveGeometry.blocked(48.3, 72, ag.height)
                && HiveGeometry.blocked(72, 72, 30));

        m = solo(h, 72, 30, Math.PI / 2);
        m.setController(h[0], (mm, r) -> r.drive(1, 0, 0));
        m.runUntil(3.5);
        check("robo atravessa por baixo da HIVE pelo centro", h[0].y > 100);

        m = solo(h, 40, 30, Math.PI / 2);
        m.setController(h[0], (mm, r) -> r.drive(1, 0, 0));
        m.runUntil(3.5);
        check("robo e bloqueado pela barra de base da HIVE", h[0].y < 44.5);

        m = solo(h, 30, 72, 0);
        m.setController(h[0], (mm, r) -> r.drive(1, 0, 0));
        m.runUntil(3.5);
        check("robo nao cruza a barra de base de lado", h[0].x + h[0].length / 2 <= 47.3);

        m = solo(h, 72, 30, Math.PI / 2);
        h[0].height = 30;
        m.setController(h[0], (mm, r) -> r.drive(1, 0, 0));
        m.runUntil(3.5);
        check("robo alto (30 in) e bloqueado pela parte movel da HIVE", h[0].y < 44.5);

        // ---------- Condicoes para virar a HIVE: 8 Pollen, 5 Nectar ou 3 Nectar + 3 Pollen ----------
        check("HIVE vira com 8 Pollen", tipsWith(8, 0));
        check("HIVE vira com 5 Nectar", tipsWith(0, 5));
        check("HIVE vira com 3 Nectar + 3 Pollen", tipsWith(3, 3));
        check("HIVE vira acima do minimo (4N+3P, 3N+4P, 9P)", tipsWith(3, 4) && tipsWith(4, 3) && tipsWith(9, 0));
        check("HIVE nao vira com 7 Pollen", !tipsWith(7, 0));
        check("HIVE nao vira com 4 Nectar", !tipsWith(0, 4));
        check("HIVE nao vira com 3 Nectar + 2 Pollen nem 2 Nectar + 3 Pollen", !tipsWith(2, 3) && !tipsWith(3, 2));
        check("HIVE nao vira com combinacoes abaixo (7P+2N, 2P+4N, 1P+4N)", !tipsWith(7, 2) && !tipsWith(2, 4) && !tipsWith(1, 4));
        Layout.TIP_COUNT_STAGED_NECTAR = false;
        m = idleMatch();
        for (int i = 0; i < 7; i++) m.deposit(m.redHive, m.spawn(Ball.Type.POLLEN));
        boolean antes = m.redHive.tips() == 0 && m.redHive.up == 0;
        m.deposit(m.redHive, m.spawn(Ball.Type.POLLEN));
        check("o 8o Pollen vira a HIVE exatamente nesse momento e zera a contagem",
            antes && m.redHive.tips() == 1 && m.redHive.up == 1 && m.redHive.pollenIn == 0);
        Layout.TIP_COUNT_STAGED_NECTAR = true;
        // com os 3 Nectar iniciais contando (padrao)
        m = idleMatch();
        check("os 3 Nectar iniciais da CELL contam para virar a HIVE (padrao)", m.redHive.nectarIn == 3 && m.redHive.pollenIn == 0);
        check("com os 3 Nectar iniciais: 3 Pollen ja viram a HIVE na primeira vez", tipsWithStaged(3, 0));
        check("com os 3 Nectar iniciais: 2 Pollen nao viram", !tipsWithStaged(2, 0));
        check("com os 3 Nectar iniciais: +2 Nectar lancados (5 no total) viram", tipsWithStaged(0, 2) && !tipsWithStaged(0, 1));
        check("com os 3 Nectar iniciais: 1 Nectar + 2 Pollen nao viram, 1 Nectar + 3 Pollen viram", !tipsWithStaged(2, 1) && tipsWithStaged(3, 1));
        m = idleMatch();
        for (int i = 0; i < 3; i++) m.deposit(m.redHive, m.spawn(Ball.Type.POLLEN));
        boolean primeira = m.redHive.tips() == 1 && m.redHive.nectarIn == 0;
        for (int i = 0; i < 7; i++) m.deposit(m.redHive, m.spawn(Ball.Type.POLLEN));
        boolean semSegunda = m.redHive.tips() == 1;
        m.deposit(m.redHive, m.spawn(Ball.Type.POLLEN));
        check("depois do 1o tip a CELL nova comeca vazia: a 2a vez exige 8 Pollen", primeira && semSegunda && m.redHive.tips() == 2);

        // ---------- Menu, posicoes iniciais, modo livre e IA ----------
        double L = ag.length, W = ag.width;
        boolean allLegal = true;
        for (Alliance al0 : Alliance.values())
            for (double[] c : StartRules.candidates(al0)) allLegal &= StartRules.check(c[0], c[1], c[2], L, W, al0) == null;
        check("posicoes iniciais padrao sao legais (G304) nas duas aliancas", allLegal);
        check("G304: robo longe da parede e ilegal", StartRules.check(30, 30, 0, L, W, Alliance.RED) != null);
        check("G304: lado da outra alianca e ilegal", StartRules.check(Layout.FIELD - L / 2, 69, Math.PI, L, W, Alliance.RED) != null);
        check("G304: dentro da LOADING ZONE e ilegal", StartRules.check(L / 2, 105, 0, L, W, Alliance.RED) != null);
        check("G304: tocando a FLOWER e ilegal", StartRules.check(L / 2, 48, 0, L, W, Alliance.RED) != null);
        check("G304: fora do campo e ilegal", StartRules.check(-5, 70, 0, L, W, Alliance.RED) != null);

        SimConfig pcfg = new SimConfig();
        StartPicker pk = new StartPicker(pcfg);
        pk.place(40, 60);
        check("Round: posicao longe da parede nao pode iniciar", !pk.canStart() && pk.status() != null);
        pk.place(5, 75);
        check("Round: clique perto da parede encosta o robo e fica valido", pk.canStart() && Math.abs(pcfg.startX - L / 2) < 1e-6);
        pk.rotate(Math.PI / 2);
        check("girar o robo mantem o contato com a parede (x = meia largura)", pk.canStart() && Math.abs(pcfg.startX - W / 2) < 1e-6);
        pk.place(-20, 200);
        check("posicao fora do campo e trazida para dentro", StartRules.check(pcfg.startX, pcfg.startY, pcfg.startH, L, W, pcfg.alliance) != null
            ? true : pcfg.startY <= Layout.FIELD);
        pcfg.mode = SimConfig.Mode.CODE_TEST;
        pk.place(72, 72);
        check("Teste de codigo aceita qualquer posicao (sem G304)", pk.canStart() && pcfg.startX == 72 && pcfg.startY == 72);
        pcfg.mode = SimConfig.Mode.ROUND; pcfg.alliance = Alliance.BLUE; pk.refresh(true);
        check("trocar de alianca volta para uma posicao inicial legal do lado azul", pk.canStart() && pcfg.startX > 72);

        SimConfig c1 = new SimConfig();
        Match m1 = Viewer.newMatch(c1);
        check("1 robo: somente o AGGRON, em Round", m1.robots.size() == 1 && m1.robots.get(0).name.equals("AGGRON") && !m1.freePlay);
        check("posicao e orientacao escolhidas sao aplicadas ao AGGRON",
            Math.abs(m1.robots.get(0).x - c1.startX) < 1e-9 && Math.abs(m1.robots.get(0).h - c1.startH) < 1e-9);
        for (Alliance al : Alliance.values()) {
            SimConfig c4 = new SimConfig();
            c4.robots = 4; c4.alliance = al; c4.resetStart();
            Match m4 = Viewer.newMatch(c4);
            int mine = 0, theirs = 0;
            boolean legal = true, free = true;
            for (Robot r : m4.robots) {
                if (r.alliance == al) mine++; else theirs++;
                legal &= StartRules.check(r.x, r.y, r.h, r.length, r.width, r.alliance) == null;
                for (Robot o : m4.robots)
                    if (o != r && Geo.sat(r.x, r.y, r.h, r.length / 2, r.width / 2, o.x, o.y, o.h, o.length / 2, o.width / 2) != null) free = false;
            }
            check("4 robos (" + al + "): 2 por alianca, AGGRON e parceiro na sua alianca", m4.robots.size() == 4 && mine == 2 && theirs == 2
                && m4.robots.get(0).alliance == al && m4.robots.get(0).name.equals("AGGRON"));
            check("4 robos (" + al + "): partidas iniciais legais e sem sobreposicao", legal && free);
        }
        SimConfig cs = new SimConfig();
        cs.robots = 4; cs.startX = 45; cs.startY = L / 2; cs.startH = Math.PI / 2;   // ocupa a 2a posicao padrao do vermelho
        Match ms = Viewer.newMatch(cs);
        boolean noOverlap = true;
        for (Robot r : ms.robots) for (Robot o : ms.robots)
            if (o != r && Geo.sat(r.x, r.y, r.h, r.length / 2, r.width / 2, o.x, o.y, o.h, o.length / 2, o.width / 2) != null) noOverlap = false;
        check("IA nao nasce sobre o AGGRON quando ele ocupa uma posicao padrao", noOverlap);

        // modo livre x Round depois dos 158 s
        SimConfig cf = new SimConfig();
        cf.mode = SimConfig.Mode.CODE_TEST;
        Match mf = Viewer.newMatch(cf);
        Robot rf = mf.robots.get(0);
        double fx0 = rf.x;
        mf.setController(rf, (mm, r) -> r.drive(mm.t >= 160 ? 0.5 : 0, 0, 0));
        mf.runUntil(165);
        check("teste de codigo: sem cronometro, robo continua habilitado apos 158 s", mf.phase() == Match.Phase.TELEOP && rf.x > fx0 + 5);
        Match mr = Viewer.newMatch(new SimConfig());
        Robot rr = mr.robots.get(0);
        double rx0 = rr.x;
        mr.setController(rr, (mm, r) -> r.drive(mm.t >= 160 ? 0.5 : 0, 0, 0));
        mr.runUntil(165);
        check("Round: apos 158 s o robo fica desabilitado", mr.phase() == Match.Phase.ENDED && Math.abs(rr.x - rx0) < 0.2);
        mf.enterNectar(Alliance.RED);
        check("teste de codigo: humano pode colocar Nectar a qualquer momento", mf.nectarInArea(Alliance.RED) == 4);

        // IA: partida completa com o AGGRON parado
        SimConfig ca = new SimConfig();
        ca.robots = 4;
        Match ma = Viewer.newMatch(ca);
        ma.runUntil(Layout.MATCH_END);
        ma.settle();
        Match.Score[] sa = ma.score();
        boolean inside = true, sep = true, noNectar = true;
        for (Robot r : ma.robots) {
            double[] cs4 = Geo.corners(r.x, r.y, r.h, r.length / 2, r.width / 2);
            for (int i = 0; i < 4; i++) inside &= cs4[2 * i] > -0.1 && cs4[2 * i] < 144.1 && cs4[2 * i + 1] > -0.1 && cs4[2 * i + 1] < 144.1;
            for (Ball hb : r.held) noNectar &= !hb.isNectar();
            for (Robot o : ma.robots) {
                if (o == r) continue;
                double[] mt = Geo.sat(r.x, r.y, r.h, r.length / 2, r.width / 2, o.x, o.y, o.h, o.length / 2, o.width / 2);
                if (mt != null && mt[2] > 0.5) sep = false;
            }
        }
        long shots = ma.log.stream().filter(x -> x.contains("entrou na CELL")).count();
        check("IA: partida completa roda, robos ficam no campo e sem sobreposicao", inside && sep);
        check("IA: nenhum robo coleta Nectar", noNectar);
        check("IA: os robos atiram na HIVE e a alianca adversaria vira a HIVE (>= 8 Pollen)", shots >= 8 && ma.blueHive.tips() >= 1 && sa[1].total() > 10);
        check("IA: o parceiro tambem pontua (HIVE vermelha vira com o AGGRON parado)", ma.redHive.tips() >= 1);

        // tela inicial renderiza sem janela
        try {
            File png = File.createTempFile("menu", ".png");
            SimConfig cm = new SimConfig();
            cm.robots = 4;
            Menu.snapshot(png.getAbsolutePath(), cm);
            check("tela inicial desenha a arena e o seletor de posicao", png.length() > 2000);
            png.delete();
        } catch (Exception e) {
            check("tela inicial desenha a arena e o seletor de posicao (" + e + ")", false);
        }

        // ---------- Anexo coletor de Pollen (2 servos, suposicoes temporarias) ----------
        Robot[] at = new Robot[1];
        double tipY = ag.length / 2 + ag.attachReach + 2;   // ponta a 2 in da parede da FLOWER de baixo
        m = solo(at, 96, tipY, -Math.PI / 2);
        at[0].intakeOn = true;
        m.runUntil(0.2);
        at[0].setAttach(1, 1);
        m.run(0.25);
        boolean metade = at[0].attachExt > 0.3 && at[0].attachExt < 0.7;
        m.run(0.4);
        check("os servos movem o anexo gradualmente ate projeta-lo", metade && at[0].attachDeployed() && at[0].attachExt <= 1.0);
        int stackAntes = m.flowers[0].stack.size();
        at[0].deployAttach(false);
        m.run(1.5);
        check("recolher o anexo debaixo da FLOWER tira o Pollen do fundo", m.flowers[0].stack.size() == stackAntes - 1);
        check("o Pollen rola ate o intake e e coletado", at[0].held.size() == 1);
        boolean notInside = true;
        for (Ball ab : m.balls) if (ab.state == Ball.State.FIELD && inside(ab, at[0])) notInside = false;
        check("o Pollen liberado nao fica dentro do robo", notInside);
        at[0].deployAttach(true); m.run(0.8); at[0].deployAttach(false); m.run(1.2);
        check("cada ciclo do anexo tira um Pollen (2o ciclo)", m.flowers[0].stack.size() == stackAntes - 2 && at[0].held.size() == 2);

        m = solo(at, 96, tipY, -Math.PI / 2);
        Ball bn = m.spawn(Ball.Type.NECTAR_BLUE); bn.state = Ball.State.FLOWER;
        m.flowers[0].stack.add(0, bn); m.flowers[0].relayout();
        at[0].deployAttach(true); m.run(1.0); at[0].deployAttach(false); m.run(1.0);
        check("Nectar no fundo da FLOWER nao sai com o anexo", m.flowers[0].stack.get(0) == bn && m.flowers[0].stack.size() == 5);

        m = solo(at, 96, ag.length / 2 + ag.attachReach + 12, -Math.PI / 2);
        at[0].deployAttach(true); m.run(1.0); at[0].deployAttach(false); m.run(1.0);
        check("fora de alcance (ponta a 12 in da parede) o anexo nao retira", m.flowers[0].stack.size() == 4);

        m = solo(at, 60, tipY, -Math.PI / 2);
        at[0].deployAttach(true); m.run(1.0); at[0].deployAttach(false); m.run(1.0);
        check("desalinhado da FLOWER (36 in ao lado) o anexo nao retira", m.flowers[0].stack.size() == 4);

        m = solo(at, 96, tipY, -Math.PI / 2);
        at[0].deployAttach(true); m.run(0.15); at[0].deployAttach(false); m.run(1.0);
        check("recolher stackAntes de projetar por completo nao retira", m.flowers[0].stack.size() == 4);

        m = solo(at, 96, tipY, -Math.PI / 2);
        at[0].setAttach(1, 0); m.run(1.5);
        check("servos desalinhados (1 e 0) nao projetam o anexo por completo", !at[0].attachDeployed());
        at[0].setAttach(1, 1); m.run(1.0);
        check("servos girando juntos projetam o anexo", at[0].attachDeployed());

        System.out.println(fails == 0 ? "\nTodos os testes passaram." : "\n" + fails + " teste(s) falharam.");
        System.exit(fails == 0 ? 0 : 1);
    }
}
