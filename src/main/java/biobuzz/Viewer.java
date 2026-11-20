package biobuzz;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Janela 2D (vista de cima) da arena com controle por teclado.
 * Rodar: biobuzz.Viewer. Para gerar uma imagem sem janela: biobuzz.Viewer --png arquivo.png
 */
public final class Viewer {
    static final double S = 5.0;                       // pixels por polegada
    static final int FIELD_PX = (int) (Layout.FIELD * S), HUD_W = 420;
    static final Color RED = new Color(225, 70, 70), BLUE = new Color(70, 115, 235);

    static int sx(double x) { return (int) Math.round(x * S); }
    static int sy(double y) { return (int) Math.round((Layout.FIELD - y) * S); }
    static Color of(Alliance a) { return a == Alliance.RED ? RED : BLUE; }

    // ---------- desenho ----------

    static void paint(Graphics2D g, Match m, List<String> hud) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(40, 42, 46));
        g.fillRect(0, 0, FIELD_PX + HUD_W, FIELD_PX);
        g.setColor(new Color(70, 74, 80));
        for (int i = 0; i <= 6; i++) {
            g.drawLine(sx(24 * i), 0, sx(24 * i), FIELD_PX);
            g.drawLine(0, sy(24 * i), FIELD_PX, sy(24 * i));
        }
        zone(g, Layout.RED_LZ, RED); zone(g, Layout.BLUE_LZ, BLUE);
        zone(g, Layout.RED_GARDEN, RED); zone(g, Layout.BLUE_GARDEN, BLUE);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3));
        g.drawRect(0, 0, FIELD_PX, FIELD_PX);
        g.setStroke(new BasicStroke(1));

        g.setColor(new Color(110, 110, 115));
        g.drawRect(sx(Layout.FRAME_CX - Layout.FRAME_W / 2), sy(Layout.FRAME_CY + Layout.FRAME_D / 2),
            sx(Layout.FRAME_W), sx(Layout.FRAME_D));
        for (Hive h : new Hive[] {m.redHive, m.blueHive}) {
            for (int i = 0; i < 2; i++) {
                boolean up = i == h.up;
                Color c = of(h.alliance);
                g.setColor(up ? c : c.darker().darker());
                int x = sx(h.axisX - Layout.CELL_OPEN_W / 2), y = sy(h.cellY(i) + Layout.CELL_DEPTH / 2);
                int w = sx(Layout.CELL_OPEN_W), hh = sx(Layout.CELL_DEPTH);
                if (up) g.fillRect(x, y, w, hh); else g.drawRect(x, y, w, hh);
                g.setColor(Color.WHITE);
                int my = sy(h.mouthY(i));
                g.drawLine(x, my, x + w, my);
                if (up) g.drawString(h.cell(i).size() + "", sx(h.axisX) - 3, sy(h.cellY(i)) + 4);
            }
        }
        for (Flower f : m.flowers) {
            g.setColor(new Color(240, 200, 60));
            g.drawOval(sx(f.ox) - 10, sy(f.oy) - 10, 20, 20);
            g.drawString(f.stack.size() + "", sx(f.ox) - 3, sy(f.oy) + 4);
        }
        for (Robot r : m.robots) drawRobot(g, r);
        for (Ball b : m.balls) {
            if (b.state != Ball.State.FIELD) continue;
            int rad = (int) Math.round(b.r * S);
            int cx = sx(b.x), cy = sy(b.y);
            boolean air = b.z > b.r + 1;
            if (air) { g.setColor(new Color(0, 0, 0, 90)); g.fillOval(cx - rad, cy - rad, 2 * rad, 2 * rad); cy -= (int) (b.z * 0.6); }
            g.setColor(b.type == Ball.Type.POLLEN ? new Color(250, 220, 60)
                : b.type == Ball.Type.NECTAR_RED ? RED.brighter() : BLUE.brighter());
            g.fillOval(cx - rad, cy - rad, 2 * rad, 2 * rad);
            g.setColor(air ? Color.WHITE : Color.DARK_GRAY);
            g.drawOval(cx - rad, cy - rad, 2 * rad, 2 * rad);
        }
        g.setColor(new Color(25, 25, 28));
        g.fillRect(FIELD_PX, 0, HUD_W, FIELD_PX);
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        g.setColor(Color.WHITE);
        int y = 22;
        for (String line : hud) {
            if (line.startsWith("#R")) { g.setColor(RED.brighter()); line = line.substring(2); }
            else if (line.startsWith("#B")) { g.setColor(BLUE.brighter()); line = line.substring(2); }
            else g.setColor(Color.WHITE);
            g.drawString(line, FIELD_PX + 12, y);
            y += 17;
        }
    }

    static void zone(Graphics2D g, double[] z, Color c) {
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 90));
        g.fillRect(sx(z[0]), sy(z[3]), sx(z[2] - z[0]), sx(z[3] - z[1]));
    }

    static void drawRobot(Graphics2D g, Robot r) {
        AffineTransform old = g.getTransform();
        g.translate(sx(r.x), sy(r.y));
        g.rotate(-r.h);
        g.scale(S, -S);
        Color c = of(r.alliance);
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 110));
        g.fill(new java.awt.geom.Rectangle2D.Double(-r.length / 2, -r.width / 2, r.length, r.width));
        g.setColor(c);
        g.setStroke(new BasicStroke((float) (1 / S)));
        g.draw(new java.awt.geom.Rectangle2D.Double(-r.length / 2, -r.width / 2, r.length, r.width));
        g.setColor(new Color(60, 200, 255));
        g.setStroke(new BasicStroke((float) (4 / S)));
        g.draw(new java.awt.geom.Line2D.Double(r.length / 2, -r.width / 2 + 1, r.length / 2, r.width / 2 - 1));
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke((float) (2 / S)));
        g.draw(new java.awt.geom.Line2D.Double(r.turretOffX, r.turretOffY,
            r.turretOffX + 9 * Math.cos(r.turret), r.turretOffY + 9 * Math.sin(r.turret)));
        g.setTransform(old);
        g.setColor(Color.WHITE);
        g.drawString(r.name + " [" + r.held.size() + "]", sx(r.x) - 14, sy(r.y) + 4);
    }

    // ---------- controle ----------

    static void autoAim(Match m, Robot r) {
        Hive h = m.hive(r.alliance);
        double c = Math.cos(r.h), s = Math.sin(r.h);
        double cx = r.x + r.turretOffX * c - r.turretOffY * s, cy = r.y + r.turretOffX * s + r.turretOffY * c;
        double tx = h.axisX, ty = h.mouthY(h.up);
        r.turretTarget = Geo.wrap(Math.atan2(ty - cy, tx - cx) - r.h);
        double v = Ballistics.speedFor(Math.hypot(tx - cx, ty - cy), Layout.CELL_Z - r.muzzleZ, r.shootElev);
        if (!Double.isNaN(v)) r.shooterTarget = v;
    }

    static Match newMatch() {
        Match m = new Match();
        m.addRobot("R1", Alliance.RED, 54, 28, Math.PI / 2);
        m.addRobot("R2", Alliance.RED, 11, 30, 0);
        m.addRobot("B1", Alliance.BLUE, 133, 100, Math.PI);
        m.addRobot("B2", Alliance.BLUE, 133, 70, Math.PI);
        m.stage();
        return m;
    }

    static List<String> hud(Match m, Robot me, boolean aim) {
        Match.Score[] s = m.score();
        int t = (int) m.t;
        List<String> l = new java.util.ArrayList<>();
        l.add("BIOBUZZ SIM   " + String.format("%d:%02d", t / 60, t % 60) + "   " + m.phase());
        l.add("#R VERMELHO " + s[0].total() + "  (auto " + s[0].auto() + ", tele " + s[0].teleop() + ")  RP " + s[0].rp());
        l.add("#B AZUL     " + s[1].total() + "  (auto " + s[1].auto() + ", tele " + s[1].teleop() + ")  RP " + s[1].rp());
        l.add("tips: V " + m.redHive.tips() + "  A " + m.blueHive.tips() + "   nectar livre V " + (m.canEnterNectar(Alliance.RED) ? "sim" : "nao"));
        l.add("");
        l.add(String.format("%s x %.1f y %.1f  head %.0f", me.name, me.x, me.y, Math.toDegrees(me.h)));
        l.add(String.format("seguro %d/%d  intake %s  auto-mira %s", me.held.size(), me.capacity, me.intakeOn ? "ON" : "off", aim ? "ON" : "off"));
        l.add(String.format("shooter %.0f / %.0f in/s  torreta %.0f gr", me.shooterSpeed, me.shooterTarget, Math.toDegrees(me.turret)));
        l.add("");
        l.add("W/S frente/tras  A/D esq/dir  Q/E girar");
        l.add("I intake   Z auto-mira   ESPACO atirar");
        l.add("J/L torreta  U/O potencia shooter");
        l.add("N nectar (humano)  Y pula o AUTO  R reinicia");
        l.add("");
        int shown = 0;
        for (int i = m.log.size() - 1; i >= 0 && shown < 8; i--, shown++) l.add(m.log.get(i).length() > 44 ? m.log.get(i).substring(0, 44) : m.log.get(i));
        return l;
    }

    public static void main(String[] args) throws Exception {
        if (args.length >= 2 && args[0].equals("--png")) { snapshot(args[1]); return; }
        System.setProperty("sun.java2d.opengl", "false");
        final Match[] cur = {newMatch()};
        final Set<Integer> keys = new HashSet<>();
        final boolean[] aim = {false};
        Runnable bind = () -> cur[0].setController(cur[0].robots.get(0), (mm, r) -> {
            double f = (keys.contains(KeyEvent.VK_W) ? 1 : 0) - (keys.contains(KeyEvent.VK_S) ? 1 : 0);
            double st = (keys.contains(KeyEvent.VK_A) ? 1 : 0) - (keys.contains(KeyEvent.VK_D) ? 1 : 0);
            double tr = (keys.contains(KeyEvent.VK_Q) ? 1 : 0) - (keys.contains(KeyEvent.VK_E) ? 1 : 0);
            r.drive(f, st, tr);
            if (aim[0]) autoAim(mm, r);
            if (keys.contains(KeyEvent.VK_SPACE)) r.shoot();
        });
        bind.run();
        SwingUtilities.invokeAndWait(() -> {
            JFrame fr = new JFrame("BIOBUZZ Sim");
            JPanel p = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Viewer.paint((Graphics2D) g, cur[0], hud(cur[0], cur[0].robots.get(0), aim[0]));
                }
            };
            p.setPreferredSize(new Dimension(FIELD_PX + HUD_W, FIELD_PX));
            fr.add(p);
            fr.addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    int k = e.getKeyCode();
                    Robot me = cur[0].robots.get(0);
                    if (keys.add(k)) {
                        if (k == KeyEvent.VK_I) me.intakeOn = !me.intakeOn;
                        if (k == KeyEvent.VK_Z) aim[0] = !aim[0];
                        if (k == KeyEvent.VK_Y && cur[0].t < Layout.TELEOP_START) cur[0].t = Layout.TELEOP_START;
                        if (k == KeyEvent.VK_N) cur[0].enterNectar(me.alliance);
                        if (k == KeyEvent.VK_R) { cur[0] = newMatch(); bind.run(); }
                    }
                    if (k == KeyEvent.VK_J) me.turretTarget += 0.08;
                    if (k == KeyEvent.VK_L) me.turretTarget -= 0.08;
                    if (k == KeyEvent.VK_U) me.shooterTarget += 5;
                    if (k == KeyEvent.VK_O) me.shooterTarget = Math.max(0, me.shooterTarget - 5);
                }
                @Override public void keyReleased(KeyEvent e) { keys.remove(e.getKeyCode()); }
            });
            fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            fr.pack();
            fr.setResizable(false);
            fr.setVisible(true);
            fr.requestFocus();
            new Timer(15, ev -> { cur[0].run(0.015); p.repaint(); }).start();
        });
    }

    static void snapshot(String file) throws Exception {
        Match m = newMatch();
        Robot me = m.robots.get(0);
        me.shootElev = Math.toRadians(60);
        m.setController(me, (mm, r) -> { autoAim(mm, r); if (r.shooterReady()) r.shoot(); });
        m.runUntil(3.4);
        BufferedImage img = new BufferedImage(FIELD_PX + HUD_W, FIELD_PX, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        paint(g, m, hud(m, me, true));
        g.dispose();
        ImageIO.write(img, "png", new File(file));
        System.out.println("salvo " + file);
    }
}
