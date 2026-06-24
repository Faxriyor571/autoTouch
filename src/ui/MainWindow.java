package ui;

import core.ClickService;
import core.CoordinateService;
import core.TimerService;
import model.Coordinate;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class MainWindow extends JFrame {

    // ─── Color Palette ───────────────────────────────────────────
    private static final Color BG_DARK       = new Color(13,  17,  23);
    private static final Color BG_CARD       = new Color(22,  27,  34);
    private static final Color BG_INPUT      = new Color(30,  37,  46);
    private static final Color ACCENT_BLUE   = new Color(88, 166, 255);
    private static final Color ACCENT_GREEN  = new Color(63, 185, 80);
    private static final Color ACCENT_RED    = new Color(248, 81,  73);
    private static final Color ACCENT_ORANGE = new Color(255, 166,  0);
    private static final Color TEXT_PRIMARY  = new Color(230, 237, 243);
    private static final Color TEXT_MUTED    = new Color(125, 133, 144);
    private static final Color BORDER_COLOR  = new Color(48,  54,  61);

    private static final Color[] POINT_COLORS = {
            ACCENT_BLUE, ACCENT_GREEN, ACCENT_ORANGE, ACCENT_RED
    };

    // ─── Services ────────────────────────────────────────────────
    private final CoordinateService coordinateService;
    private final TimerService      timerService;
    private final ClickService      clickService;

    // ─── UI Components ───────────────────────────────────────────
    private JLabel     currentTimeLabel;
    private JTextField targetTimeField;
    private JLabel     statusLabel;
    private JLabel     statusDot;
    private JLabel[]   pointLabels = new JLabel[4];
    private JButton    startButton;
    private JButton    stopButton;

    public MainWindow(CoordinateService coordinateService,
                      TimerService      timerService,
                      ClickService      clickService) {

        this.coordinateService = coordinateService;
        this.timerService      = timerService;
        this.clickService      = clickService;

        setupWindow();
        buildUI();
        startClock();
    }

    // ════════════════════════════════════════════════════════════
    //  WINDOW SETUP
    // ════════════════════════════════════════════════════════════

    private void setupWindow() {
        setTitle("Auto Touch Pro");
        setSize(480, 720);
        setMinimumSize(new Dimension(420, 640));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setBackground(BG_DARK);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                timerService.stopClock();
                timerService.stopCountdown();
            }
        });
    }

    // ════════════════════════════════════════════════════════════
    //  UI BUILD
    // ════════════════════════════════════════════════════════════

    private void buildUI() {
        JPanel root = new JPanel();
        root.setBackground(BG_DARK);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(20, 20, 16, 20));

        root.add(buildHeader());
        root.add(vgap(18));
        root.add(buildClockCard());
        root.add(vgap(12));
        root.add(buildTargetCard());
        root.add(vgap(12));
        root.add(buildCoordinatesCard());
        root.add(vgap(12));
        root.add(buildStatusCard());
        root.add(vgap(18));
        root.add(buildActionButtons());
        root.add(vgap(16));
        root.add(buildFooter());

        setContentPane(root);
        setVisible(true);
    }

    // ════════════════════════════════════════════════════════════
    //  SECTION BUILDERS
    // ════════════════════════════════════════════════════════════

    private JPanel buildHeader() {
        JPanel p = transparent();
        p.setLayout(new BorderLayout());
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel title = new JLabel("Auto Touch Pro");
        title.setFont(font(22, Font.BOLD));
        title.setForeground(TEXT_PRIMARY);

        JLabel badge = pill("v1.0", ACCENT_BLUE);
        JPanel right = transparent();
        right.add(badge);

        p.add(title, BorderLayout.WEST);
        p.add(right,  BorderLayout.EAST);
        return p;
    }

    private JPanel buildClockCard() {
        JPanel card = card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        card.add(sectionLabel("JORIY VAQT"));
        card.add(vgap(10));

        currentTimeLabel = new JLabel("00:00:00.000");
        currentTimeLabel.setFont(new Font("Monospaced", Font.BOLD, 34));
        currentTimeLabel.setForeground(ACCENT_BLUE);
        currentTimeLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(currentTimeLabel);

        return card;
    }

    private JPanel buildTargetCard() {
        JPanel card = card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        card.add(sectionLabel("MAQSAD VAQT"));
        card.add(vgap(10));

        targetTimeField = new JTextField("12:00:00.000");
        targetTimeField.setFont(new Font("Monospaced", Font.PLAIN, 18));
        targetTimeField.setForeground(TEXT_PRIMARY);
        targetTimeField.setBackground(BG_INPUT);
        targetTimeField.setCaretColor(ACCENT_BLUE);
        targetTimeField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        targetTimeField.setAlignmentX(LEFT_ALIGNMENT);
        applyInputBorder(targetTimeField);

        card.add(targetTimeField);
        return card;
    }

    private JPanel buildCoordinatesCard() {
        JPanel card = card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        card.add(sectionLabel("KOORDINATALAR"));
        card.add(vgap(12));

        Coordinate[] pts = coordinateService.getAllPoints();
        for (int i = 0; i < pts.length; i++) {
            card.add(buildPointRow(i, pts[i]));
            if (i < pts.length - 1) card.add(vgap(8));
        }

        return card;
    }

    private JPanel buildPointRow(int index, Coordinate coord) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(BG_INPUT);
        row.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        row.setAlignmentX(LEFT_ALIGNMENT);

        Color accent = POINT_COLORS[index];

        // Left side
        JPanel left = transparent();
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));

        JLabel dot = colorDot(accent);
        JLabel nameLabel = new JLabel(coord.getName());
        nameLabel.setFont(font(13, Font.BOLD));
        nameLabel.setForeground(TEXT_PRIMARY);

        pointLabels[index] = new JLabel(coord.toString());
        pointLabels[index].setFont(font(12, Font.PLAIN));
        pointLabels[index].setForeground(TEXT_MUTED);

        left.add(dot);
        left.add(hgap(10));
        left.add(nameLabel);
        left.add(hgap(10));
        left.add(pointLabels[index]);

        // Save button
        JButton btn = smallButton("Saqlash", accent);
        btn.addActionListener(e -> {
            // Robot API bilan joriy sichqoncha joyi olinadi
            Point pos = clickService.getCurrentMousePosition();
            coordinateService.savePoint(index, pos.x, pos.y);
            pointLabels[index].setText(coord.toString());
        });

        // Hover
        row.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { row.setBackground(new Color(35, 43, 55)); }
            public void mouseExited(MouseEvent e)  { row.setBackground(BG_INPUT); }
        });

        row.add(left, BorderLayout.CENTER);
        row.add(btn,  BorderLayout.EAST);
        return row;
    }

    private JPanel buildStatusCard() {
        JPanel card = card();
        card.setLayout(new BorderLayout());

        card.add(sectionLabel("HOLAT"), BorderLayout.NORTH);

        JPanel inner = transparent();
        inner.setLayout(new BoxLayout(inner, BoxLayout.X_AXIS));
        inner.setBorder(new EmptyBorder(10, 0, 0, 0));

        statusDot = colorDot(ACCENT_RED);

        statusLabel = new JLabel("TO'XTAGAN");
        statusLabel.setFont(font(18, Font.BOLD));
        statusLabel.setForeground(ACCENT_RED);

        inner.add(statusDot);
        inner.add(hgap(10));
        inner.add(statusLabel);
        inner.add(Box.createHorizontalGlue());

        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildActionButtons() {
        JPanel p = transparent();
        p.setLayout(new GridLayout(1, 2, 12, 0));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        startButton = actionButton("BOSHLASH",    ACCENT_GREEN);
        stopButton  = actionButton("TO'XTATISH",  ACCENT_RED);

        stopButton.setEnabled(false);

        startButton.addActionListener(e -> onStart());
        stopButton.addActionListener(e  -> onStop());

        p.add(startButton);
        p.add(stopButton);
        return p;
    }

    private JPanel buildFooter() {
        JPanel p = transparent();
        p.setLayout(new BorderLayout());
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        JLabel left  = new JLabel("Auto Touch Pro");
        JLabel right = new JLabel("Professional Edition");
        left.setFont(font(11, Font.PLAIN));  left.setForeground(TEXT_MUTED);
        right.setFont(font(11, Font.PLAIN)); right.setForeground(TEXT_MUTED);

        p.add(left,  BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    // ════════════════════════════════════════════════════════════
    //  ACTIONS
    // ════════════════════════════════════════════════════════════

    private void onStart() {
        String target = targetTimeField.getText().trim();
        try {
            timerService.startCountdown(target, () ->
                    SwingUtilities.invokeLater(() -> {
                        clickService.clickAll(
                                coordinateService.getAllPoints(), 200
                        );
                        setStatus(false);
                    })
            );
            setStatus(true);
            startButton.setEnabled(false);
            stopButton.setEnabled(true);

        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Xato",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void onStop() {
        timerService.stopCountdown();
        setStatus(false);
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    private void setStatus(boolean running) {
        if (running) {
            statusLabel.setText("ISHLAYAPTI");
            statusLabel.setForeground(ACCENT_GREEN);
            statusDot.setBackground(ACCENT_GREEN);
        } else {
            statusLabel.setText("TO'XTAGAN");
            statusLabel.setForeground(ACCENT_RED);
            statusDot.setBackground(ACCENT_RED);
        }
    }

    private void startClock() {
        timerService.startClock(time ->
                SwingUtilities.invokeLater(() ->
                        currentTimeLabel.setText(time)
                )
        );
    }

    // ════════════════════════════════════════════════════════════
    //  COMPONENT FACTORIES
    // ════════════════════════════════════════════════════════════

    private JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(BG_CARD);
        p.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(16, 18, 16, 18)
        ));
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return p;
    }

    private JPanel transparent() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        return p;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(10, Font.BOLD));
        l.setForeground(TEXT_MUTED);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JLabel colorDot(Color color) {
        JLabel d = new JLabel();
        d.setPreferredSize(new Dimension(10, 10));
        d.setMaximumSize(new Dimension(10, 10));
        d.setMinimumSize(new Dimension(10, 10));
        d.setOpaque(true);
        d.setBackground(color);
        return d;
    }

    private JLabel pill(String text, Color fg) {
        JLabel l = new JLabel(text) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(font(11, Font.BOLD));
        l.setForeground(fg);
        l.setBorder(new EmptyBorder(3, 10, 3, 10));
        l.setOpaque(false);
        return l;
    }

    private JButton smallButton(String text, Color accent) {
        JButton b = new JButton(text) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isPressed()  ? accent.darker()  :
                        getModel().isRollover() ? accent.darker()  : BG_DARK;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(accent);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(font(12, Font.BOLD));
        b.setForeground(accent);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(86, 32));
        return b;
    }

    private JButton actionButton(String text, Color accent) {
        JButton b = new JButton(text) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = !isEnabled()              ? new Color(40, 47, 54) :
                        getModel().isPressed()    ? accent.darker()       :
                                getModel().isRollover()   ? accent.brighter()     : accent;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(font(14, Font.BOLD));
        b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void applyInputBorder(JTextField f) {
        Border normal = new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        );
        Border focused = new CompoundBorder(
                new LineBorder(ACCENT_BLUE, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        );
        f.setBorder(normal);
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { f.setBorder(focused); }
            public void focusLost(FocusEvent e)   { f.setBorder(normal);  }
        });
    }

    // ════════════════════════════════════════════════════════════
    //  UTILITIES
    // ════════════════════════════════════════════════════════════

    private Font   font(int size, int style)  { return new Font("Segoe UI", style, size); }
    private Component vgap(int h) { return Box.createRigidArea(new Dimension(0, h)); }
    private Component hgap(int w) { return Box.createRigidArea(new Dimension(w, 0)); }
}
