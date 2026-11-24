package biobuzz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Partida BIOBUZZ: campo, fisica das bolas, HIVES, FLOWERS, robos, fases e pontuacao. */
public final class Match {
    public interface Controller { void loop(Match m, Robot r); }

    public enum Phase { PRE, AUTO, TRANSITION, TELEOP, ENDED }

    public static final double DT = 0.005;
    private static final int CONTROL_EVERY = 4; // controladores a 50 Hz

    public double t = 0;
    public final List<Robot> robots = new ArrayList<>();
    public final List<Ball> balls = new ArrayList<>();
    public final Hive redHive = new Hive(Alliance.RED, Layout.RED_HIVE_X, Layout.HIVE_CENTER_Y, 0);
    public final Hive blueHive = new Hive(Alliance.BLUE, Layout.BLUE_HIVE_X, Layout.HIVE_CENTER_Y, 1);
    public final Flower[] flowers = buildFlowers();
    /** Ambiente livre (teste de codigo): sem cronometro nem fases; os robos ficam sempre habilitados. */
    public boolean freePlay = false;
    public final List<String> log = new ArrayList<>();

    // limiares de RP (Tabela 10-3, eventos regulares)
    public int swarmThreshold = 16, pollinator1 = 4, pollinator2 = 7;

    private final Map<Robot, Controller> controllers = new HashMap<>();
    private final List<Ball> areaNectar = new ArrayList<>();
    private final int[] nectarEntered = new int[2];
    private final int[] foulCredit = new int[2];
    private final Random rng = new Random(1);
    private int nextId = 0, stepCount = 0;

    private static Flower[] buildFlowers() {
        Flower[] f = new Flower[Layout.FLOWER_WALL.length];
        for (int i = 0; i < f.length; i++) {
            double[] w = Layout.FLOWER_WALL[i];
            f[i] = new Flower(Layout.FLOWER_NAME[i], w[0], w[1], w[2], w[3]);
        }
        return f;
    }

    public Robot addRobot(String name, Alliance a, double x, double y, double h) {
        Robot r = new Robot(name, a);
        r.setPose(x, y, h);
        robots.add(r);
        return r;
    }

    public void setController(Robot r, Controller c) { controllers.put(r, c); }

    public Hive hive(Alliance a) { return a == Alliance.RED ? redHive : blueHive; }

    public Phase phase() {
        if (freePlay) return Phase.TELEOP;
        if (t < 0) return Phase.PRE;
        if (t < Layout.AUTO_END) return Phase.AUTO;
        if (t < Layout.TELEOP_START) return Phase.TRANSITION;
        if (t < Layout.MATCH_END) return Phase.TELEOP;
        return Phase.ENDED;
    }

    public Ball spawn(Ball.Type type) {
        Ball b = new Ball(nextId++, type);
        balls.add(b);
        return b;
    }

    private static Ball.Type nectarType(Alliance a) {
        return a == Alliance.RED ? Ball.Type.NECTAR_RED : Ball.Type.NECTAR_BLUE;
    }

    /** Posiciona todos os elementos conforme a Figura 10-2 e 10.3.1. Chame depois de adicionar os robos. */
    public void stage() {
        balls.clear(); areaNectar.clear();
        for (Flower f : flowers) {
            f.stack.clear();
            for (int i = 0; i < 4; i++) {
                Ball b = spawn(Ball.Type.POLLEN);
                b.state = Ball.State.FLOWER;
                f.stack.add(b);
            }
            f.relayout();
        }
        double d = 2 * Layout.POLLEN_R;
        for (int i = 0; i < 4; i++) {
            Ball r = spawn(Ball.Type.POLLEN);
            r.x = Layout.POLLEN_R + d * i; r.y = Layout.POLLEN_R;
            Ball b = spawn(Ball.Type.POLLEN);
            b.x = Layout.FIELD - Layout.POLLEN_R - d * i; b.y = Layout.FIELD - Layout.POLLEN_R;
        }
        for (Alliance a : Alliance.values()) {
            int count = 0;
            for (Robot r : robots) {
                if (r.alliance != a) continue;
                count++;
                for (int i = 0; i < 4; i++) {
                    Ball b = spawn(Ball.Type.POLLEN);
                    b.state = Ball.State.HELD;
                    r.held.add(b);
                }
            }
            int missing = Math.max(0, 2 - count), n = 4 * missing;
            double[] lz = a == Alliance.RED ? Layout.RED_LZ : Layout.BLUE_LZ;
            for (int i = 0; i < n; i++) {
                Ball b = spawn(Ball.Type.POLLEN);
                b.x = a == Alliance.RED ? Layout.POLLEN_R : Layout.FIELD - Layout.POLLEN_R;
                b.y = (lz[1] + lz[3]) / 2 + (i - (n - 1) / 2.0) * d;
            }
            Hive h = hive(a);
            h.pollenIn = 0;
            h.nectarIn = Layout.TIP_COUNT_STAGED_NECTAR ? 3 : 0;
            for (int i = 0; i < 3; i++) {
                Ball b = spawn(nectarType(a));
                b.state = Ball.State.CELL;
                h.cell(h.up).add(b);
            }
            for (int i = 0; i < 5; i++) {
                Ball b = spawn(nectarType(a));
                b.state = Ball.State.AREA;
                areaNectar.add(b);
            }
        }
    }

    // ---------- Nectar da area da alianca ----------

    public int nectarInArea(Alliance a) {
        int n = 0;
        for (Ball b : areaNectar) if (b.nectarAlliance() == a && b.state == Ball.State.AREA) n++;
        return n;
    }

    public boolean canEnterNectar(Alliance a) {
        if (nectarInArea(a) == 0) return false;
        return freePlay || t >= Layout.LAST_MINUTE || nectarEntered[a.ordinal()] < hive(a).tips();
    }

    /** Humano da alianca coloca um Nectar em jogo (um por tip, todos no ultimo minuto). */
    public boolean enterNectar(Alliance a) {
        if (!canEnterNectar(a)) return false;
        for (Ball b : areaNectar) {
            if (b.nectarAlliance() == a && b.state == Ball.State.AREA) {
                b.state = Ball.State.FIELD;
                b.x = a == Alliance.RED ? b.r + 0.5 : Layout.FIELD - b.r - 0.5;
                b.y = 72;
                b.vx = a == Alliance.RED ? 30 : -30;
                b.vy = b.vz = 0;
                nectarEntered[a.ordinal()]++;
                return true;
            }
        }
        return false;
    }

    public void foul(Alliance offender, boolean major) {
        foulCredit[offender.opponent().ordinal()] += major ? 20 : 5;
    }

    // ---------- Loop principal ----------

    public void run(double seconds) {
        int steps = (int) Math.round(seconds / DT);
        for (int i = 0; i < steps; i++) {
            if (stepCount % CONTROL_EVERY == 0) {
                for (Robot r : robots) {
                    Controller c = controllers.get(r);
                    if (c != null) c.loop(this, r);
                }
            }
            step();
        }
    }

    /** Roda ate a hora t (segundos de partida). */
    public void runUntil(double time) {
        if (time > t) run(time - t);
    }

    /** Depois do fim, deixa robos e elementos pararem antes de contar (conforme 10.5). */
    public void settle() { run(6.0); }

    public void step() {
        Phase before = phase();
        boolean enabled = before == Phase.AUTO || before == Phase.TELEOP;
        for (Robot r : robots) r.update(DT, enabled);
        collideRobots();
        if (enabled) {
            for (Robot r : robots) { intake(r); fire(r); attachment(r); }
        } else {
            for (Robot r : robots) r.shootRequest = false;
        }
        stepBalls();
        t += DT;
        stepCount++;
        if (before == Phase.AUTO && phase() != Phase.AUTO) {
            for (Robot r : robots) { r.leftInAuto = isOffWall(r); r.parkedInAuto = isParked(r); }
        }
    }

    // ---------- Robos ----------

    private void collideRobots() {
        double hl, hw;
        for (Robot r : robots) {
            double[] p = Geo.corners(r.x, r.y, r.h, r.length / 2, r.width / 2);
            double minX = 1e9, maxX = -1e9, minY = 1e9, maxY = -1e9;
            for (int i = 0; i < 4; i++) {
                minX = Math.min(minX, p[2 * i]); maxX = Math.max(maxX, p[2 * i]);
                minY = Math.min(minY, p[2 * i + 1]); maxY = Math.max(maxY, p[2 * i + 1]);
            }
            if (minX < 0) { r.x -= minX; if (r.vx < 0) r.vx = 0; }
            if (maxX > Layout.FIELD) { r.x -= maxX - Layout.FIELD; if (r.vx > 0) r.vx = 0; }
            if (minY < 0) { r.y -= minY; if (r.vy < 0) r.vy = 0; }
            if (maxY > Layout.FIELD) { r.y -= maxY - Layout.FIELD; if (r.vy > 0) r.vy = 0; }
            for (HiveGeometry.Obstacle o : HiveGeometry.OBSTACLES) {
                if (!o.blocks(r.height)) continue;
                double[] m = Geo.sat(r.x, r.y, r.h, r.length / 2, r.width / 2, o.cx(), o.cy(), 0, o.hw(), o.hh());
                if (m != null) {
                    r.x += m[0] * m[2]; r.y += m[1] * m[2];
                    double dot = r.vx * m[0] + r.vy * m[1];
                    if (dot < 0) { r.vx -= dot * m[0]; r.vy -= dot * m[1]; }
                }
            }
        }
        for (int i = 0; i < robots.size(); i++) {
            for (int j = i + 1; j < robots.size(); j++) {
                Robot a = robots.get(i), b = robots.get(j);
                double[] m = Geo.sat(a.x, a.y, a.h, a.length / 2, a.width / 2, b.x, b.y, b.h, b.length / 2, b.width / 2);
                if (m == null) continue;
                a.x += m[0] * m[2] / 2; a.y += m[1] * m[2] / 2;
                b.x -= m[0] * m[2] / 2; b.y -= m[1] * m[2] / 2;
                double rel = (a.vx - b.vx) * m[0] + (a.vy - b.vy) * m[1];
                if (rel < 0) {
                    a.vx -= rel / 2 * m[0]; a.vy -= rel / 2 * m[1];
                    b.vx += rel / 2 * m[0]; b.vy += rel / 2 * m[1];
                }
            }
        }
    }

    private void take(Robot r, Ball b) {
        if (!r.canIntake(b)) return;
        b.state = Ball.State.HELD;
        b.vx = b.vy = b.vz = 0;
        r.held.add(b);
        if (b.isNectar() && b.nectarAlliance() != r.alliance)
            log.add(String.format("t=%.1f %s controlou NECTAR do oponente (G408)", t, r.name));
        if (r.held.size() > 4)
            log.add(String.format("t=%.1f %s controla mais de 4 elementos (G407)", t, r.name));
    }

    private void intake(Robot r) {
        if (!r.intakeOn || r.held.size() >= r.capacity) return;
        double hl = r.length / 2, hw = (r.width - 2) / 2;
        for (Ball b : balls) {
            if (b.state != Ball.State.FIELD || b.z > b.r + 1.0) continue;
            if (!r.canIntake(b)) continue;
            double[] l = r.toLocal(b.x, b.y);
            if (l[0] > hl - 1 && l[0] < hl + r.intakeReach && Math.abs(l[1]) < hw) {
                take(r, b);
                if (r.held.size() >= r.capacity) return;
            }
        }
        for (Flower f : flowers) {
            if (!r.intakeRetrievesFlower || f.stack.isEmpty() || f.cooldown > 0 || !r.canIntake(f.stack.get(0))) continue;
            double[] l = r.toLocal(f.wallX, f.wallY);
            if (l[0] > hl - 1.5 && l[0] < hl + r.intakeReach + 0.5 && Math.abs(l[1]) < hw) {
                Ball b = f.stack.remove(0);
                f.relayout();
                f.cooldown = 0.4;
                take(r, b);
                if (r.held.size() >= r.capacity) return;
            }
        }
    }

    /**
     * Anexo coletor (passivo): enquanto estiver projetado por baixo da FLOWER e bem posicionado, derruba o Pollen do fundo
     * da pilha, um a cada ATTACH_DROP_INTERVAL, ate esvaziar. O Pollen sai pela abertura de retirada e rola em direcao ao
     * robo (o intake o pega). O Nectar e grande demais para sair e bloqueia a pilha. Nao precisa recolher o anexo.
     */
    private void attachment(Robot r) {
        if (!r.attachDeployed()) return;
        double hl = r.length / 2;
        for (Flower f : flowers) {
            if (f.stack.isEmpty() || f.cooldown > 0) continue;
            double[] l = r.toLocal(f.wallX, f.wallY);
            boolean inFront = l[0] > hl - 0.5;
            boolean reaches = l[0] - (hl + r.attachReach) <= Layout.ATTACH_TIP_TO_WALL;
            boolean aligned = Math.abs(l[1]) <= r.attachWidth / 2;
            if (!(inFront && reaches && aligned)) continue;
            Ball b = f.stack.get(0);
            if (b.isNectar()) continue;
            f.stack.remove(0);
            f.relayout();
            f.cooldown = Layout.ATTACH_DROP_INTERVAL;
            b.state = Ball.State.FIELD;
            b.x = f.wallX + f.nx * 3.5; b.y = f.wallY + f.ny * 3.5;
            b.z = b.r;
            double dx = r.x - b.x, dy = r.y - b.y, d = Math.max(1e-6, Math.hypot(dx, dy));
            b.vx = dx / d * 35; b.vy = dy / d * 35; b.vz = 0;
            log.add(String.format("t=%.2f %s derrubou Pollen da FLOWER %s com o anexo", t, r.name, f.name));
            return;
        }
    }

    private void fire(Robot r) {
        if (!r.shootRequest) return;
        r.shootRequest = false;
        if (r.held.isEmpty() || t - r.lastShotT < r.minShotInterval || !r.shooterReady()) return;
        Ball b = r.held.poll();
        double c = Math.cos(r.h), s = Math.sin(r.h);
        b.x = r.x + r.turretOffX * c - r.turretOffY * s;
        b.y = r.y + r.turretOffX * s + r.turretOffY * c;
        b.z = r.muzzleZ;
        double v = r.shooterSpeed * (1 + r.speedNoise * rng.nextGaussian());
        double az = r.h + r.turret;
        b.vx = r.vx + v * Math.cos(r.shootElev) * Math.cos(az);
        b.vy = r.vy + v * Math.cos(r.shootElev) * Math.sin(az);
        b.vz = v * Math.sin(r.shootElev);
        b.state = Ball.State.FIELD;
        b.ignoreRobot = r;
        r.lastShotT = t;
        r.shooterSpeed *= 1 - r.shotSpeedDrop;
    }

    // ---------- Bolas ----------

    private void stepBalls() {
        for (Flower f : flowers) f.cooldown = Math.max(0, f.cooldown - DT);
        for (Ball b : balls) {
            if (b.state != Ball.State.FIELD) continue;
            double px = b.x, py = b.y, pz = b.z;
            if (b.z > b.r + 1e-9 || b.vz != 0) {
                b.vz -= Layout.G * DT;
                b.z += b.vz * DT;
            }
            if (b.z <= b.r) {
                b.z = b.r;
                if (b.vz < 0) b.vz = b.vz < -20 ? -b.vz * Layout.FLOOR_E : 0;
            }
            if (b.z <= b.r + 1e-9 && b.vz == 0) {
                double sp = Math.hypot(b.vx, b.vy);
                if (sp > 0) {
                    double ns = Math.max(0, sp - Layout.ROLL_DECEL * DT);
                    b.vx *= ns / sp; b.vy *= ns / sp;
                }
            }
            b.x += b.vx * DT; b.y += b.vy * DT;
            double lo = b.r, hi = Layout.FIELD - b.r;
            if (b.x < lo) { b.x = lo; b.vx = Math.abs(b.vx) > 5 ? -b.vx * Layout.WALL_E : 0; }
            else if (b.x > hi) { b.x = hi; b.vx = Math.abs(b.vx) > 5 ? -b.vx * Layout.WALL_E : 0; }
            if (b.y < lo) { b.y = lo; b.vy = Math.abs(b.vy) > 5 ? -b.vy * Layout.WALL_E : 0; }
            else if (b.y > hi) { b.y = hi; b.vy = Math.abs(b.vy) > 5 ? -b.vy * Layout.WALL_E : 0; }
            if (!captureCell(b, py)) captureFlower(b, pz);
        }
        collideBalls();
        collideBallsWithRobots();
    }

    private boolean captureCell(Ball b, double prevY) {
        if (b.z <= b.r + 1.0) return false;
        for (Hive h : new Hive[] {redHive, blueHive}) {
            double mouth = h.mouthY(h.up);
            boolean crossed = (prevY - mouth) * (b.y - mouth) <= 0 && prevY != b.y;
            if (!crossed || Math.signum(b.vy) != h.entryDir(h.up)) continue;
            if (Math.abs(b.x - h.axisX) > Layout.CELL_OPEN_W / 2 - 0.3 * b.r) continue;
            if (Math.abs(b.z - Layout.CELL_Z) > Layout.CELL_OPEN_H / 2 - 0.3 * b.r) continue;
            deposit(h, b);
            return true;
        }
        return false;
    }

    /** Um elemento lancado entra na CELL voltada para cima: conta para a condicao de virar a HIVE. */
    void deposit(Hive h, Ball b) {
        b.state = Ball.State.CELL;
        b.vx = b.vy = b.vz = 0;
        h.cell(h.up).add(b);
        if (b.isNectar()) h.nectarIn++; else h.pollenIn++;
        log.add(String.format("t=%.2f %s entrou na CELL %s (Pollen %d, Nectar %d)", t, b.type, h.alliance, h.pollenIn, h.nectarIn));
        if (h.shouldTip()) tip(h);
    }

    private void tip(Hive h) {
        int dir = h.up == 0 ? -1 : 1;
        for (Ball b : new ArrayList<>(h.cell(h.up))) {
            b.state = Ball.State.FIELD;
            b.x = h.axisX + (rng.nextDouble() - 0.5) * 8;
            b.y = h.mouthY(h.up) + dir * 3;
            b.z = 12;
            b.vx = 0; b.vy = dir * 25; b.vz = 0;
        }
        h.cell(h.up).clear();
        h.up = 1 - h.up;
        h.pollenIn = 0; h.nectarIn = 0;
        if (!freePlay && t < Layout.TELEOP_START) h.tipsAuto++; else h.tipsTele++;
        log.add(String.format("t=%.2f HIVE %s TIP (#%d)", t, h.alliance, h.tips()));
    }

    private void captureFlower(Ball b, double prevZ) {
        if (b.vz >= 0 || prevZ < Layout.FLOWER_OPEN_Z || b.z >= Layout.FLOWER_OPEN_Z) return;
        for (Flower f : flowers) {
            if (Math.hypot(b.x - f.ox, b.y - f.oy) > Layout.FLOWER_CATCH_R || !f.fits()) continue;
            b.state = Ball.State.FLOWER;
            b.vx = b.vy = b.vz = 0;
            f.stack.add(b);
            f.relayout();
            if (!freePlay && t < Layout.LAST_MINUTE)
                log.add(String.format("t=%.1f elemento colocado na FLOWER %s antes do ultimo minuto (G410)", t, f.name));
            return;
        }
    }

    private void collideBalls() {
        int n = balls.size();
        for (int i = 0; i < n; i++) {
            Ball a = balls.get(i);
            if (a.state != Ball.State.FIELD) continue;
            for (int j = i + 1; j < n; j++) {
                Ball b = balls.get(j);
                if (b.state != Ball.State.FIELD) continue;
                double dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z, min = a.r + b.r;
                if (Math.abs(dx) > min || Math.abs(dy) > min || Math.abs(dz) > min) continue;
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist >= min || dist < 1e-9) continue;
                double nx = dx / dist, ny = dy / dist, nz = dz / dist, push = (min - dist) / 2;
                a.x -= nx * push; a.y -= ny * push; a.z -= nz * push;
                b.x += nx * push; b.y += ny * push; b.z += nz * push;
                double rel = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny + (b.vz - a.vz) * nz;
                if (rel < 0) {
                    double ma = a.weight, mb = b.weight, jimp = -(1 + Layout.BALL_E) * rel / (1 / ma + 1 / mb);
                    a.vx -= jimp / ma * nx; a.vy -= jimp / ma * ny; a.vz -= jimp / ma * nz;
                    b.vx += jimp / mb * nx; b.vy += jimp / mb * ny; b.vz += jimp / mb * nz;
                }
                if (a.z < a.r) a.z = a.r;
                if (b.z < b.r) b.z = b.r;
            }
        }
    }


    // ---------- Robo x elementos ----------

    /** Penetracao da bola no retangulo do robo (2D): {nx, ny, profundidade, contatoX, contatoY} (normal do robo para a bola) ou null. */
    private static double[] ballRobotContact(Ball b, Robot r) {
        double[] l = r.toLocal(b.x, b.y);
        double hl = r.length / 2, hw = r.width / 2;
        double cxl = Geo.clamp(l[0], -hl, hl), cyl = Geo.clamp(l[1], -hw, hw);
        double dx = l[0] - cxl, dy = l[1] - cyl, dist = Math.hypot(dx, dy), nxl, nyl, pen;
        if (dist > 1e-9) {
            if (dist >= b.r) return null;
            nxl = dx / dist; nyl = dy / dist; pen = b.r - dist;
        } else {
            double px = hl - Math.abs(l[0]), py = hw - Math.abs(l[1]);
            if (px < py) { nxl = l[0] >= 0 ? 1 : -1; nyl = 0; pen = px + b.r; cxl = nxl * hl; cyl = l[1]; }
            else { nxl = 0; nyl = l[1] >= 0 ? 1 : -1; pen = py + b.r; cxl = l[0]; cyl = nyl * hw; }
        }
        double c = Math.cos(r.h), s = Math.sin(r.h);
        return new double[] {nxl * c - nyl * s, nxl * s + nyl * c, pen, cxl * c - cyl * s, cxl * s + cyl * c};
    }

    /** Colisao entre robos e elementos no chao. A fisica elemento x elemento (collideBalls) nao e alterada. */
    private void collideBallsWithRobots() {
        double lo, hi;
        for (Ball b : balls) {
            if (b.state != Ball.State.FIELD) continue;
            for (Robot r : robots) {
                double[] k = ballRobotContact(b, r);
                if (b.ignoreRobot == r) {
                    if (k == null || b.z - b.r >= r.height) b.ignoreRobot = null;
                    continue;
                }
                if (k == null || b.z - b.r >= r.height) continue;   // longe ou passando por cima do robo
                double nx = k[0], ny = k[1], pen = k[2];
                lo = b.r; hi = Layout.FIELD - b.r;
                double px = Geo.clamp(b.x + nx * pen, lo, hi), py = Geo.clamp(b.y + ny * pen, lo, hi);
                double rem = pen - ((px - b.x) * nx + (py - b.y) * ny);   // parte que a parede impede a bola de absorver
                b.x = px; b.y = py;
                if (rem > 1e-6) {                                         // bola prensada: o robo e empurrado de volta
                    r.x -= nx * rem; r.y -= ny * rem;
                    double vin = r.vx * nx + r.vy * ny;
                    if (vin > 0) { r.vx -= vin * nx; r.vy -= vin * ny; }
                }
                double vsx = r.vx - r.w * k[4], vsy = r.vy + r.w * k[3];   // velocidade do ponto de contato do robo
                double vn = (b.vx - vsx) * nx + (b.vy - vsy) * ny;
                if (vn < 0) {
                    double j = -(1 + Layout.ROBOT_BALL_E) * vn / (1 / b.massKg + 1 / r.mass);
                    b.vx += j / b.massKg * nx; b.vy += j / b.massKg * ny;
                    r.vx -= j / r.mass * nx;   r.vy -= j / r.mass * ny;     // reacao no robo (limitada pelo controle de aceleracao)
                }
            }
        }
    }

    // ---------- Pontuacao ----------

    private boolean isOffWall(Robot r) {
        double[] p = Geo.corners(r.x, r.y, r.h, r.length / 2, r.width / 2);
        for (int i = 0; i < 4; i++) {
            double px = p[2 * i], py = p[2 * i + 1];
            if (px < 0.3 || px > Layout.FIELD - 0.3 || py < 0.3 || py > Layout.FIELD - 0.3) return false;
        }
        return true;
    }

    private boolean isParked(Robot r) {
        double[] lz = r.alliance == Alliance.RED ? Layout.RED_LZ : Layout.BLUE_LZ;
        return Geo.overlapsRect(r.x, r.y, r.h, r.length / 2, r.width / 2, lz);
    }

    public static final class Score {
        public int leave, parkAuto, parkTele, tipsAuto, tipsTele, cell, flower, bottom, garden, foul;
        public int rpSwarm, rpPollinator1, rpPollinator2, rpResult;
        public int auto() { return leave * 3 + parkAuto * 5 + tipsAuto * 20; }
        public int teleop() { return parkTele * 5 + tipsTele * 20 + cell * 2 + flower * 2 + bottom * 5 + garden; }
        public int total() { return auto() + teleop() + foul; }
        public int rp() { return rpSwarm + rpPollinator1 + rpPollinator2 + rpResult; }

        @Override
        public String toString() {
            return String.format("AUTO %d | TELEOP %d | faltas %d | TOTAL %d | RP %d "
                    + "(leave %d, park %d+%d, tips %d+%d, cell %d, flower %d, bottom %d, garden %d)",
                auto(), teleop(), foul, total(), rp(), leave, parkAuto, parkTele, tipsAuto, tipsTele,
                cell, flower, bottom, garden);
        }
    }

    /** Pontuacao no estado atual. Chame depois de settle() para a contagem final. */
    public Score[] score() {
        Score[] s = {new Score(), new Score()};
        for (Robot r : robots) {
            Score x = s[r.alliance.ordinal()];
            if (r.leftInAuto) x.leave++;
            if (r.parkedInAuto) x.parkAuto++;
            if (isParked(r)) x.parkTele++;
        }
        for (Alliance a : Alliance.values()) {
            Score x = s[a.ordinal()];
            Hive h = hive(a);
            x.tipsAuto = h.tipsAuto; x.tipsTele = h.tipsTele;
            x.cell = h.cell(h.up).size();
            x.foul = foulCredit[a.ordinal()];
        }
        for (Flower f : flowers) {
            List<Ball> in = new ArrayList<>();
            for (Ball b : f.stack) if (f.scoring(b)) in.add(b);
            Alliance owner = null;
            for (int i = in.size() - 1; i >= 0 && owner == null; i--) owner = in.get(i).nectarAlliance();
            if (owner != null) s[owner.ordinal()].flower += in.size();
            for (Ball b : in) {
                if (b.isNectar()) { s[b.nectarAlliance().ordinal()].bottom++; break; }
            }
        }
        for (Ball b : balls) {
            if (b.state != Ball.State.FIELD || b.z > b.r + 0.5 || b.speed() > 1) continue;
            if (inGarden(b, Layout.RED_GARDEN)) s[0].garden++;
            if (inGarden(b, Layout.BLUE_GARDEN)) s[1].garden++;
        }
        for (Alliance a : Alliance.values()) {
            Score x = s[a.ordinal()], o = s[a.opponent().ordinal()];
            if ((x.leave * 3 + x.parkAuto * 5 + x.parkTele * 5) >= swarmThreshold) x.rpSwarm = 1;
            int tips = x.tipsAuto + x.tipsTele;
            if (tips >= pollinator1) x.rpPollinator1 = 1;
            if (tips >= pollinator2) x.rpPollinator2 = 1;
            x.rpResult = x.total() > o.total() ? 3 : x.total() == o.total() ? 1 : 0;
        }
        return s;
    }

    private static boolean inGarden(Ball b, double[] g) {
        double cx = Geo.clamp(b.x, g[0], g[2]), cy = Geo.clamp(b.y, g[1], g[3]);
        return Math.hypot(b.x - cx, b.y - cy) <= b.r;
    }
}
