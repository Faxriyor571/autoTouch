package ui;

import core.ClickService;
import core.CoordinateService;
import core.HotkeyService;
import core.TimerService;
import model.Coordinate;
import result.AdaptiveLatencyModel;
import result.ResultObservation;
import result.ResultObserverService;
import time.TimeSyncSnapshot;
import time.UzexTimeSyncService;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainWindow extends JFrame {

    private static final Color BG_DARK      = new Color(13,  17,  23);
    private static final Color BG_CARD      = new Color(22,  27,  34);
    private static final Color BG_INPUT     = new Color(30,  37,  46);
    private static final Color ACCENT_BLUE  = new Color(88, 166, 255);
    private static final Color ACCENT_GREEN = new Color(63, 185,  80);
    private static final Color ACCENT_RED   = new Color(248, 81,  73);
    private static final Color ACCENT_AMBER = new Color(255, 166,   0);
    private static final Color TEXT_PRIMARY = new Color(230, 237, 243);
    private static final Color TEXT_MUTED   = new Color(125, 133, 144);
    private static final Color BORDER_COLOR = new Color(48,  54,  61);

    private static final Color[] DOT_COLORS = {
            new Color(88, 166, 255),
            new Color(63, 185,  80),
            new Color(255, 166,   0),
            new Color(248,  81,  73),
            new Color(180, 100, 255)
    };

    // Bitta sichqoncha parallel bosa olmaydi, lekin 0 ms interval barcha
    // nuqtalarni imkon qadar tez ketma-ket bosadi.
    private static final int CLICK_DELAY_MS = 0;
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private static Font sf(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    private final CoordinateService coordinateService;
    private final TimerService      timerService;
    private final ClickService      clickService;
    private final HotkeyService     hotkeyService;
    private final UzexTimeSyncService timeSyncService;
    private final ResultObserverService resultObserverService;
    private final AdaptiveLatencyModel adaptiveLatencyModel;

    private enum State { STOPPED, WAITING, RUNNING }
    private State currentState = State.STOPPED;
    private volatile boolean criticalTiming;

    private JLabel     clockLabel;
    private JLabel     uzexClockLabel;
    private JLabel     syncStatusLabel;
    private JLabel     syncMetricsLabel;
    private JLabel     observerStatusLabel;
    private JLabel     adaptiveBiasLabel;
    private JLabel     adaptiveStatusLabel;
    private JCheckBox  networkCompensationCheck;
    private volatile ResultObservation lastResultObservation;
    private JTextField targetTimeField;
    private JLabel     countdownLabel;
    private JLabel     statusDot;
    private JLabel     statusLabel;
    private JLabel     resultLabel;
    private JPanel     pointsPanel;
    private JButton    startBtn;
    private JButton    stopBtn;

    public MainWindow(CoordinateService coordinateService,
                      TimerService      timerService,
                      ClickService      clickService,
                      HotkeyService     hotkeyService,
                      UzexTimeSyncService timeSyncService,
                      ResultObserverService resultObserverService,
                      AdaptiveLatencyModel adaptiveLatencyModel) {
        this.coordinateService = coordinateService;
        this.timerService      = timerService;
        this.clickService      = clickService;
        this.hotkeyService     = hotkeyService;
        this.timeSyncService   = timeSyncService;
        this.resultObserverService = resultObserverService;
        this.adaptiveLatencyModel = adaptiveLatencyModel;

        setupWindow();
        buildUI();

        timerService.startClock(time -> {
            if (criticalTiming) return;
            SwingUtilities.invokeLater(() -> {
                clockLabel.setText(time);
                TimeSyncSnapshot sync = timeSyncService.getSnapshot();
                String liveClock = resultObserverService.getLastClockText();
                if (liveClock != null && !liveClock.isBlank()) {
                    uzexClockLabel.setText(liveClock);
                } else if (sync.isUsable()) {
                    uzexClockLabel.setText(
                            sync.conservativeServerNow().format(TIME_FORMATTER)
                    );
                }
            });
        }, () -> criticalTiming);

        timeSyncService.start(sync -> {
            if (!criticalTiming) {
                SwingUtilities.invokeLater(() -> updateSyncUi(sync));
            }
        });

        startResultObserver();

        hotkeyService.setUiCallback(ignored -> {
            System.out.println("[UI] refreshPointsPanel() chaqirildi");
            refreshPointsPanel();
            flashTitle();
        });
    }

    private void setupWindow() {
        setTitle("Auto_Click");
        setSize(520, 760);
        setMinimumSize(new Dimension(460, 640));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(BG_DARK);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                hotkeyService.stop();
                timerService.shutdown();
                timeSyncService.shutdown();
                resultObserverService.shutdown();
            }
        });
    }

    private void buildUI() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(BG_DARK);

        JPanel content = new JPanel();
        content.setBackground(BG_DARK);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(12, 12, 12, 12));

        content.add(buildHeader());        content.add(gap(8));
        content.add(buildClockCard());     content.add(gap(6));
        content.add(buildSyncCard());      content.add(gap(6));
        content.add(buildTargetCard());    content.add(gap(6));
        content.add(buildPointsCard());    content.add(gap(6));
        content.add(buildStatusCard());    content.add(gap(6));
        content.add(buildResultCard());    content.add(gap(6));
        content.add(buildActionButtons()); content.add(gap(6));
        content.add(buildFooter());

        JScrollPane scrollPane = new JScrollPane(
                content,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BG_DARK);
        scrollPane.setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        wrapper.add(scrollPane, gbc);

        setContentPane(wrapper);
        setVisible(true);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        p.setPreferredSize(new Dimension(0, 34));
        p.setMinimumSize(new Dimension(0, 34));
        JLabel title = new JLabel("Auto_Click");
        title.setFont(sf(Font.BOLD, 18));
        title.setForeground(TEXT_PRIMARY);
        p.add(title, BorderLayout.WEST);
        p.add(makePill("v3.0", ACCENT_BLUE), BorderLayout.EAST);
        return p;
    }

    private JPanel buildClockCard() {
        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(makeLabel("LOKAL KOMPYUTER VAQTI"));
        card.add(gap(4));
        clockLabel = new JLabel("00:00:00.000");
        clockLabel.setFont(new Font("Monospaced", Font.BOLD, 24));
        clockLabel.setForeground(ACCENT_BLUE);
        clockLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(clockLabel);
        return card;
    }

    private JPanel buildSyncCard() {
        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(makeLabel("UZEX SERVER VAQTI"));
        card.add(gap(4));

        uzexClockLabel = new JLabel("--:--:--.---");
        uzexClockLabel.setFont(new Font("Monospaced", Font.BOLD, 20));
        uzexClockLabel.setForeground(ACCENT_GREEN);
        uzexClockLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(uzexClockLabel);
        card.add(gap(4));

        syncStatusLabel = new JLabel("SINXRONLANMOQDA...");
        syncStatusLabel.setFont(sf(Font.BOLD, 11));
        syncStatusLabel.setForeground(ACCENT_AMBER);
        syncStatusLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(syncStatusLabel);

        syncMetricsLabel = new JLabel("Offset: —  |  RTT: —  |  Jitter: —");
        syncMetricsLabel.setFont(sf(Font.PLAIN, 10));
        syncMetricsLabel.setForeground(TEXT_MUTED);
        syncMetricsLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(syncMetricsLabel);

        adaptiveBiasLabel = new JLabel("Adaptive bias: --");
        adaptiveBiasLabel.setFont(sf(Font.PLAIN, 10));
        adaptiveBiasLabel.setForeground(TEXT_MUTED);
        adaptiveBiasLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(adaptiveBiasLabel);
        card.add(gap(7));

        observerStatusLabel = new JLabel("BROWSER EXTENSION: OFFLINE");
        observerStatusLabel.setFont(sf(Font.BOLD, 10));
        observerStatusLabel.setForeground(ACCENT_AMBER);
        observerStatusLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(observerStatusLabel);

        adaptiveStatusLabel = new JLabel("Adaptive: 3 ta real natija kutilmoqda");
        adaptiveStatusLabel.setFont(sf(Font.PLAIN, 10));
        adaptiveStatusLabel.setForeground(TEXT_MUTED);
        adaptiveStatusLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(adaptiveStatusLabel);
        card.add(gap(7));

        networkCompensationCheck = new JCheckBox(
                "Serverga yetib borish vaqtini kompensatsiya qilish",
                false
        );
        networkCompensationCheck.setOpaque(false);
        networkCompensationCheck.setForeground(TEXT_PRIMARY);
        networkCompensationCheck.setFont(sf(Font.PLAIN, 10));
        networkCompensationCheck.setFocusPainted(false);
        networkCompensationCheck.setAlignmentX(LEFT_ALIGNMENT);
        card.add(networkCompensationCheck);
        return card;
    }

    private void updateSyncUi(TimeSyncSnapshot sync) {
        Color color = switch (sync.status()) {
            case SYNCED -> ACCENT_GREEN;
            case DEGRADED, CALIBRATING -> ACCENT_AMBER;
            case DISCONNECTED -> ACCENT_RED;
        };
        syncStatusLabel.setForeground(color);
        syncStatusLabel.setText(
                sync.status() + (sync.hubConnected() ? "  •  SIGNALR ONLINE" : "  •  HTTP")
        );

        adaptiveBiasLabel.setText(String.format("Adaptive bias: %.0f ms", sync.adaptiveBiasMillis()));
        if (sync.sampleCount() > 0) {
            syncMetricsLabel.setText(String.format(
                    "Offset: %+.1f ms  |  min RTT: %.1f ms  |  Jitter: %.1f ms  |  ±%.1f ms",
                    sync.offsetMillis(), sync.minRttMillis(),
                    sync.jitterMillis(), sync.uncertaintyMillis()
            ));
            String liveClock = resultObserverService.getLastClockText();
            if (liveClock != null && !liveClock.isBlank()) {
                uzexClockLabel.setText(liveClock);
            } else if (sync.isUsable()) {
                uzexClockLabel.setText(
                        sync.conservativeServerNow().format(TIME_FORMATTER)
                );
            }
        } else {
            syncMetricsLabel.setText(sync.message());
            adaptiveBiasLabel.setText(String.format("Adaptive bias: %.0f ms", sync.adaptiveBiasMillis()));
        }
    }

    private void startResultObserver() {
        adaptiveLatencyModel.addListener(adaptive ->
                SwingUtilities.invokeLater(() -> updateAdaptiveUi(adaptive))
        );
        try {
            resultObserverService.start(observation -> {
                lastResultObservation = observation;
                adaptiveLatencyModel.observe(observation);
                if (!criticalTiming) {
                    SwingUtilities.invokeLater(this::updateObserverUi);
                }
            });
        } catch (RuntimeException error) {
            observerStatusLabel.setText("OBSERVER XATOSI: " + error.getMessage());
            observerStatusLabel.setForeground(ACCENT_RED);
        }

        new javax.swing.Timer(1_000, event -> {
            if (!criticalTiming) updateObserverUi();
        }).start();
    }

    private void updateObserverUi() {
        if (!resultObserverService.isExtensionOnline()) {
            observerStatusLabel.setText("BROWSER EXTENSION: OFFLINE");
            observerStatusLabel.setForeground(ACCENT_AMBER);
            return;
        }

        ResultObservation observation = lastResultObservation;
        if (observation != null
                && observation.observedAt().plusSeconds(20).isAfter(Instant.now())) {
            observerStatusLabel.setText(
                    "NATIJA: " + observation.method() + " " + observation.path()
                            + (observation.hasServerTimestamp()
                            ? "  •  TIME TOPILDI"
                            : "  •  TIME YO'Q")
            );
        } else {
            observerStatusLabel.setText("BROWSER EXTENSION: ONLINE");
        }
        observerStatusLabel.setForeground(ACCENT_GREEN);
    }

    private void updateAdaptiveUi(AdaptiveLatencyModel.AdaptiveSnapshot adaptive) {
        if (adaptive.active()) {
            adaptiveStatusLabel.setText(String.format(
                    "Adaptive lead: %+.3f ms  |  %d sample  |  MAD %.3f ms",
                    adaptive.correctionMillis(),
                    adaptive.sampleCount(),
                    adaptive.madMillis()
            ));
            adaptiveStatusLabel.setForeground(ACCENT_GREEN);
        } else {
            adaptiveStatusLabel.setText(
                    "Adaptive: " + adaptive.sampleCount() + "/3 real natija"
            );
            adaptiveStatusLabel.setForeground(TEXT_MUTED);
        }
    }

    private JPanel buildTargetCard() {
        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(makeLabel("UZEX MAQSAD VAQTI  (HH:mm:ss.SSS)"));
        card.add(gap(8));
        targetTimeField = new JTextField("12:00:00.000");
        targetTimeField.setFont(new Font("Monospaced", Font.PLAIN, 16));
        targetTimeField.setForeground(TEXT_PRIMARY);
        targetTimeField.setBackground(BG_INPUT);
        targetTimeField.setCaretColor(ACCENT_BLUE);
        targetTimeField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        targetTimeField.setPreferredSize(new Dimension(0, 40));
        targetTimeField.setAlignmentX(LEFT_ALIGNMENT);
        styleInput(targetTimeField);
        card.add(targetTimeField);
        card.add(gap(6));
        countdownLabel = new JLabel("--:--.---");
        countdownLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        countdownLabel.setForeground(ACCENT_AMBER);
        countdownLabel.setAlignmentX(LEFT_ALIGNMENT);
        JPanel cRow = new JPanel();
        cRow.setOpaque(false);
        cRow.setLayout(new BoxLayout(cRow, BoxLayout.X_AXIS));
        cRow.setAlignmentX(LEFT_ALIGNMENT);
        cRow.add(makeLabel("QOLGAN VAQT:  "));
        cRow.add(countdownLabel);
        card.add(cRow);
        return card;
    }

    private JPanel buildPointsCard() {
        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(LEFT_ALIGNMENT);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        headerRow.add(makeLabel("KOORDINATALAR"), BorderLayout.WEST);
        JLabel hint = new JLabel("F1  →  yangi nuqta qo'shish");
        hint.setFont(sf(Font.PLAIN, 10));
        hint.setForeground(ACCENT_GREEN);
        headerRow.add(hint, BorderLayout.EAST);
        card.add(headerRow);
        card.add(gap(6));
        pointsPanel = new JPanel();
        pointsPanel.setOpaque(false);
        pointsPanel.setLayout(new BoxLayout(pointsPanel, BoxLayout.Y_AXIS));
        pointsPanel.setAlignmentX(LEFT_ALIGNMENT);
        showEmptyHint();
        card.add(pointsPanel);
        return card;
    }

    private JPanel buildStatusCard() {
        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(makeLabel("HOLAT"));
        card.add(gap(8));
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(LEFT_ALIGNMENT);
        statusDot   = makeDot(ACCENT_RED);
        statusLabel = new JLabel("TO'XTAGAN");
        statusLabel.setFont(sf(Font.BOLD, 14));
        statusLabel.setForeground(ACCENT_RED);
        row.add(statusDot);
        row.add(Box.createRigidArea(new Dimension(10, 0)));
        row.add(statusLabel);
        row.add(Box.createHorizontalGlue());
        card.add(row);
        return card;
    }

    private JPanel buildResultCard() {
        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(makeLabel("OXIRGI NATIJA"));
        card.add(gap(8));
        resultLabel = new JLabel("—");
        resultLabel.setFont(sf(Font.PLAIN, 12));
        resultLabel.setForeground(TEXT_MUTED);
        resultLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(resultLabel);
        return card;
    }

    private JPanel buildActionButtons() {
        JPanel p = new JPanel(new GridLayout(1, 2, 12, 0));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        p.setPreferredSize(new Dimension(0, 44));
        p.setAlignmentX(LEFT_ALIGNMENT);
        startBtn = makeBigBtn("BOSHLASH",   ACCENT_GREEN);
        stopBtn  = makeBigBtn("TO'XTATISH", ACCENT_RED);
        stopBtn.setEnabled(false);
        startBtn.addActionListener(e -> onStart());
        stopBtn.addActionListener(e  -> onStop());
        p.add(startBtn);
        p.add(stopBtn);
        return p;
    }

    private JPanel buildFooter() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        p.setPreferredSize(new Dimension(0, 18));
        p.setAlignmentX(LEFT_ALIGNMENT);
        JLabel l = new JLabel("Auto Touch Pro  —  Professional Edition");
        JLabel r = new JLabel("v3.0");
        l.setFont(sf(Font.PLAIN, 10)); l.setForeground(TEXT_MUTED);
        r.setFont(sf(Font.PLAIN, 10)); r.setForeground(TEXT_MUTED);
        p.add(l, BorderLayout.WEST);
        p.add(r, BorderLayout.EAST);
        return p;
    }

    private void refreshPointsPanel() {
        pointsPanel.removeAll();
        List<Coordinate> all = coordinateService.getAll();
        if (all.isEmpty()) {
            showEmptyHint();
        } else {
            for (int i = 0; i < all.size(); i++) {
                if (i > 0) pointsPanel.add(gap(6));
                pointsPanel.add(buildPointRow(all.get(i)));
            }
        }
        pointsPanel.revalidate();
        pointsPanel.repaint();
        pack();
        setLocationRelativeTo(null);
    }

    private void showEmptyHint() {
        JLabel hint = new JLabel("Koordinatalar yo'q  —  F1 bosing");
        hint.setFont(sf(Font.ITALIC, 12));
        hint.setForeground(TEXT_MUTED);
        hint.setAlignmentX(LEFT_ALIGNMENT);
        pointsPanel.add(hint);
    }

    private JPanel buildPointRow(Coordinate coord) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(BG_INPUT);
        row.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        row.setPreferredSize(new Dimension(0, 44));
        row.setAlignmentX(LEFT_ALIGNMENT);

        int idx = parseIndex(coord.getName());
        Color accent = DOT_COLORS[idx % DOT_COLORS.length];

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));

        JLabel nameLabel  = new JLabel(coord.getName());
        nameLabel.setFont(sf(Font.BOLD, 12));
        nameLabel.setForeground(TEXT_PRIMARY);

        JLabel coordLabel = new JLabel(coord.toString());
        coordLabel.setFont(sf(Font.PLAIN, 11));
        coordLabel.setForeground(TEXT_MUTED);

        left.add(makeDot(accent));
        left.add(Box.createRigidArea(new Dimension(10, 0)));
        left.add(nameLabel);
        left.add(Box.createRigidArea(new Dimension(8, 0)));
        left.add(coordLabel);

        JPanel btns = new JPanel();
        btns.setOpaque(false);
        btns.setLayout(new BoxLayout(btns, BoxLayout.X_AXIS));

        JButton testBtn   = makeSmallBtn("Test",      ACCENT_BLUE);
        JButton deleteBtn = makeSmallBtn("O'chirish", ACCENT_RED);

        testBtn.addActionListener(e ->
                new Thread(() -> clickService.clickAt(coord)).start()
        );
        deleteBtn.addActionListener(e -> {
            coordinateService.remove(coord);
            refreshPointsPanel();
        });

        btns.add(testBtn);
        btns.add(Box.createRigidArea(new Dimension(6, 0)));
        btns.add(deleteBtn);

        Color hoverBg = new Color(35, 43, 55);
        row.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { row.setBackground(hoverBg);  }
            @Override public void mouseExited(MouseEvent e)  { row.setBackground(BG_INPUT); }
        });

        row.add(left, BorderLayout.CENTER);
        row.add(btns, BorderLayout.EAST);
        return row;
    }

    private int parseIndex(String name) {
        try {
            String[] parts = name.split(" ");
            return Integer.parseInt(parts[parts.length - 1]) - 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private void onStart() {
        if (coordinateService.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Kamida bitta koordinata saqlang!\nBoshqa oynada F1 tugmasini bosing.",
                    "Koordinata yo'q",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        String targetText = targetTimeField.getText().trim();
        LocalTime targetTime;
        try {
            targetTime = TimerService.parse(targetText);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(), "Xato", JOptionPane.ERROR_MESSAGE);
            return;
        }
        TimeSyncSnapshot armedSync = timeSyncService.getSnapshot();
        if (!armedSync.isUsable()) {
            JOptionPane.showMessageDialog(this,
                    "UZEX vaqti hali sinxronlanmagan. SYNCED holatini kuting.",
                    "Vaqt sinxronlanmagan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!targetTime.isAfter(armedSync.conservativeServerNow().toLocalTime())) {
            JOptionPane.showMessageDialog(this,
                    "Belgilangan UZEX vaqti o'tib ketgan. Kelajak vaqtini kiriting.",
                    "Vaqt o'tib ketgan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // F1 orqali ro'yxat o'zgarib qolmasligi uchun start paytidagi nusxa.
        List<Coordinate> pointsToClick = List.copyOf(coordinateService.getAll());
        boolean compensateNetwork = networkCompensationCheck.isSelected();
        long targetServerEpochNanos = armedSync.targetServerEpochNanos(targetTime);
        long adaptiveCorrectionNanos = adaptiveLatencyModel.correctionNanos();
        long firstTargetNano = armedSync.targetNanoTime(
                targetServerEpochNanos,
                compensateNetwork
        ) - adaptiveCorrectionNanos;
        if (firstTargetNano - System.nanoTime() < 250_000_000L) {
            JOptionPane.showMessageDialog(this,
                    "Target juda yaqin. Kamida 300 ms oldin BOSHLASH tugmasini bosing.",
                    "Target juda yaqin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        adaptiveLatencyModel.arm(targetServerEpochNanos);
        setState(State.WAITING);
        criticalTiming = false;
        resultLabel.setText("—");
        resultLabel.setForeground(TEXT_MUTED);
        timerService.startCountdownDynamic(
                () -> {
                    TimeSyncSnapshot latest = timeSyncService.getSnapshot();
                    TimeSyncSnapshot usable = latest.isUsable() ? latest : armedSync;
                    return usable.targetNanoTime(
                            targetServerEpochNanos,
                            compensateNetwork
                    ) - adaptiveCorrectionNanos;
                },
                remainingMs -> {
                    if (remainingMs <= 200) {
                        criticalTiming = true;
                        return;
                    }
                    SwingUtilities.invokeLater(() ->
                            countdownLabel.setText(TimerService.formatRemainingMs(remainingMs))
                    );
                },
                () -> {
                    criticalTiming = true;
                    clickService.prepareFirstClick(pointsToClick.get(0));
                },
                timerDelayMs -> {
                    // Bu callback precision thread'da ishlaydi. Uni UI navbatiga
                    // yuborish yoki yangi thread ochish real clickni kechiktiradi.
                    ClickService.ClickReport report;
                    try {
                        report = clickService.clickAllPrepared(
                                pointsToClick,
                                CLICK_DELAY_MS
                        );
                    } catch (RuntimeException clickError) {
                        criticalTiming = false;
                        SwingUtilities.invokeLater(() -> {
                            setState(State.STOPPED);
                            countdownLabel.setText("--:--.---");
                            resultLabel.setForeground(ACCENT_RED);
                            resultLabel.setText("Click xatosi: " + clickError.getMessage());
                        });
                        return;
                    }
                    criticalTiming = false;

                    TimeSyncSnapshot resultSync = timeSyncService.getSnapshot();
                    if (!resultSync.isUsable()) resultSync = armedSync;
                    long dispatchServerEpochNanos = resultSync.estimatedServerEpochNanos(
                            report.firstClickNanoTime()
                    );
                    long predictedArrivalEpochNanos = dispatchServerEpochNanos
                            + (compensateNetwork
                            ? resultSync.estimatedOutboundLatencyNanos()
                            : 0L);
                    double arrivalDeltaMs =
                            (predictedArrivalEpochNanos - targetServerEpochNanos) / 1_000_000.0;
                    double clickBurstMs = Math.max(0,
                            report.lastClickNanoTime()
                                    - report.firstClickNanoTime()) / 1_000_000.0;
                    Instant firstClickInstant = report.wallClockAnchor().minusNanos(
                            report.wallClockAnchorNanoTime()
                                    - report.firstClickNanoTime()
                    );
                    String firstClickTime = firstClickInstant
                            .atZone(ZoneId.systemDefault())
                            .format(TIME_FORMATTER);
                    String serverDispatchTime = TimeSyncSnapshot
                            .fromEpochNanos(dispatchServerEpochNanos)
                            .atZone(TimeSyncSnapshot.UZEX_ZONE)
                            .format(TIME_FORMATTER);

                    SwingUtilities.invokeLater(() -> {
                        setState(State.STOPPED);
                        countdownLabel.setText("--:--.---");
                        double absoluteDeltaMs = Math.abs(arrivalDeltaMs);
                        resultLabel.setForeground(
                                absoluteDeltaMs <= 5  ? ACCENT_GREEN :
                                        absoluteDeltaMs <= 20 ? ACCENT_AMBER : ACCENT_RED
                        );
                        resultLabel.setText(String.format(
                                "<html>Lokal click: %s &nbsp;|&nbsp; UZEX: %s"
                                        + "<br>Taxminiy server arrival farqi: %+.3f ms"
                                        + "<br>%d ta bosish davomiyligi: %.3f ms</html>",
                                firstClickTime, serverDispatchTime, arrivalDeltaMs,
                                report.clickCount(), clickBurstMs
                        ));
                    });
                }
        );
    }

    private void onStop() {
        timerService.stopCountdown();
        criticalTiming = false;
        setState(State.STOPPED);
        countdownLabel.setText("--:--.---");
    }

    private void setState(State state) {
        currentState = state;
        startBtn.setEnabled(state == State.STOPPED);
        stopBtn.setEnabled(state  != State.STOPPED);
        networkCompensationCheck.setEnabled(state == State.STOPPED);
        switch (state) {
            case STOPPED:
                statusLabel.setText("TO'XTAGAN");
                statusLabel.setForeground(ACCENT_RED);
                statusDot.setBackground(ACCENT_RED);
                break;
            case WAITING:
                statusLabel.setText("KUTILMOQDA");
                statusLabel.setForeground(ACCENT_AMBER);
                statusDot.setBackground(ACCENT_AMBER);
                break;
            case RUNNING:
                statusLabel.setText("ISHLAYAPTI");
                statusLabel.setForeground(ACCENT_GREEN);
                statusDot.setBackground(ACCENT_GREEN);
                break;
        }
    }

    private void flashTitle() {
        List<Coordinate> all = coordinateService.getAll();
        if (all.isEmpty()) return;
        Coordinate last = all.get(all.size() - 1);
        setTitle(last.getName() + " saqlandi  —  " + last.toString());
        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            SwingUtilities.invokeLater(() -> setTitle("Auto Touch Pro"));
        }).start();
    }

    private JPanel makeCard() {
        JPanel p = new JPanel();
        p.setBackground(BG_CARD);
        p.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Short.MAX_VALUE));
        return p;
    }

    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(sf(Font.BOLD, 10));
        l.setForeground(TEXT_MUTED);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JLabel makeDot(Color color) {
        JLabel d = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        d.setPreferredSize(new Dimension(10, 10));
        d.setMaximumSize(new Dimension(10, 10));
        d.setMinimumSize(new Dimension(10, 10));
        d.setOpaque(false);
        d.setBackground(color);
        return d;
    }

    private JLabel makePill(String text, Color fg) {
        JLabel l = new JLabel(text) {
            @Override
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
        l.setFont(sf(Font.BOLD, 11));
        l.setForeground(fg);
        l.setBorder(new EmptyBorder(4, 10, 4, 10));
        l.setOpaque(false);
        return l;
    }

    private JButton makeSmallBtn(String text, Color accent) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = !isEnabled()            ? new Color(40, 47, 54) :
                        getModel().isPressed()  ? accent.darker()       :
                                getModel().isRollover() ? accent                : BG_DARK;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(isEnabled() ? accent : BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(sf(Font.BOLD, 11));
        b.setForeground(accent);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(82, 28));
        return b;
    }

    private JButton makeBigBtn(String text, Color accent) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = !isEnabled()            ? new Color(40, 47, 54) :
                        getModel().isPressed()  ? accent.darker()       :
                                getModel().isRollover() ? accent.brighter()     : accent;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(sf(Font.BOLD, 14));
        b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void styleInput(JTextField f) {
        Border normal  = new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true), new EmptyBorder(8, 12, 8, 12));
        Border focused = new CompoundBorder(
                new LineBorder(ACCENT_BLUE,  1, true), new EmptyBorder(8, 12, 8, 12));
        f.setBorder(normal);
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { f.setBorder(focused); }
            @Override public void focusLost(FocusEvent e)   { f.setBorder(normal);  }
        });
    }

    private Component gap(int size) {
        return Box.createRigidArea(new Dimension(0, size));
    }
}

