package biobuzz;

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
        m.addRobot("R1", Alliance.RED, 9, 70, 0);
        m.addRobot("R2", Alliance.RED, 9, 30, 0);
        m.addRobot("B1", Alliance.BLUE, 135, 100, Math.PI);
        m.addRobot("B2", Alliance.BLUE, 135, 70, Math.PI);
        m.stage();
        return m;
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
        r1.setPose(9, 105, 0);
        m.setController(r1, (mm, r) -> r.drive(Geo.clamp((12.5 - r.x) * 0.5, 0, 0.3), 0, 0));
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
        m.setController(r1, (mm, r) -> { if (r.shooterReady()) r.shoot(); });
        m.runUntil(10);
        check("HIVE vermelha tombou no AUTO", red.tipsAuto >= 1 && red.up == 1);
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

        System.out.println(fails == 0 ? "\nTodos os testes passaram." : "\n" + fails + " teste(s) falharam.");
        System.exit(fails == 0 ? 0 : 1);
    }
}
