package edu.icesi.sitmmio.visualization;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.nio.file.*;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class VisualizationApp extends JFrame {

    // ── Colores corporativos MIO ──────────────────────────────────────────────
    private static final Color COLOR_MIO_RED     = new Color(200, 30, 30);
    private static final Color COLOR_MIO_DARK    = new Color(40, 40, 60);
    private static final Color COLOR_MIO_LIGHT   = new Color(245, 245, 250);
    private static final Color COLOR_PANEL_BG    = new Color(30, 30, 50);
    private static final Color COLOR_ACCENT      = new Color(255, 180, 0);
    private static final Color COLOR_ROUTE_LINE  = new Color(255, 100, 50);
    private static final Color COLOR_GPS_POINT   = new Color(50, 200, 255);
    private static final Color COLOR_GRID        = new Color(60, 60, 80);
    private static final Color COLOR_TEXT_LIGHT  = new Color(220, 220, 230);
    private static final Color COLOR_SPEED_BAR   = new Color(50, 180, 120);
    private static final Color COLOR_SPEED_LOW   = new Color(50, 180, 120);
    private static final Color COLOR_SPEED_MED   = new Color(255, 180, 0);
    private static final Color COLOR_SPEED_HIGH  = new Color(200, 30, 30);

    // ── Datos ─────────────────────────────────────────────────────────────────
    // Por rendimiento: NO se guardan los millones de registros completos.
    // Se mantiene una muestra representativa (reservoir sampling) por ruta,
    // máx. MAX_POINTS_PER_ROUTE puntos, ordenados por orden de aparición.
    private static final int MAX_POINTS_PER_ROUTE = 600;

    private Map<String, List<GpsRecord>>  gpsByRoute        = new LinkedHashMap<>();
    private List<SpeedRecord>             allSpeedRecords   = new ArrayList<>();
    private List<String>                  routes            = new ArrayList<>();
    private String                        selectedRoute     = null;
    private List<GpsRecord>               filteredGps       = new ArrayList<>();
    private List<SpeedRecord>             filteredSpeeds    = new ArrayList<>();

    // ── UI Components ─────────────────────────────────────────────────────────
    private JComboBox<String>       routeCombo;
    private JLabel                  statusLabel;
    private RouteMapPanel           mapPanel;
    private SpeedChartPanel         chartPanel;
    private JTable                  speedTable;
    private DefaultTableModel       tableModel;
    private JLabel                  statsLabel;

    // ── Paths (configurables) ─────────────────────────────────────────────────
    private String datagramsPath = "/opt/sitm-mio/datagrams-MiniPilot.csv";
    private String speedsPath    = "results/route_month_speeds_v1.csv";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ignored) {}
            new VisualizationApp().setVisible(true);
        });
    }

    public VisualizationApp() {
        super("SITM-MIO — Visualización de Recorridos y Velocidades");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_PANEL_BG);
        buildUI();
        loadDemoData();
    }

    // ── Construcción de la UI ─────────────────────────────────────────────────
    private void buildUI() {
        setLayout(new BorderLayout(0, 0));

        add(buildHeader(),    BorderLayout.NORTH);
        add(buildSidebar(),   BorderLayout.WEST);
        add(buildCenter(),    BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_MIO_RED);
        header.setBorder(new EmptyBorder(12, 20, 12, 20));
        header.setPreferredSize(new Dimension(0, 65));

        JLabel title = new JLabel("SITM-MIO   —   Sistema Inteligente de Transporte Masivo");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Visualización de Recorridos GPS y Velocidades Promedio por Ruta");
        subtitle.setFont(new Font("Arial", Font.PLAIN, 12));
        subtitle.setForeground(new Color(255, 220, 220));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        titlePanel.add(title);
        titlePanel.add(subtitle);
        header.add(titlePanel, BorderLayout.WEST);

        JLabel logo = new JLabel("Icesi University  |  Ingeniería de Software");
        logo.setFont(new Font("Arial", Font.ITALIC, 11));
        logo.setForeground(new Color(255, 200, 200));
        logo.setHorizontalAlignment(SwingConstants.RIGHT);
        header.add(logo, BorderLayout.EAST);

        return header;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(COLOR_MIO_DARK);
        sidebar.setBorder(new EmptyBorder(18, 16, 18, 16));
        sidebar.setPreferredSize(new Dimension(290, 0));

        sidebar.add(sectionLabel("FUENTES DE DATOS"));
        sidebar.add(Box.createVerticalStrut(10));

        JButton loadDatagramsBtn = styledButton("Cargar datagramas CSV", COLOR_MIO_RED);
        loadDatagramsBtn.addActionListener(e -> loadDatagramsDialog());
        sidebar.add(loadDatagramsBtn);
        sidebar.add(Box.createVerticalStrut(8));

        JButton loadSpeedsBtn = styledButton("Cargar resultados CSV", new Color(70, 110, 175));
        loadSpeedsBtn.addActionListener(e -> loadSpeedsDialog());
        sidebar.add(loadSpeedsBtn);
        sidebar.add(Box.createVerticalStrut(8));

        JButton demoBtn = styledButton("Restablecer datos demo", new Color(70, 70, 95));
        demoBtn.addActionListener(e -> loadDemoData());
        sidebar.add(demoBtn);

        sidebar.add(Box.createVerticalStrut(24));
        sidebar.add(sectionLabel("FILTROS"));
        sidebar.add(Box.createVerticalStrut(10));

        JLabel routeLabel = new JLabel("Ruta");
        routeLabel.setForeground(new Color(160, 165, 185));
        routeLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        routeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(routeLabel);
        sidebar.add(Box.createVerticalStrut(5));

        routeCombo = new JComboBox<>();
        routeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        routeCombo.setBackground(new Color(50, 50, 75));
        routeCombo.setForeground(Color.WHITE);
        routeCombo.setFont(new Font("Arial", Font.PLAIN, 13));
        routeCombo.setFocusable(false);
        routeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setBackground(isSelected ? new Color(90, 90, 130) : new Color(50, 50, 75));
                l.setForeground(Color.WHITE);
                l.setBorder(new EmptyBorder(4, 8, 4, 8));
                return l;
            }
        });
        routeCombo.addActionListener(e -> onRouteSelected());
        sidebar.add(routeCombo);

        sidebar.add(Box.createVerticalStrut(24));
        sidebar.add(sectionLabel("ESTADISTICAS"));
        sidebar.add(Box.createVerticalStrut(10));

        statsLabel = new JLabel();
        statsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(statsLabel);
        setStatsPlaceholder();

        sidebar.add(Box.createVerticalGlue());

        sidebar.add(new JSeparator() {{ setForeground(COLOR_GRID); setMaximumSize(new Dimension(Integer.MAX_VALUE, 1)); }});
        sidebar.add(Box.createVerticalStrut(10));

        JLabel about = new JLabel("<html><body style='color:#7d7f99;font-size:10px;line-height:150%'>" +
                "<b style='color:#aab0cc'>SITM-MIO</b> &nbsp;v1.0<br>" +
                "Proyecto Final &middot; Ingenieria de Software<br>" +
                "Universidad Icesi &middot; 2026</body></html>");
        about.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(about);

        return sidebar;
    }

    private void setStatsPlaceholder() {
        statsLabel.setText("<html><body style='width:240px'>" +
                "<div style='background:#34344f;border-radius:6px;padding:14px 12px;color:#8a8eac;font-size:12px;line-height:150%;font-family:Arial'>" +
                "Selecciona una ruta en el filtro de arriba para ver sus estadisticas." +
                "</div></body></html>");
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new GridLayout(1, 2, 8, 0));
        center.setBackground(COLOR_PANEL_BG);
        center.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Panel izquierdo: mapa
        JPanel leftPanel = new JPanel(new BorderLayout(0, 6));
        leftPanel.setBackground(COLOR_PANEL_BG);

        JPanel mapHeader = new JPanel(new BorderLayout());
        mapHeader.setOpaque(false);

        JLabel mapTitle = panelTitle("Recorrido GPS de la ruta");
        mapHeader.add(mapTitle, BorderLayout.WEST);

        JPanel zoomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        zoomBar.setOpaque(false);
        JButton zoomOutBtn = zoomButton("-");
        JButton zoomResetBtn = zoomButton("Reset");
        JButton zoomInBtn = zoomButton("+");
        zoomBar.add(zoomOutBtn);
        zoomBar.add(zoomResetBtn);
        zoomBar.add(zoomInBtn);
        mapHeader.add(zoomBar, BorderLayout.EAST);

        leftPanel.add(mapHeader, BorderLayout.NORTH);

        mapPanel = new RouteMapPanel();
        leftPanel.add(mapPanel, BorderLayout.CENTER);

        zoomInBtn.addActionListener(e -> mapPanel.zoomBy(1.25));
        zoomOutBtn.addActionListener(e -> mapPanel.zoomBy(1 / 1.25));
        zoomResetBtn.addActionListener(e -> mapPanel.resetView());

        // Panel derecho: chart + tabla
        JPanel rightPanel = new JPanel(new BorderLayout(0, 6));
        rightPanel.setBackground(COLOR_PANEL_BG);

        JLabel chartTitle = panelTitle("Velocidad promedio por mes (km/h)");
        rightPanel.add(chartTitle, BorderLayout.NORTH);

        chartPanel = new SpeedChartPanel();
        chartPanel.setPreferredSize(new Dimension(0, 300));

        tableModel = new DefaultTableModel(
                new String[]{"Mes", "Vel. (km/h)", "Distancia (km)", "Segmentos", "Buses"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        speedTable = new JTable(tableModel);
        speedTable.setFont(new Font("Arial", Font.PLAIN, 13));
        speedTable.setRowHeight(28);
        speedTable.setShowGrid(true);
        speedTable.setGridColor(COLOR_GRID);
        speedTable.setIntercellSpacing(new Dimension(1, 1));
        speedTable.setSelectionBackground(new Color(90, 90, 135));
        speedTable.setSelectionForeground(Color.WHITE);
        speedTable.setFillsViewportHeight(true);
        speedTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        int[] prefWidths = {90, 110, 120, 100, 80};
        for (int c = 0; c < prefWidths.length; c++) {
            speedTable.getColumnModel().getColumn(c).setPreferredWidth(prefWidths[c]);
        }

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setOpaque(true);
                if (isSelected) {
                    setBackground(new Color(90, 90, 135));
                    setForeground(Color.WHITE);
                } else {
                    setBackground(row % 2 == 0 ? new Color(45, 45, 68) : new Color(40, 40, 60));
                    setForeground(COLOR_TEXT_LIGHT);
                }
                setBorder(new EmptyBorder(0, 10, 0, 10));
                setHorizontalAlignment(column == 0 ? SwingConstants.LEFT : SwingConstants.RIGHT);
                return c;
            }
        };
        for (int c = 0; c < speedTable.getColumnCount(); c++) {
            speedTable.getColumnModel().getColumn(c).setCellRenderer(cellRenderer);
        }

        JTableHeader header = speedTable.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                l.setOpaque(true);
                l.setBackground(COLOR_MIO_RED);
                l.setForeground(Color.WHITE);
                l.setFont(new Font("Arial", Font.BOLD, 12));
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(220, 80, 80)),
                        new EmptyBorder(6, 10, 6, 10)));
                l.setHorizontalAlignment(column == 0 ? SwingConstants.LEFT : SwingConstants.RIGHT);
                return l;
            }
        });
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 34));

        JScrollPane tableScroll = new JScrollPane(speedTable);
        tableScroll.getViewport().setBackground(new Color(40, 40, 60));
        tableScroll.setBorder(BorderFactory.createLineBorder(COLOR_GRID));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chartPanel, tableScroll);
        splitPane.setDividerLocation(320);
        splitPane.setDividerSize(8);
        splitPane.setBackground(COLOR_PANEL_BG);
        splitPane.setOpaque(false);
        splitPane.setBorder(null);
        splitPane.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                return new javax.swing.plaf.basic.BasicSplitPaneDivider(this) {
                    @Override public void paint(Graphics g) {
                        g.setColor(COLOR_PANEL_BG);
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }
                };
            }
        });

        rightPanel.add(splitPane, BorderLayout.CENTER);

        center.add(leftPanel);
        center.add(rightPanel);
        return center;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(20, 20, 35));
        bar.setBorder(new EmptyBorder(4, 15, 4, 15));

        statusLabel = new JLabel("Listo. Carga un archivo CSV o usa los datos demo.");
        statusLabel.setForeground(new Color(150, 200, 150));
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        bar.add(statusLabel, BorderLayout.WEST);

        JLabel hint = new JLabel("SITM-MIO Visualizer  |  Ingeniería de Software — Universidad Icesi");
        hint.setForeground(new Color(100, 100, 120));
        hint.setFont(new Font("Arial", Font.PLAIN, 10));
        bar.add(hint, BorderLayout.EAST);

        return bar;
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────
    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(150, 155, 180));
        l.setFont(new Font("Arial", Font.BOLD, 11));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel panelTitle(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(COLOR_TEXT_LIGHT);
        l.setFont(new Font("Arial", Font.BOLD, 14));
        l.setBorder(new EmptyBorder(0, 4, 6, 0));
        return l;
    }

    private JButton zoomButton(String text) {
        JButton btn = new JButton(text);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(true);
        btn.setBackground(new Color(50, 50, 75));
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 11));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_GRID),
                new EmptyBorder(4, 10, 4, 10)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(75, 75, 105)); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(new Color(50, 50, 75)); }
        });
        return btn;
    }

    private JButton styledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(false);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setBorder(new EmptyBorder(10, 14, 10, 14));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bg.brighter()); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(bg); }
        });
        return btn;
    }

    // ── Carga de datos ────────────────────────────────────────────────────────
    private void loadDemoData() {
        gpsByRoute.clear();
        allSpeedRecords.clear();
        routes.clear();

        // Rutas demo simulando el MIO de Cali
        String[] demoRoutes = {"A10", "B15", "C22", "D31", "E45", "F52", "G67", "H73"};

        Random rng = new Random(42);

        // Coordenadas base de Cali
        double baseLat = 3.4516;
        double baseLon = -76.5320;

        for (String route : demoRoutes) {
            routes.add(route);
            List<GpsRecord> pts = new ArrayList<>();
            // Generar trayectoria GPS para cada ruta
            double lat = baseLat + (rng.nextDouble() - 0.5) * 0.15;
            double lon = baseLon + (rng.nextDouble() - 0.5) * 0.15;

            for (int point = 0; point < 80; point++) {
                lat += (rng.nextDouble() - 0.48) * 0.003;
                lon += (rng.nextDouble() - 0.48) * 0.003;
                lat  = Math.max(3.35, Math.min(3.55, lat));
                lon  = Math.max(-76.62, Math.min(-76.45, lon));
                pts.add(new GpsRecord(route, "BUS-" + (rng.nextInt(5) + 1), lat, lon));
            }
            gpsByRoute.put(route, pts);

            // Generar velocidades por mes
            for (int month = 1; month <= 6; month++) {
                double speed = 18 + rng.nextDouble() * 20;
                double dist  = 150 + rng.nextDouble() * 300;
                long segs    = 200 + rng.nextInt(500);
                long buses   = 3 + rng.nextInt(8);
                allSpeedRecords.add(new SpeedRecord(route,
                        YearMonth.of(2024, month), speed, dist, segs, buses));
            }
        }

        refreshRouteCombo();

        statusLabel.setText("Datos demo cargados: " + routes.size() +
                " rutas, " + allSpeedRecords.size() + " registros de velocidad.");

        mapPanel.clearRoute();
        chartPanel.clearData();
        clearTable();
        setStatsPlaceholder();
    }

    private void refreshRouteCombo() {
        String previous = selectedRoute;
        routeCombo.removeAllItems();
        routeCombo.addItem("- Seleccionar ruta -");
        for (String r : routes) routeCombo.addItem(r);
        if (previous != null && routes.contains(previous)) {
            routeCombo.setSelectedItem(previous);
        }
    }

    private void loadDatagramsDialog() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccionar datagrams CSV");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadDatagramsFromFile(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void loadSpeedsDialog() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccionar resultados CSV (route_month_speeds)");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadSpeedsFromFile(fc.getSelectedFile().getAbsolutePath());
        }
    }

    /**
     * Carga datagramas con un solo pase streaming sobre el archivo (BufferedReader).
     * No se guardan los millones de filas: se mantiene un reservoir sample de hasta
     * MAX_POINTS_PER_ROUTE puntos por ruta (orden de llegada), seguro para archivos
     * de 8M+ filas porque la memoria usada es proporcional a (#rutas x muestra), no al total.
     * Corre en background con barra de progreso para no congelar la UI.
     */
    private void loadDatagramsFromFile(String path) {
        File file = new File(path);
        long fileSize = file.length();

        JDialog progressDialog = buildProgressDialog("Cargando datagramas...");

        SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
            Map<String, List<GpsRecord>> result = new LinkedHashMap<>();
            Map<String, Integer> seenCount = new HashMap<>();
            long loaded = 0;
            long skippedBadCoords = 0;

            @Override
            protected Void doInBackground() {
                Random rng = new Random(7);
                long bytesRead = 0;
                int lastPct = -1;

                try (BufferedReader br = Files.newBufferedReader(Paths.get(path))) {
                    String line;
                    boolean first = true;
                    while ((line = br.readLine()) != null) {
                        bytesRead += line.length() + 1;
                        if (first) { first = false; continue; } // skip header

                        if (isCancelled()) return null;

                        String[] parts = line.split(",", -1);
                        if (parts.length < 6) continue;
                        try {
                            String routeId = parts[0].trim();
                            String busId   = parts[1].trim();
                            double lat     = Double.parseDouble(parts[3].trim());
                            double lon     = Double.parseDouble(parts[4].trim());
                            if (lat < 3.0 || lat > 4.0 || lon < -77.5 || lon > -75.5) {
                                skippedBadCoords++;
                                continue;
                            }

                            List<GpsRecord> bucket = result.computeIfAbsent(routeId, k -> new ArrayList<>());
                            int seen = seenCount.merge(routeId, 1, Integer::sum);

                            // Reservoir sampling: mantiene una muestra uniforme de tamaño fijo
                            if (bucket.size() < MAX_POINTS_PER_ROUTE) {
                                bucket.add(new GpsRecord(routeId, busId, lat, lon));
                            } else {
                                int j = rng.nextInt(seen);
                                if (j < MAX_POINTS_PER_ROUTE) {
                                    bucket.set(j, new GpsRecord(routeId, busId, lat, lon));
                                }
                            }
                            loaded++;
                        } catch (NumberFormatException ignored) {}

                        if (fileSize > 0) {
                            int pct = (int) Math.min(100, (bytesRead * 100L) / fileSize);
                            if (pct != lastPct) {
                                lastPct = pct;
                                publish(pct);
                            }
                        }
                    }
                } catch (IOException ex) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(VisualizationApp.this,
                            "Error leyendo archivo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
                }
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                int pct = chunks.get(chunks.size() - 1);
                updateProgressDialog(progressDialog, pct);
            }

            @Override
            protected void done() {
                progressDialog.dispose();
                if (isCancelled()) {
                    statusLabel.setText("Carga de datagramas cancelada.");
                    return;
                }
                gpsByRoute = result;
                routes = new ArrayList<>(result.keySet());
                Collections.sort(routes);
                refreshRouteCombo();
                statusLabel.setText(String.format(
                        "Datagramas procesados: %,d filas leidas, %,d rutas, %,d fuera de Cali descartadas. " +
                                "Muestra: hasta %d puntos por ruta.",
                        loaded, routes.size(), skippedBadCoords, MAX_POINTS_PER_ROUTE));
            }
        };

        worker.execute();
        progressDialog.setVisible(true); // bloquea hasta dispose() en done()
    }

    /**
     * Carga resultados de velocidad. Este archivo es pequeño (agregado por ruta/mes),
     * por lo que se lee completo sin sampling, pero igual en background por seguridad.
     */
    private void loadSpeedsFromFile(String path) {
        JDialog progressDialog = buildProgressDialog("Cargando resultados...");

        SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
            List<SpeedRecord> result = new ArrayList<>();
            int loaded = 0;

            @Override
            protected Void doInBackground() {
                try (BufferedReader br = Files.newBufferedReader(Paths.get(path))) {
                    String line;
                    boolean first = true;
                    while ((line = br.readLine()) != null) {
                        if (first) { first = false; continue; }
                        String[] parts = line.split(",", -1);
                        if (parts.length < 8) continue;
                        try {
                            String routeId  = parts[0].trim();
                            YearMonth month = YearMonth.parse(parts[1].trim());
                            double avgSpeed = Double.parseDouble(parts[4].trim());
                            double dist     = Double.parseDouble(parts[2].trim());
                            long segs       = Long.parseLong(parts[6].trim());
                            long buses      = Long.parseLong(parts[7].trim());
                            result.add(new SpeedRecord(routeId, month, avgSpeed, dist, segs, buses));
                            loaded++;
                        } catch (Exception ignored) {}
                    }
                } catch (IOException ex) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(VisualizationApp.this,
                            "Error leyendo archivo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
                }
                publish(100);
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                updateProgressDialog(progressDialog, chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                progressDialog.dispose();
                allSpeedRecords = result;
                statusLabel.setText("Resultados cargados: " + loaded + " registros de velocidad.");
                if (selectedRoute != null) onRouteSelected();
            }
        };

        worker.execute();
        progressDialog.setVisible(true);
    }

    private JDialog buildProgressDialog(String title) {
        JDialog dialog = new JDialog(this, "Procesando", true);
        dialog.setUndecorated(false);
        dialog.setSize(380, 130);
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(16, 18, 16, 18));
        content.setBackground(COLOR_MIO_DARK);

        JLabel label = new JLabel(title);
        label.setForeground(COLOR_TEXT_LIGHT);
        label.setFont(new Font("Arial", Font.BOLD, 13));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(label);
        content.add(Box.createVerticalStrut(10));

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(0);
        bar.setStringPainted(true);
        bar.setName("progressBar");
        content.add(bar);
        content.add(Box.createVerticalStrut(10));

        JLabel hint = new JLabel("Procesando en segundo plano (sample por ruta para mantener rendimiento)...");
        hint.setForeground(new Color(150, 155, 180));
        hint.setFont(new Font("Arial", Font.PLAIN, 10));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(hint);

        dialog.setContentPane(content);
        return dialog;
    }

    private void updateProgressDialog(JDialog dialog, int pct) {
        for (Component c : ((JPanel) dialog.getContentPane()).getComponents()) {
            if (c instanceof JProgressBar) {
                ((JProgressBar) c).setValue(pct);
            }
        }
    }

    // ── Interacción ───────────────────────────────────────────────────────────
    private void onRouteSelected() {
        Object sel = routeCombo.getSelectedItem();
        if (sel == null || sel.toString().startsWith("—") || sel.toString().startsWith("-")) {
            mapPanel.clearRoute();
            chartPanel.clearData();
            clearTable();
            setStatsPlaceholder();
            return;
        }
        selectedRoute = sel.toString();

        filteredGps = gpsByRoute.getOrDefault(selectedRoute, Collections.emptyList());

        filteredSpeeds = new ArrayList<>();
        for (SpeedRecord r : allSpeedRecords)
            if (r.routeId.equals(selectedRoute)) filteredSpeeds.add(r);
        filteredSpeeds.sort(Comparator.comparing(r -> r.month));

        mapPanel.setRoute(filteredGps);
        chartPanel.setData(filteredSpeeds);
        updateTable();
        updateStats();

        statusLabel.setText("Ruta: " + selectedRoute + " — " + filteredGps.size() +
                " puntos GPS, " + filteredSpeeds.size() + " meses con datos.");
    }

    private void updateTable() {
        tableModel.setRowCount(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");
        for (SpeedRecord r : filteredSpeeds) {
            tableModel.addRow(new Object[]{
                    r.month.format(fmt),
                    String.format("%.2f", r.avgSpeedKmh),
                    String.format("%.1f", r.totalDistKm),
                    r.validSegments,
                    r.observedBuses
            });
        }
    }

    private void clearTable() {
        tableModel.setRowCount(0);
    }

    private void updateStats() {
        if (filteredSpeeds.isEmpty()) {
            setStatsPlaceholder();
            return;
        }
        double maxSpeed = filteredSpeeds.stream().mapToDouble(r -> r.avgSpeedKmh).max().orElse(0);
        double minSpeed = filteredSpeeds.stream().mapToDouble(r -> r.avgSpeedKmh).min().orElse(0);
        double avgSpeed = filteredSpeeds.stream().mapToDouble(r -> r.avgSpeedKmh).average().orElse(0);
        long totalSegs  = filteredSpeeds.stream().mapToLong(r -> r.validSegments).sum();

        String row = "<div style='display:block;padding:3px 0;font-size:12px;color:#9a9eb8'>%s: " +
                "<span style='color:%s;font-weight:bold'>%s</span></div>";

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='width:250px;font-family:Arial'>");
        sb.append("<div style='background:#34344f;border-radius:6px;padding:10px 12px'>");
        sb.append(String.format("<div style='color:#ffb400;font-size:14px;font-weight:bold;margin-bottom:8px'>Ruta %s</div>", selectedRoute));
        sb.append(String.format(row, "Puntos GPS",         "#e8e9f5", String.valueOf(filteredGps.size())));
        sb.append(String.format(row, "Meses con datos",    "#e8e9f5", String.valueOf(filteredSpeeds.size())));
        sb.append(String.format(row, "Velocidad maxima",   "#ff8b7a", String.format("%.1f km/h", maxSpeed)));
        sb.append(String.format(row, "Velocidad minima",   "#7adba0", String.format("%.1f km/h", minSpeed)));
        sb.append(String.format(row, "Velocidad promedio", "#ffd166", String.format("%.1f km/h", avgSpeed)));
        sb.append(String.format(row, "Segmentos totales",  "#e8e9f5", String.valueOf(totalSegs)));
        sb.append("</div></body></html>");

        statsLabel.setText(sb.toString());
    }

    // ── Panel del Mapa GPS ────────────────────────────────────────────────────
    class RouteMapPanel extends JPanel {
        private List<GpsRecord> points = new ArrayList<>();
        private double minLat, maxLat, minLon, maxLon;
        private double baseMinLat, baseMaxLat, baseMinLon, baseMaxLon;

        // Estado de zoom/pan
        private double zoom = 1.0;
        private double panX = 0, panY = 0; // desplazamiento en pixeles de pantalla
        private Point lastDrag = null;

        RouteMapPanel() {
            setBackground(new Color(15, 20, 35));
            setBorder(BorderFactory.createLineBorder(COLOR_GRID));
            setPreferredSize(new Dimension(500, 500));
            setFocusable(true);

            // Zoom con scroll del mouse
            addMouseWheelListener(e -> {
                requestFocusInWindow();
                double oldZoom = zoom;
                double factor = (e.getWheelRotation() < 0) ? 1.2 : 1 / 1.2;
                double newZoom = oldZoom * factor;
                newZoom = Math.max(0.5, Math.min(newZoom, 60.0));

                // Mantener el punto bajo el cursor fijo al hacer zoom
                Point p = e.getPoint();
                double scaleChange = newZoom / oldZoom;
                panX = p.x - (p.x - panX) * scaleChange;
                panY = p.y - (p.y - panY) * scaleChange;

                zoom = newZoom;
                repaint();
                e.consume();
            });

            // Pan con drag del mouse
            MouseAdapter dragHandler = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                    lastDrag = e.getPoint();
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
                @Override public void mouseReleased(MouseEvent e) {
                    lastDrag = null;
                    setCursor(Cursor.getDefaultCursor());
                }
                @Override public void mouseDragged(MouseEvent e) {
                    if (lastDrag != null) {
                        panX += e.getX() - lastDrag.x;
                        panY += e.getY() - lastDrag.y;
                        lastDrag = e.getPoint();
                        repaint();
                    }
                }
            };
            addMouseListener(dragHandler);
            addMouseMotionListener(dragHandler);

            // Doble click para resetear vista
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        zoom = 1.0;
                        panX = 0;
                        panY = 0;
                        repaint();
                    }
                }
            });
        }

        void setRoute(List<GpsRecord> pts) {
            this.points = pts;
            if (!pts.isEmpty()) {
                baseMinLat = pts.stream().mapToDouble(p -> p.lat).min().orElse(3.3);
                baseMaxLat = pts.stream().mapToDouble(p -> p.lat).max().orElse(3.6);
                baseMinLon = pts.stream().mapToDouble(p -> p.lon).min().orElse(-76.7);
                baseMaxLon = pts.stream().mapToDouble(p -> p.lon).max().orElse(-76.4);
                double padLat = (baseMaxLat - baseMinLat) * 0.12 + 0.001;
                double padLon = (baseMaxLon - baseMinLon) * 0.12 + 0.001;
                baseMinLat -= padLat; baseMaxLat += padLat;
                baseMinLon -= padLon; baseMaxLon += padLon;
                minLat = baseMinLat; maxLat = baseMaxLat;
                minLon = baseMinLon; maxLon = baseMaxLon;
            }
            // Resetear vista al cambiar de ruta
            zoom = 1.0;
            panX = 0;
            panY = 0;
            repaint();
        }

        void clearRoute() {
            points = new ArrayList<>();
            zoom = 1.0; panX = 0; panY = 0;
            repaint();
        }

        void zoomBy(double factor) {
            double oldZoom = zoom;
            double newZoom = Math.max(0.5, Math.min(oldZoom * factor, 60.0));
            double cx = getWidth() / 2.0, cy = getHeight() / 2.0;
            double scaleChange = newZoom / oldZoom;
            panX = cx - (cx - panX) * scaleChange;
            panY = cy - (cy - panY) * scaleChange;
            zoom = newZoom;
            repaint();
        }

        void resetView() {
            zoom = 1.0;
            panX = 0;
            panY = 0;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();

            // Fondo degradado
            GradientPaint bg = new GradientPaint(0, 0, new Color(10, 15, 30),
                    0, h, new Color(20, 30, 50));
            g2.setPaint(bg);
            g2.fillRect(0, 0, w, h);

            // Grilla
            g2.setColor(COLOR_GRID);
            g2.setStroke(new BasicStroke(0.5f));
            for (int x = 0; x < w; x += 40) g2.drawLine(x, 0, x, h);
            for (int y = 0; y < h; y += 40) g2.drawLine(0, y, w, y);

            if (points.isEmpty()) {
                g2.setColor(new Color(100, 100, 120));
                g2.setFont(new Font("Arial", Font.PLAIN, 14));
                String msg = "Selecciona una ruta para ver el recorrido GPS";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
                return;
            }

            // Aplicar transformación de zoom/pan
            Graphics2D g2t = (Graphics2D) g2.create();
            g2t.translate(panX, panY);
            g2t.scale(zoom, zoom);

            // Dibujar línea de ruta
            g2t.setStroke(new BasicStroke((float)(2.5f / zoom), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 1; i < points.size(); i++) {
                GpsRecord prev = points.get(i - 1);
                GpsRecord curr = points.get(i);
                int x1 = lonToX(prev.lon, w), y1 = latToY(prev.lat, h);
                int x2 = lonToX(curr.lon, w), y2 = latToY(curr.lat, h);

                float ratio = (float) i / points.size();
                Color lineColor = blend(new Color(50, 100, 255), COLOR_ROUTE_LINE, ratio);
                g2t.setColor(lineColor);
                g2t.drawLine(x1, y1, x2, y2);
            }

            // Dibujar puntos GPS
            for (int i = 0; i < points.size(); i++) {
                GpsRecord p = points.get(i);
                int x = lonToX(p.lon, w), y = latToY(p.lat, h);
                double size = ((i == 0 || i == points.size() - 1) ? 10 : 5) / zoom;
                Color c = (i == 0) ? Color.GREEN : (i == points.size() - 1) ? COLOR_MIO_RED : COLOR_GPS_POINT;
                g2t.setColor(c);
                g2t.fillOval((int)(x - size/2), (int)(y - size/2), (int)size, (int)size);
                g2t.setColor(c.darker());
                g2t.drawOval((int)(x - size/2), (int)(y - size/2), (int)size, (int)size);
            }
            g2t.dispose();

            // Leyenda (sin transformar, siempre fija)
            g2.setFont(new Font("Arial", Font.BOLD, 11));
            drawLegendDot(g2, Color.GREEN,       10, h - 78, "Inicio");
            drawLegendDot(g2, COLOR_MIO_RED,     10, h - 60, "Fin");
            drawLegendDot(g2, COLOR_GPS_POINT,   10, h - 42, "Puntos GPS (muestra de " + points.size() + ")");

            // Nombre de ruta
            g2.setColor(COLOR_ACCENT);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.drawString("Ruta: " + selectedRoute, 10, 25);

            // Indicador de zoom y ayuda
            g2.setColor(new Color(150, 150, 170));
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            String zoomMsg = String.format("Zoom: %.1fx  |  Scroll: zoom  ·  Arrastrar: mover  ·  Doble click: reset", zoom);
            FontMetrics fmZ = g2.getFontMetrics();
            g2.drawString(zoomMsg, w - fmZ.stringWidth(zoomMsg) - 10, h - 10);
        }

        private void drawLegendDot(Graphics2D g2, Color c, int x, int y, String label) {
            g2.setColor(c);
            g2.fillOval(x, y, 10, 10);
            g2.setColor(COLOR_TEXT_LIGHT);
            g2.drawString(label, x + 16, y + 10);
        }

        private int lonToX(double lon, int w) {
            return (int) ((lon - minLon) / (maxLon - minLon) * (w - 20) + 10);
        }

        private int latToY(double lat, int h) {
            return (int) ((1.0 - (lat - minLat) / (maxLat - minLat)) * (h - 20) + 10);
        }

        private Color blend(Color a, Color b, float t) {
            return new Color(
                    (int)(a.getRed()   * (1-t) + b.getRed()   * t),
                    (int)(a.getGreen() * (1-t) + b.getGreen() * t),
                    (int)(a.getBlue()  * (1-t) + b.getBlue()  * t));
        }
    }

    // ── Panel del Gráfico de Velocidades ─────────────────────────────────────
    class SpeedChartPanel extends JPanel {
        private List<SpeedRecord> data = new ArrayList<>();

        SpeedChartPanel() {
            setBackground(new Color(15, 20, 35));
            setBorder(BorderFactory.createLineBorder(COLOR_GRID));
        }

        void setData(List<SpeedRecord> d) { this.data = d; repaint(); }
        void clearData() { data = new ArrayList<>(); repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int padL = 65, padR = 20, padT = 20, padB = 50;

            // Fondo
            g2.setColor(new Color(15, 20, 35));
            g2.fillRect(0, 0, w, h);

            if (data.isEmpty()) {
                g2.setColor(new Color(100, 100, 120));
                g2.setFont(new Font("Arial", Font.PLAIN, 13));
                String msg = "Selecciona una ruta para ver velocidades";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
                return;
            }

            double maxSpeed = data.stream().mapToDouble(r -> r.avgSpeedKmh).max().orElse(60);
            double minSpeedVal = data.stream().mapToDouble(r -> r.avgSpeedKmh).min().orElse(0);
            double avgSpeedVal = data.stream().mapToDouble(r -> r.avgSpeedKmh).average().orElse(0);
            double range = Math.max(maxSpeed - minSpeedVal, 1.0);
            // Umbrales relativos al propio rango de datos: el tercio inferior es "lento",
            // el tercio superior es "rapido", el medio es "normal".
            double lowThreshold  = minSpeedVal + range / 3.0;
            double highThreshold = minSpeedVal + range * 2.0 / 3.0;
            maxSpeed = Math.ceil(maxSpeed / 10) * 10 + 5;
            int chartW = w - padL - padR;
            int chartH = h - padT - padB;

            // Grilla horizontal
            g2.setStroke(new BasicStroke(0.5f));
            for (int i = 0; i <= 5; i++) {
                int y = padT + chartH - (int)(chartH * i / 5.0);
                g2.setColor(COLOR_GRID);
                g2.drawLine(padL, y, padL + chartW, y);
                g2.setColor(new Color(150, 150, 170));
                g2.setFont(new Font("Arial", Font.PLAIN, 10));
                g2.drawString(String.format("%.0f", maxSpeed * i / 5), 5, y + 4);
            }

            // Ejes
            g2.setColor(new Color(150, 150, 170));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(padL, padT, padL, padT + chartH);
            g2.drawLine(padL, padT + chartH, padL + chartW, padT + chartH);

            // Barras
            int barW = Math.max(10, chartW / data.size() - 8);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yy");

            for (int i = 0; i < data.size(); i++) {
                SpeedRecord r = data.get(i);
                int x = padL + (i * chartW / data.size()) + (chartW / data.size() - barW) / 2;
                int barH = (int)(chartH * r.avgSpeedKmh / maxSpeed);
                int y = padT + chartH - barH;

                // Color según velocidad relativa al rango de esta ruta
                Color barColor;
                if (range < 0.01) {
                    barColor = COLOR_SPEED_MED; // todos los meses iguales
                } else if (r.avgSpeedKmh <= lowThreshold)  barColor = COLOR_SPEED_LOW;
                else if (r.avgSpeedKmh >= highThreshold)   barColor = COLOR_SPEED_HIGH;
                else                                        barColor = COLOR_SPEED_MED;

                // Gradiente en la barra
                GradientPaint gp = new GradientPaint(x, y, barColor.brighter(), x, y + barH, barColor.darker());
                g2.setPaint(gp);
                g2.fillRoundRect(x, y, barW, barH, 4, 4);

                g2.setColor(barColor.brighter());
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(x, y, barW, barH, 4, 4);

                // Valor encima de la barra (o adentro si no hay espacio)
                String val = String.format("%.1f", r.avgSpeedKmh);
                g2.setFont(new Font("Arial", Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                int labelY = y - 5;
                if (labelY < padT + 12) {
                    g2.setColor(Color.WHITE);
                    labelY = y + 14;
                } else {
                    g2.setColor(COLOR_TEXT_LIGHT);
                }
                g2.drawString(val, x + (barW - fm.stringWidth(val)) / 2, labelY);

                // Label mes
                g2.setColor(COLOR_TEXT_LIGHT);
                g2.setFont(new Font("Arial", Font.PLAIN, 9));
                String label = r.month.format(fmt);
                g2.drawString(label, x + (barW - fm.stringWidth(label)) / 2 - 2, padT + chartH + 14);
            }

            // Línea de promedio
            double avg = data.stream().mapToDouble(r -> r.avgSpeedKmh).average().orElse(0);
            int avgY = padT + chartH - (int)(chartH * avg / maxSpeed);
            g2.setColor(COLOR_ACCENT);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[]{6f, 4f}, 0f));
            g2.drawLine(padL, avgY, padL + chartW, avgY);

            // Etiqueta de promedio en una caja fija arriba a la derecha (evita solapamientos)
            String avgLabel = String.format("Promedio: %.1f km/h", avg);
            g2.setFont(new Font("Arial", Font.BOLD, 11));
            FontMetrics fmAvg = g2.getFontMetrics();
            int boxW = fmAvg.stringWidth(avgLabel) + 16;
            int boxH = 22;
            int boxX = padL + chartW - boxW;
            int boxY = padT - 2;
            g2.setColor(new Color(40, 40, 60, 230));
            g2.fillRoundRect(boxX, boxY, boxW, boxH, 6, 6);
            g2.setColor(COLOR_ACCENT);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(boxX, boxY, boxW, boxH, 6, 6);
            g2.drawString(avgLabel, boxX + 8, boxY + 15);

            // Título Y
            g2.setColor(new Color(150, 150, 170));
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            AffineTransform orig = g2.getTransform();
            g2.rotate(-Math.PI / 2);
            g2.drawString("Velocidad (km/h)", -(padT + chartH / 2 + 40), 12);
            g2.setTransform(orig);
        }
    }

    // ── Modelos de datos ──────────────────────────────────────────────────────
    static class GpsRecord {
        String routeId, busId;
        double lat, lon;
        GpsRecord(String r, String b, double la, double lo) {
            routeId = r; busId = b; lat = la; lon = lo;
        }
    }

    static class SpeedRecord {
        String routeId;
        YearMonth month;
        double avgSpeedKmh, totalDistKm;
        long validSegments, observedBuses;
        SpeedRecord(String r, YearMonth m, double s, double d, long seg, long buses) {
            routeId = r; month = m; avgSpeedKmh = s; totalDistKm = d;
            validSegments = seg; observedBuses = buses;
        }
    }
}