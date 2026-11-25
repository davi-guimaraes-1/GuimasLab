package biobuzz;

import biobuzz.sdk.TeamCodeBuilder;
import biobuzz.sdk.TeamCodeLoader;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/** Tela inicial: modo, numero de robos, alianca e posicao/orientacao inicial do AGGRON escolhidas na arena. */
public final class Menu {
    private Menu() {}

    static final double SC = 4.0;
    static final int PX = (int) (Layout.FIELD * SC);
    static final Color RED = Viewer.RED, BLUE = Viewer.BLUE;

    /** Arena clicavel: clique posiciona, arraste gira, roda do mouse ou Q/E tambem giram. */
    static final class FieldPanel extends JPanel {
        final SimConfig cfg;
        final StartPicker picker;
        final Runnable onChange;

        FieldPanel(SimConfig cfg, StartPicker picker, Runnable onChange) {
            this.cfg = cfg; this.picker = picker; this.onChange = onChange;
            setPreferredSize(new Dimension(PX, PX));
            setFocusable(true);
            MouseAdapter ma = new MouseAdapter() {
                double px0, py0;
                @Override public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                    px0 = fx(e.getX()); py0 = fy(e.getY());
                    picker.place(px0, py0);
                    FieldPanel.this.onChange.run();
                }
                @Override public void mouseDragged(MouseEvent e) {
                    double x = fx(e.getX()), y = fy(e.getY());
                    if (Math.hypot(x - px0, y - py0) > 4) picker.setHeading(Math.atan2(y - py0, x - px0));
                    FieldPanel.this.onChange.run();
                }
                @Override public void mouseWheelMoved(MouseWheelEvent e) {
                    picker.rotate(-e.getWheelRotation() * Math.toRadians(15));
                    FieldPanel.this.onChange.run();
                }
            };
            addMouseListener(ma); addMouseMotionListener(ma); addMouseWheelListener(ma);
            bindKey(KeyEvent.VK_Q, Math.toRadians(5));
            bindKey(KeyEvent.VK_E, -Math.toRadians(5));
        }

        private void bindKey(int key, double delta) {
            getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key, 0), "rot" + key);
            getActionMap().put("rot" + key, new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { picker.rotate(delta); onChange.run(); }
            });
        }

        static double fx(int px) { return px / SC; }
        static double fy(int py) { return (PX - py) / SC; }
        static int sx(double x) { return (int) Math.round(x * SC); }
        static int sy(double y) { return (int) Math.round((Layout.FIELD - y) * SC); }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(40, 42, 46));
            g.fillRect(0, 0, PX, PX);
            g.setColor(new Color(RED.getRed(), RED.getGreen(), RED.getBlue(), 28));
            g.fillRect(0, 0, PX / 2, PX);
            g.setColor(new Color(BLUE.getRed(), BLUE.getGreen(), BLUE.getBlue(), 28));
            g.fillRect(PX / 2, 0, PX / 2, PX);
            g.setColor(new Color(70, 74, 80));
            for (int i = 0; i <= 6; i++) { g.drawLine(sx(24 * i), 0, sx(24 * i), PX); g.drawLine(0, sy(24 * i), PX, sy(24 * i)); }
            zone(g, Layout.RED_LZ, RED); zone(g, Layout.BLUE_LZ, BLUE);
            zone(g, Layout.RED_GARDEN, RED); zone(g, Layout.BLUE_GARDEN, BLUE);
            g.setColor(new Color(110, 110, 115));
            g.drawRect(sx(Layout.FRAME_CX - Layout.FRAME_W / 2), sy(Layout.FRAME_CY + Layout.FRAME_D / 2), sx(Layout.FRAME_W), sx(Layout.FRAME_D));
            g.setColor(new Color(240, 200, 60));
            for (double[] w : Layout.FLOWER_WALL) {
                double ox = w[0] + w[2] * Layout.FLOWER_OFFSET, oy = w[1] + w[3] * Layout.FLOWER_OFFSET;
                g.drawOval(sx(ox) - 8, sy(oy) - 8, 16, 16);
            }
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(3));
            g.drawRect(0, 0, PX - 1, PX - 1);
            g.setStroke(new BasicStroke(1));
            if (cfg.robots == 4) {
                for (double[] a : StartRules.aiStarts(cfg)) {
                    Color c = a[0] == 0 ? RED : BLUE;
                    robot(g, a[1], a[2], a[3], new Color(c.getRed(), c.getGreen(), c.getBlue(), 70), c, true);
                }
            }
            String why = picker.status();
            Color edge = why == null ? new Color(70, 220, 110) : new Color(255, 80, 80);
            Color base = cfg.alliance == Alliance.RED ? RED : BLUE;
            robot(g, cfg.startX, cfg.startY, cfg.startH, new Color(base.getRed(), base.getGreen(), base.getBlue(), 150),
                cfg.mode == SimConfig.Mode.ROUND ? edge : Color.WHITE, false);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            g.setColor(Color.WHITE);
            g.drawString("Clique: posicionar   Arraste: girar   Roda do mouse ou Q/E: girar", 10, 18);
            g.drawString("AGGRON  x " + String.format("%.1f", cfg.startX) + "  y " + String.format("%.1f", cfg.startY)
                + "  heading " + String.format("%.0f", Math.toDegrees(cfg.startH)) + " gr", 10, PX - 10);
        }

        private static void zone(Graphics2D g, double[] z, Color c) {
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 90));
            g.fillRect(sx(z[0]), sy(z[3]), sx(z[2] - z[0]), sx(z[3] - z[1]));
        }

        private static void robot(Graphics2D g, double x, double y, double h, Color fill, Color edge, boolean ghost) {
            double l = StartRules.len(), w = StartRules.wid();
            AffineTransform old = g.getTransform();
            g.translate(sx(x), sy(y));
            g.rotate(-h);
            g.scale(SC, -SC);
            g.setColor(fill);
            g.fill(new Rectangle2D.Double(-l / 2, -w / 2, l, w));
            g.setColor(edge);
            g.setStroke(ghost ? new BasicStroke((float) (1 / SC), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                new float[] {(float) (4 / SC), (float) (4 / SC)}, 0f) : new BasicStroke((float) (2.5 / SC)));
            g.draw(new Rectangle2D.Double(-l / 2, -w / 2, l, w));
            g.setColor(new Color(60, 200, 255));
            g.setStroke(new BasicStroke((float) (4 / SC)));
            g.draw(new java.awt.geom.Line2D.Double(l / 2, -w / 2 + 1, l / 2, w / 2 - 1));
            if (!ghost) {
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke((float) (1.5 / SC)));
                g.draw(new java.awt.geom.Line2D.Double(0, 0, l / 2 + 4, 0));
            }
            g.setTransform(old);
            g.setStroke(new BasicStroke(1));
        }
    }

    public static void show(SimConfig cfg, Consumer<SimConfig> onStart) {
        Runnable build = () -> {
            StartPicker picker = new StartPicker(cfg);
            JFrame f = new JFrame("GuimasLab - Configuracao");
            JLabel status = new JLabel();
            JButton start = new JButton("Iniciar simulacao");
            FieldPanel[] fpr = new FieldPanel[1];
            Runnable refresh = () -> {
                String why = picker.status();
                status.setText("<html><div style='width:230px'>" + (why == null
                    ? "<font color='#2e9b52'>Posicao inicial valida.</font>"
                    : "<font color='#c0392b'>Posicao invalida: " + why + "</font>")
                    + (cfg.mode == SimConfig.Mode.CODE_TEST ? "<br>Teste de codigo: qualquer posicao e aceita." : "") + "</div></html>");
                start.setEnabled(picker.canStart());
                if (fpr[0] != null) fpr[0].repaint();
            };
            FieldPanel fp = new FieldPanel(cfg, picker, refresh);
            fpr[0] = fp;

            JPanel side = new JPanel();
            side.setLayout(new javax.swing.BoxLayout(side, javax.swing.BoxLayout.Y_AXIS));
            side.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            side.setPreferredSize(new Dimension(300, PX));
            JRadioButton round = new JRadioButton("Round (partida completa, cronometro e pontuacao)", cfg.mode == SimConfig.Mode.ROUND);
            JRadioButton test = new JRadioButton("Teste de codigo (ambiente livre)", cfg.mode == SimConfig.Mode.CODE_TEST);
            JRadioButton r1 = new JRadioButton("1 robo (somente AGGRON)", cfg.robots == 1);
            JRadioButton r4 = new JRadioButton("4 robos (AGGRON + 3 com IA)", cfg.robots == 4);
            JRadioButton red = new JRadioButton("Alianca vermelha", cfg.alliance == Alliance.RED);
            JRadioButton blue = new JRadioButton("Alianca azul", cfg.alliance == Alliance.BLUE);
            ButtonGroup g1 = new ButtonGroup(), g2 = new ButtonGroup(), g3 = new ButtonGroup();
            g1.add(round); g1.add(test); g2.add(r1); g2.add(r4); g3.add(red); g3.add(blue);
            round.addActionListener(e -> { cfg.mode = SimConfig.Mode.ROUND; picker.refresh(false); refresh.run(); });
            test.addActionListener(e -> { cfg.mode = SimConfig.Mode.CODE_TEST; picker.refresh(false); refresh.run(); });
            r1.addActionListener(e -> { cfg.robots = 1; refresh.run(); });
            r4.addActionListener(e -> { cfg.robots = 4; refresh.run(); });
            red.addActionListener(e -> { cfg.alliance = Alliance.RED; picker.refresh(true); refresh.run(); });
            blue.addActionListener(e -> { cfg.alliance = Alliance.BLUE; picker.refresh(true); refresh.run(); });
            // codigo da equipe (so no Teste de codigo)
            JTextField srcField = new JTextField(cfg.teamCodeSrc, 18);
            JButton browse = new JButton("..."), compile = new JButton("Compilar TeamCode");
            JComboBox<String> layout = new JComboBox<>(new String[] {"Rodas: superior = lado esquerdo (inferido da Atlas)", "Rodas: padrao (superior = frente)"});
            layout.addActionListener(e -> cfg.wheelLayout = layout.getSelectedIndex() == 0
                ? biobuzz.sdk.SdkParams.WheelLayout.ATLAS_SIDES : biobuzz.sdk.SdkParams.WheelLayout.STANDARD);
            JComboBox<String> ops = new JComboBox<>();
            JLabel codeStatus = new JLabel("<html>Opcional: escolha a pasta <i>TeamCode/src/main/java</i> do projeto da equipe.</html>");
            String NONE = "(nenhum: dirigir com o teclado)";
            ops.addItem(NONE);
            Runnable fillOps = () -> {
                ops.removeAllItems(); ops.addItem(NONE);
                if (cfg.teamCodeClasses != null) for (String n : TeamCodeLoader.list(cfg.teamCodeClasses)) ops.addItem(n);
            };
            browse.addActionListener(e -> {
                JFileChooser fc = new JFileChooser(srcField.getText().isEmpty() ? null : new File(srcField.getText()));
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (fc.showOpenDialog(f) == JFileChooser.APPROVE_OPTION) srcField.setText(fc.getSelectedFile().getAbsolutePath());
            });
            compile.addActionListener(e -> {
                cfg.teamCodeSrc = srcField.getText().trim();
                codeStatus.setText("Compilando...");
                TeamCodeBuilder.Result res = TeamCodeBuilder.build(new File(cfg.teamCodeSrc));
                if (res.error != null) {
                    cfg.teamCodeClasses = null;
                    codeStatus.setText("<html><font color='#c0392b'>" + res.error.split("\n")[0] + "</font></html>");
                    System.err.println(res.error);
                } else {
                    cfg.teamCodeClasses = res.classesDir;
                    codeStatus.setText("<html><font color='#2e9b52'>" + res.files + " arquivos compilados, " + res.opModes.size() + " OpModes.</font></html>");
                }
                fillOps.run();
            });
            ops.addActionListener(e -> {
                Object sel = ops.getSelectedItem();
                cfg.opMode = (sel == null || NONE.equals(sel)) ? null : sel.toString();
            });
            start.addActionListener(e -> { if (picker.canStart()) { f.dispose(); onStart.accept(cfg); } });
            JLabel title = new JLabel("GuimasLab");
            title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
            JPanel codeBox = new JPanel(new GridLayout(0, 1, 3, 3));
            codeBox.setBorder(BorderFactory.createTitledBorder("Codigo da equipe (Teste de codigo)"));
            JPanel pathRow = new JPanel(new BorderLayout(4, 0));
            pathRow.add(srcField, BorderLayout.CENTER); pathRow.add(browse, BorderLayout.EAST);
            codeBox.add(pathRow); codeBox.add(compile); codeBox.add(ops); codeBox.add(layout); codeBox.add(codeStatus);
            for (java.awt.Component c : new java.awt.Component[] {title, new JLabel("Modo"), round, test, new JLabel("Robos"), r1, r4,
                new JLabel("Alianca"), red, blue, codeBox, status, start}) {
                ((JComponent) c).setAlignmentX(0f);
                side.add(c);
                side.add(javax.swing.Box.createVerticalStrut(5));
            }
            f.add(side, BorderLayout.WEST);
            f.add(fp, BorderLayout.CENTER);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.pack();
            f.setResizable(false);
            f.setLocationRelativeTo(null);
            refresh.run();
            f.setVisible(true);
        };
        if (SwingUtilities.isEventDispatchThread()) build.run(); else SwingUtilities.invokeLater(build);
    }

    /** Gera uma imagem da tela de configuracao (para conferir sem abrir janela). */
    static void snapshot(String file, SimConfig cfg) throws Exception {
        StartPicker picker = new StartPicker(cfg);
        FieldPanel fp = new FieldPanel(cfg, picker, () -> { });
        fp.setSize(PX, PX);
        BufferedImage img = new BufferedImage(PX, PX, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        fp.paint(g);
        g.dispose();
        ImageIO.write(img, "png", new File(file));
    }
}
