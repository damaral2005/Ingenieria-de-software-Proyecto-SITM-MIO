package edu.icesi.sitmmio.visualization;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
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

    // ── Configuracion formato datagrams-MiniPilot.csv ─────────────────────────
    private static final int    ROUTE_COL  = 7;
    private static final int    BUS_COL    = 11;
    private static final int    LAT_COL    = 4;
    private static final int    LON_COL    = 5;
    private static final double COORD_SCALE = 10_000_000.0;
    private static final int    SAMPLE_RATE = 15; // 1 de cada 15 filas

    // ── Datos ─────────────────────────────────────────────────────────────────
    private List<GpsRecord>         allGpsRecords     = new ArrayList<>();
    private List<SpeedRecord>       allSpeedRecords   = new ArrayList<>();
    private List<String>            routes            = new ArrayList<>();
    private String                  selectedRoute     = null;
    private List<GpsRecord>         filteredGps       = new ArrayList<>();
    private List<SpeedRecord>       filteredSpeeds    = new ArrayList<>();

    // ── UI Components ─────────────────────────────────────────────────────────
    private JComboBox<String>       routeCombo;
    private JLabel                  statusLabel;
    private RouteMapPanel           mapPanel;
    private SpeedChartPanel         chartPanel;
    private JTable                  speedTable;
    private DefaultTableModel       tableModel;
    private JLabel                  statsLabel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new VisualizationApp().setVisible(true);
        });
    }

    public VisualizationApp() {
        super("SITM-MIO — Visualizacion de Recorridos y Velocidades");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_PANEL_BG);
        buildUI();
        loadDemoData();
    }

    // ── Construccion de la UI ─────────────────────────────────────────────────
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

        JLabel title = new JLabel("SITM-MIO  —  Sistema Inteligente de Transporte Masivo");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Visualizacion de Recorridos GPS y Velocidades Promedio por Ruta");
        subtitle.setFont(new Font("Arial", Font.PLAIN, 12));
        subtitle.setForeground(new Color(255, 220, 220));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        titlePanel.add(title);
        titlePanel.add(subtitle);
        header.add(titlePanel, BorderLayout.WEST);

        JLabel logo = new JLabel("Icesi University  |  Ingenieria de Software");
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
        sidebar.setBorder(new EmptyBorder(15, 15, 15, 15));
        sidebar.setPreferredSize(new Dimension(280, 0));

        sidebar.add(sectionLabel("FUENTES DE DATOS"));
        sidebar.add(Box.createVerticalStrut(8));

        JButton loadDatagramsBtn = styledButton("Cargar Datagramas CSV", COLOR_MIO_RED);
        loadDatagramsBtn.addActionListener(e -> loadDatagramsDialog());
        sidebar.add(loadDatagramsBtn);
        sidebar.add(Box.createVerticalStrut(6));

        JButton loadSpeedsBtn = styledButton("Cargar Resultados CSV", new Color(80, 120, 180));
        loadSpeedsBtn.addActionListener(e -> loadSpeedsDialog());
        sidebar.add(loadSpeedsBtn);
        sidebar.add(Box.createVerticalStrut(6));

        JButton demoBtn = styledButton("Cargar Datos Demo", new Color(60, 60, 80));
        demoBtn.addActionListener(e -> loadDemoData());
        sidebar.add(demoBtn);

        sidebar.add(Box.createVerticalStrut(20));
        sidebar.add(sectionLabel("FILTROS"));
        sidebar.add(Box.createVerticalStrut(8));

        JLabel routeLabel = new JLabel("Seleccionar Ruta:");
        routeLabel.setForeground(COLOR_TEXT_LIGHT);
        routeLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        routeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(routeLabel);
        sidebar.add(Box.createVerticalStrut(4));

        routeCombo = new JComboBox<>();
        routeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        routeCombo.setBackground(COLOR_MIO_DARK);
        routeCombo.setForeground(Color.WHITE);
        routeCombo.setFont(new Font("Arial", Font.PLAIN, 12));
        routeCombo.addActionListener(e -> onRouteSelected());
        sidebar.add(routeCombo);

        sidebar.add(Box.createVerticalStrut(20));
        sidebar.add(sectionLabel("ESTADISTICAS"));
        sidebar.add(Box.createVerticalStrut(8));

        statsLabel = new JLabel("<html><body style='color:#ddd;font-size:11px'>" +
                "Selecciona una ruta para<br>ver estadisticas.</body></html>");
        statsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(statsLabel);

        sidebar.add(Box.createVerticalGlue());

        sidebar.add(sectionLabel("ACERCA DE"));
        sidebar.add(Box.createVerticalStrut(4));
        JLabel about = new JLabel("<html><body style='color:#aaa;font-size:10px'>" +
                "SITM-MIO v1.0<br>" +
                "Proyecto Final — ISW<br>" +
                "Universidad Icesi 2026</body></html>");
        about.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(about);

        return sidebar;
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new GridLayout(1, 2, 8, 0));
        center.setBackground(COLOR_PANEL_BG);
        center.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel leftPanel = new JPanel(new BorderLayout(0, 6));
        leftPanel.setBackground(COLOR_PANEL_BG);
        JLabel mapTitle = panelTitle("Recorrido GPS de la Ruta");
        leftPanel.add(mapTitle, BorderLayout.NORTH);
        mapPanel = new RouteMapPanel();
        leftPanel.add(mapPanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout(0, 6));
        rightPanel.setBackground(COLOR_PANEL_BG);
        JLabel chartTitle = panelTitle("Velocidad Promedio por Mes (km/h)");
        rightPanel.add(chartTitle, BorderLayout.NORTH);

        chartPanel = new SpeedChartPanel();
        chartPanel.setPreferredSize(new Dimension(0, 300));

        tableModel = new DefaultTableModel(
                new String[]{"Mes", "Vel. Promedio (km/h)", "Distancia (km)", "Segmentos", "Buses"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        speedTable = new JTable(tableModel);
        speedTable.setBackground(COLOR_MIO_DARK);
        speedTable.setForeground(COLOR_TEXT_LIGHT);
        speedTable.setGridColor(COLOR_GRID);
        speedTable.setFont(new Font("Arial", Font.PLAIN, 12));
        speedTable.getTableHeader().setBackground(COLOR_MIO_RED);
        speedTable.getTableHeader().setForeground(Color.WHITE);
        speedTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        speedTable.setRowHeight(24);
        speedTable.setSelectionBackground(new Color(80, 80, 120));

        JScrollPane tableScroll = new JScrollPane(speedTable);
        tableScroll.getViewport().setBackground(COLOR_MIO_DARK);
        tableScroll.setBorder(BorderFactory.createLineBorder(COLOR_GRID));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chartPanel, tableScroll);
        splitPane.setDividerLocation(320);
        splitPane.setBackground(COLOR_PANEL_BG);
        splitPane.setBorder(null);

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

        JLabel hint = new JLabel("SITM-MIO Visualizer  |  Ingenieria de Software — Universidad Icesi");
        hint.setForeground(new Color(100, 100, 120));
        hint.setFont(new Font("Arial", Font.PLAIN, 10));
        bar.add(hint, BorderLayout.EAST);

        return bar;
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────
    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(COLOR_ACCENT);
        l.setFont(new Font("Arial", Font.BOLD, 11));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel panelTitle(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(COLOR_TEXT_LIGHT);
        l.setFont(new Font("Arial", Font.BOLD, 13));
        l.setBorder(new EmptyBorder(0, 4, 4, 0));
        return l;
    }

    private JButton styledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 11));
        btn.setBorder(new EmptyBorder(8, 12, 8, 12));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.addMouseListener(new MouseAdapter() {
            Color original = bg;
            public void mouseEntered(MouseEvent e) { btn.setBackground(bg.brighter()); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(original); }
        });
        return btn;
    }

    // ── Carga de datos ────────────────────────────────────────────────────────
    private void loadDemoData() {
        allGpsRecords.clear();
        allSpeedRecords.clear();
        routes.clear();

        String[] demoRoutes = {"A10", "B15", "C22", "D31", "E45", "F52", "G67", "H73"};
        Random rng = new Random(42);
        double baseLat = 3.4516;
        double baseLon = -76.5320;

        for (String route : demoRoutes) {
            routes.add(route);
            double lat = baseLat + (rng.nextDouble() - 0.5) * 0.15;
            double lon = baseLon + (rng.nextDouble() - 0.5) * 0.15;
            for (int point = 0; point < 80; point++) {
                lat += (rng.nextDouble() - 0.48) * 0.003;
                lon += (rng.nextDouble() - 0.48) * 0.003;
                lat  = Math.max(3.35, Math.min(3.55, lat));
                lon  = Math.max(-76.62, Math.min(-76.45, lon));
                allGpsRecords.add(new GpsRecord(route, "BUS-" + (rng.nextInt(5) + 1), lat, lon));
            }
            for (int month = 1; month <= 6; month++) {
                double speed = 18 + rng.nextDouble() * 20;
                double dist  = 150 + rng.nextDouble() * 300;
                long segs    = 200 + rng.nextInt(500);
                long buses   = 3 + rng.nextInt(8);
                allSpeedRecords.add(new SpeedRecord(route, YearMonth.of(2024, month), speed, dist, segs, buses));
            }
        }

        routeCombo.removeAllItems();
        routeCombo.addItem("— Seleccionar ruta —");
        for (String r : routes) routeCombo.addItem(r);

        statusLabel.setText("Datos demo cargados: " + routes.size() +
                " rutas, " + allGpsRecords.size() + " puntos GPS.");
        mapPanel.clearRoute();
        chartPanel.clearData();
        clearTable();
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

    // ── METODO MODIFICADO: lee el formato real de datagrams-MiniPilot.csv ─────
    private void loadDatagramsFromFile(String path) {
        statusLabel.setText("Cargando datagramas... puede tardar 20-40 segundos.");

        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            final List<GpsRecord>    newRecords = new ArrayList<>();
            final Set<String>        routeSet   = new LinkedHashSet<>();
            int loaded = 0;

            @Override
            protected Void doInBackground() {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(
                            new FileInputStream(path),
                            java.nio.charset.Charset.forName("ISO-8859-1")), 1 << 20)) {

                    String line;
                    int lineNum = 0;
                    while ((line = br.readLine()) != null) {
                        lineNum++;
                        // Tomar 1 de cada SAMPLE_RATE filas para rendimiento
                        if (lineNum % SAMPLE_RATE != 0) continue;

                        String[] parts = line.split(",", -1);
                        if (parts.length <= Math.max(ROUTE_COL, Math.max(BUS_COL,
                                Math.max(LAT_COL, LON_COL)))) continue;

                        try {
                            String routeId = parts[ROUTE_COL].trim();
                            String busId   = parts[BUS_COL].trim();
                            double lat     = Long.parseLong(parts[LAT_COL].trim()) / COORD_SCALE;
                            double lon     = Long.parseLong(parts[LON_COL].trim()) / COORD_SCALE;

                            if (routeId.isEmpty()) continue;
                            if (lat < 3.0 || lat > 4.0 || lon < -77.5 || lon > -75.5) continue;

                            newRecords.add(new GpsRecord(routeId, busId, lat, lon));
                            routeSet.add(routeId);
                            loaded++;
                        } catch (NumberFormatException ignored) {}

                        if (lineNum % 200_000 == 0) {
                            publish("Cargando... " + loaded + " puntos leidos, " + routeSet.size() + " rutas encontradas.");
                        }
                    }
                } catch (IOException ex) {
                    publish("ERROR: " + ex.getMessage());
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                statusLabel.setText(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                allGpsRecords.clear();
                routes.clear();
                allGpsRecords.addAll(newRecords);
                routes.addAll(routeSet);

                routeCombo.removeAllItems();
                routeCombo.addItem("— Seleccionar ruta —");
                for (String r : routes) routeCombo.addItem(r);

                mapPanel.clearRoute();
                chartPanel.clearData();
                clearTable();

                statusLabel.setText("Datagramas cargados: " + loaded +
                        " puntos GPS, " + routes.size() + " rutas reales del MIO.");
            }
        };
        worker.execute();
    }

    private void loadSpeedsFromFile(String path) {
        allSpeedRecords.clear();
        int loaded = 0;
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
                    allSpeedRecords.add(new SpeedRecord(routeId, month, avgSpeed, dist, segs, buses));
                    loaded++;
                } catch (Exception ignored) {}
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error leyendo archivo: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        statusLabel.setText("Resultados de velocidad cargados: " + loaded + " registros.");
        if (selectedRoute != null) onRouteSelected();
    }

    // ── Interaccion ───────────────────────────────────────────────────────────
    private void onRouteSelected() {
        Object sel = routeCombo.getSelectedItem();
        if (sel == null || sel.toString().startsWith("—")) {
            mapPanel.clearRoute();
            chartPanel.clearData();
            clearTable();
            statsLabel.setText("<html><body style='color:#ddd;font-size:11px'>Selecciona una ruta.</body></html>");
            return;
        }
        selectedRoute = sel.toString();

        filteredGps = new ArrayList<>();
        for (GpsRecord r : allGpsRecords)
            if (r.routeId.equals(selectedRoute)) filteredGps.add(r);

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

    private void clearTable() { tableModel.setRowCount(0); }

    private void updateStats() {
        if (filteredSpeeds.isEmpty()) return;
        double maxSpeed = filteredSpeeds.stream().mapToDouble(r -> r.avgSpeedKmh).max().orElse(0);
        double minSpeed = filteredSpeeds.stream().mapToDouble(r -> r.avgSpeedKmh).min().orElse(0);
        double avgSpeed = filteredSpeeds.stream().mapToDouble(r -> r.avgSpeedKmh).average().orElse(0);
        long totalSegs  = filteredSpeeds.stream().mapToLong(r -> r.validSegments).sum();

        statsLabel.setText(String.format(
            "<html><body style='color:#ddd;font-size:11px'>" +
            "<b style='color:#ffb400'>Ruta: %s</b><br>" +
            "Puntos GPS: %d<br>" +
            "Meses con datos: %d<br>" +
            "Vel. max: <b style='color:#ff6464'>%.1f km/h</b><br>" +
            "Vel. min: <b style='color:#64ff96'>%.1f km/h</b><br>" +
            "Vel. promedio: <b>%.1f km/h</b><br>" +
            "Segmentos totales: %d" +
            "</body></html>",
            selectedRoute, filteredGps.size(), filteredSpeeds.size(),
            maxSpeed, minSpeed, avgSpeed, totalSegs));
    }

    // ── Panel del Mapa GPS ────────────────────────────────────────────────────
    class RouteMapPanel extends JPanel {
        private List<GpsRecord> points = new ArrayList<>();
        private double minLat, maxLat, minLon, maxLon;

        RouteMapPanel() {
            setBackground(new Color(15, 20, 35));
            setBorder(BorderFactory.createLineBorder(COLOR_GRID));
            setPreferredSize(new Dimension(500, 500));
        }

        void setRoute(List<GpsRecord> pts) {
            this.points = pts;
            if (!pts.isEmpty()) {
                minLat = pts.stream().mapToDouble(p -> p.lat).min().orElse(3.3);
                maxLat = pts.stream().mapToDouble(p -> p.lat).max().orElse(3.6);
                minLon = pts.stream().mapToDouble(p -> p.lon).min().orElse(-76.7);
                maxLon = pts.stream().mapToDouble(p -> p.lon).max().orElse(-76.4);
                double padLat = (maxLat - minLat) * 0.12 + 0.001;
                double padLon = (maxLon - minLon) * 0.12 + 0.001;
                minLat -= padLat; maxLat += padLat;
                minLon -= padLon; maxLon += padLon;
            }
            repaint();
        }

        void clearRoute() { points = new ArrayList<>(); repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();

            GradientPaint bg = new GradientPaint(0, 0, new Color(10, 15, 30), 0, h, new Color(20, 30, 50));
            g2.setPaint(bg);
            g2.fillRect(0, 0, w, h);

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

            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 1; i < points.size(); i++) {
                GpsRecord prev = points.get(i - 1);
                GpsRecord curr = points.get(i);
                int x1 = lonToX(prev.lon, w), y1 = latToY(prev.lat, h);
                int x2 = lonToX(curr.lon, w), y2 = latToY(curr.lat, h);
                float ratio = (float) i / points.size();
                Color lineColor = blend(new Color(50, 100, 255), COLOR_ROUTE_LINE, ratio);
                g2.setColor(lineColor);
                g2.drawLine(x1, y1, x2, y2);
            }

            for (int i = 0; i < points.size(); i++) {
                GpsRecord p = points.get(i);
                int x = lonToX(p.lon, w), y = latToY(p.lat, h);
                int size = (i == 0 || i == points.size() - 1) ? 10 : 5;
                Color c = (i == 0) ? Color.GREEN : (i == points.size() - 1) ? COLOR_MIO_RED : COLOR_GPS_POINT;
                g2.setColor(c);
                g2.fillOval(x - size/2, y - size/2, size, size);
                g2.setColor(c.darker());
                g2.drawOval(x - size/2, y - size/2, size, size);
            }

            g2.setFont(new Font("Arial", Font.BOLD, 11));
            drawLegendDot(g2, Color.GREEN,     10, h - 60, "Inicio");
            drawLegendDot(g2, COLOR_MIO_RED,   10, h - 42, "Fin");
            drawLegendDot(g2, COLOR_GPS_POINT, 10, h - 24, "Punto GPS (" + points.size() + ")");

            g2.setColor(COLOR_ACCENT);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.drawString("Ruta: " + selectedRoute, 10, 25);
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

    // ── Panel del Grafico de Velocidades ─────────────────────────────────────
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
            maxSpeed = Math.ceil(maxSpeed / 10) * 10 + 5;
            int chartW = w - padL - padR;
            int chartH = h - padT - padB;

            g2.setStroke(new BasicStroke(0.5f));
            for (int i = 0; i <= 5; i++) {
                int y = padT + chartH - (int)(chartH * i / 5.0);
                g2.setColor(COLOR_GRID);
                g2.drawLine(padL, y, padL + chartW, y);
                g2.setColor(new Color(150, 150, 170));
                g2.setFont(new Font("Arial", Font.PLAIN, 10));
                g2.drawString(String.format("%.0f", maxSpeed * i / 5), 5, y + 4);
            }

            g2.setColor(new Color(150, 150, 170));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(padL, padT, padL, padT + chartH);
            g2.drawLine(padL, padT + chartH, padL + chartW, padT + chartH);

            int barW = Math.max(10, chartW / data.size() - 8);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yy");

            for (int i = 0; i < data.size(); i++) {
                SpeedRecord r = data.get(i);
                int x = padL + (i * chartW / data.size()) + (chartW / data.size() - barW) / 2;
                int barH = (int)(chartH * r.avgSpeedKmh / maxSpeed);
                int y = padT + chartH - barH;

                Color barColor;
                if (r.avgSpeedKmh < 20)      barColor = COLOR_SPEED_LOW;
                else if (r.avgSpeedKmh < 32) barColor = COLOR_SPEED_MED;
                else                         barColor = COLOR_SPEED_HIGH;

                GradientPaint gp = new GradientPaint(x, y, barColor.brighter(), x, y + barH, barColor.darker());
                g2.setPaint(gp);
                g2.fillRoundRect(x, y, barW, barH, 4, 4);

                g2.setColor(barColor.brighter());
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(x, y, barW, barH, 4, 4);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 10));
                String val = String.format("%.1f", r.avgSpeedKmh);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(val, x + (barW - fm.stringWidth(val)) / 2, y - 3);

                g2.setColor(COLOR_TEXT_LIGHT);
                g2.setFont(new Font("Arial", Font.PLAIN, 9));
                String label = r.month.format(fmt);
                g2.drawString(label, x + (barW - fm.stringWidth(label)) / 2 - 2, padT + chartH + 14);
            }

            double avg = data.stream().mapToDouble(r -> r.avgSpeedKmh).average().orElse(0);
            int avgY = padT + chartH - (int)(chartH * avg / maxSpeed);
            g2.setColor(COLOR_ACCENT);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[]{6f, 4f}, 0f));
            g2.drawLine(padL, avgY, padL + chartW, avgY);
            g2.setFont(new Font("Arial", Font.BOLD, 10));
            g2.drawString(String.format("Promedio: %.1f km/h", avg), padL + 4, avgY - 4);

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