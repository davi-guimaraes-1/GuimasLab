package biobuzz;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * IA simples para os robos adversarios e o parceiro: coleta Pollen solto no chao, vai ate um ponto em frente a CELL
 * voltada para cima da propria HIVE, calcula a velocidade do shooter e atira, e estaciona na LOADING ZONE no final.
 * Desvia das barras de base da HIVE passando pelo corredor central e destrava quando fica preso.
 */
public final class SimpleAI implements Match.Controller {
    private enum S { COLLECT, SHOOT, PARK }

    public static final double SHOOT_DIST = 24.0;

    /** Faz o papel do humano da alianca: coloca Nectar em jogo quando permitido. */
    public boolean humanPlayer = false;

    private S state = S.COLLECT;
    private double humanNext = 0;
    private final Map<Ball, Double> blacklist = new HashMap<>();
    private Ball target;
    private double progT = 0, progX, progY, unstickUntil = -1, unstickDir = 1;

    @Override
    public void loop(Match m, Robot r) {
        if (humanPlayer && m.t >= humanNext && m.canEnterNectar(r.alliance)) {
            m.enterNectar(r.alliance);
            humanNext = m.t + 4.0;
        }
        Match.Phase ph = m.phase();
        r.intakeOn = false;
        if (ph != Match.Phase.AUTO && ph != Match.Phase.TELEOP) { r.drive(0, 0, 0); r.shooterTarget = 0; return; }

        boolean endgame = !m.freePlay && m.t > Layout.MATCH_END - 12;
        if (endgame) state = S.PARK;
        else if (state == S.PARK) state = S.COLLECT;
        if (state == S.COLLECT && (r.held.size() >= 4 || (!r.held.isEmpty() && findTarget(m, r) == null))) state = S.SHOOT;
        if (state == S.SHOOT && r.held.isEmpty()) state = S.COLLECT;
        if (state != S.SHOOT) { r.shooterTarget = 0; r.shootRequest = false; }

        if (m.t < unstickUntil) { r.drive(0, unstickDir * 0.7, 0); return; }

        boolean moving = true;
        switch (state) {
            case COLLECT: {
                Ball b = findTarget(m, r);
                if (b == null) { r.drive(0, 0, 0); moving = false; break; }
                r.intakeOn = true;
                double[] w = route(r, b.x, b.y);
                goTo(r, w[0], w[1], Math.atan2(b.y - r.y, b.x - r.x), 0.9);
                break;
            }
            case SHOOT: {
                Hive h = m.hive(r.alliance);
                int dir = h.entryDir(h.up);
                double mouth = h.mouthY(h.up), tx = h.axisX, ty = mouth - dir * SHOOT_DIST;
                double th = dir > 0 ? Math.PI / 2 : -Math.PI / 2;
                r.turretTarget = 0;
                double[] w = route(r, tx, ty);
                goTo(r, w[0], w[1], th, 0.9);
                double c = Math.cos(r.h), s = Math.sin(r.h);
                double mx = r.x + r.turretOffX * c - r.turretOffY * s, my = r.y + r.turretOffX * s + r.turretOffY * c;
                double v = Ballistics.speedFor(Math.hypot(h.axisX - mx, mouth - my), Layout.CELL_Z - r.muzzleZ, r.shootElev);
                if (!Double.isNaN(v)) r.shooterTarget = v;
                boolean there = Math.hypot(tx - r.x, ty - r.y) < 2.5 && Math.abs(Geo.wrap(th - r.h)) < 0.08;
                if (there && Math.hypot(r.vx, r.vy) < 6 && r.shooterReady()) r.shoot();
                moving = !there;
                break;
            }
            default: {
                double[] lz = r.alliance == Alliance.RED ? Layout.RED_LZ : Layout.BLUE_LZ;
                double tx = r.alliance == Alliance.RED ? r.length / 2 + 2 : Layout.FIELD - r.length / 2 - 2;
                double ty = (lz[1] + lz[3]) / 2, th = r.alliance == Alliance.RED ? 0 : Math.PI;
                double[] w = route(r, tx, ty);
                goTo(r, w[0], w[1], th, 0.6);
                moving = Math.hypot(tx - r.x, ty - r.y) > 1.5;
            }
        }
        checkStuck(m, r, moving);
    }

    private Ball findTarget(Match m, Robot r) {
        if (target != null && target.state == Ball.State.FIELD && target.z <= target.r + 0.6
            && !blacklisted(m, target)) return target;
        Ball best = null;
        double bd = 1e9;
        for (Ball b : m.balls) {
            if (b.type != Ball.Type.POLLEN || b.state != Ball.State.FIELD || b.z > b.r + 0.6 || b.speed() > 10) continue;
            if (blacklisted(m, b) || !reachable(r, b)) continue;
            double d = Math.hypot(b.x - r.x, b.y - r.y);
            if (d < bd) { bd = d; best = b; }
        }
        target = best;
        return best;
    }

    /** Ignora bolas embaixo da HIVE e no lado do adversario (evita engarrafar o corredor central). */
    private static boolean reachable(Robot r, Ball b) {
        double fx0 = Layout.FRAME_CX - Layout.FRAME_W / 2 - 2, fx1 = Layout.FRAME_CX + Layout.FRAME_W / 2 + 2;
        double fy0 = Layout.FRAME_CY - Layout.FRAME_D / 2 - 2, fy1 = Layout.FRAME_CY + Layout.FRAME_D / 2 + 2;
        if (b.x > fx0 && b.x < fx1 && b.y > fy0 && b.y < fy1) return false;
        return r.alliance == Alliance.RED ? b.x < Layout.FIELD / 2 + 6 : b.x > Layout.FIELD / 2 - 6;
    }

    private boolean blacklisted(Match m, Ball b) {
        Double until = blacklist.get(b);
        return until != null && m.t < until;
    }

    private void checkStuck(Match m, Robot r, boolean moving) {
        if (!moving) { progT = m.t; progX = r.x; progY = r.y; return; }
        if (m.t - progT < 1.5) return;
        if (Math.hypot(r.x - progX, r.y - progY) < 1.0) {
            unstickUntil = m.t + 0.8;
            unstickDir = -unstickDir;
            if (state == S.COLLECT && target != null) blacklist.put(target, m.t + 12);
        }
        progT = m.t; progX = r.x; progY = r.y;
    }

    // ---------- navegacao ----------

    static void goTo(Robot r, double tx, double ty, double th, double maxPower) {
        double ex = tx - r.x, ey = ty - r.y, c = Math.cos(r.h), s = Math.sin(r.h);
        double f = (ex * c + ey * s) * 0.06, st = (-ex * s + ey * c) * 0.06;
        double mag = Math.hypot(f, st), dist = Math.hypot(ex, ey);
        if (mag > maxPower) { f *= maxPower / mag; st *= maxPower / mag; }
        else if (dist > 0.7 && mag < 0.15) { f *= 0.15 / mag; st *= 0.15 / mag; }
        else if (dist <= 0.7) { f = 0; st = 0; }
        r.drive(f, st, Geo.clamp(Geo.wrap(th - r.h) * 1.5, -0.8, 0.8));
    }

    private static boolean crossesBars(double x1, double y1, double x2, double y2) {
        for (int i = 0; i <= 30; i++) {
            double x = x1 + (x2 - x1) * i / 30, y = y1 + (y2 - y1) * i / 30;
            for (HiveGeometry.Obstacle o : HiveGeometry.OBSTACLES)
                if (o.zBottom == 0 && x > o.xmin - 10 && x < o.xmax + 10 && y > o.ymin - 10 && y < o.ymax + 10) return true;
        }
        return false;
    }

    /** Proximo ponto de passagem: contorna a HIVE pela faixa externa do proprio lado quando o caminho reto cruzaria as barras. */
    static double[] route(Robot r, double tx, double ty) {
        if (!crossesBars(r.x, r.y, tx, ty)) return new double[] {tx, ty};
        double xo = r.alliance == Alliance.RED ? 30 : Layout.FIELD - 30;
        if (Math.abs(r.x - xo) > 10) return new double[] {xo, r.y};
        return new double[] {xo, ty};
    }
}
