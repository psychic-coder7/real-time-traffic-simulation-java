// ===================== GUI.java =====================
package org.example;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Hashtable;
import java.util.Locale;

public final class GUI {

    private GUI() {}

    // ===================== UI Theme =====================
    private static final Color BG_DARK = new Color(0x0F172A);
    private static final Color BG_PANEL = new Color(0x111827);
    private static final Color ACCENT_BLUE = new Color(0x3B82F6);
    private static final Color ACCENT_GREEN = new Color(0x22C55E);
    private static final Color ACCENT_RED = new Color(0xEF4444);
    private static final Color TEXT_MUTED = new Color(0x9CA3AF);
    private static final Color BORDER_COL = new Color(0x1F2937);

    // ===================== FILTER STATE =====================
    public static class VehicleFilter implements MapVisualisation.Filter {
        public volatile boolean showCars = true;
        public volatile boolean showTrucks = true;
        public volatile boolean showBuses = true;
        public volatile double minSpeedMps = 0.0;

        private boolean allowsType(String type) {
            if (Main.TYPE_CAR.equals(type)) return showCars;
            if (Main.TYPE_TRUCK.equals(type)) return showTrucks;
            if (Main.TYPE_BUS.equals(type)) return showBuses;
            return true;
        }

        @Override
        public boolean allows(String type, double speedMps) {
            return allowsType(type) && speedMps >= minSpeedMps;
        }
    }

    public static final VehicleFilter FILTER = new VehicleFilter();

    // ===================== UI Labels =====================
    private static final JLabel activeVehiclesLabel = new JLabel("Active Vehicles (all): 0");
    private static final JLabel visibleVehiclesLabel = new JLabel("Visible Vehicles (filtered): 0");
    private static final JLabel byTypeLabel = new JLabel("By Type: car=0 truck=0 bus=0");

    private static final JLabel avgWaitLabel = new JLabel("Avg Wait Time: 0.0 s");
    private static final JLabel congestionLabel = new JLabel("Congestion Index: 0.00");
    private static final JLabel throughputLabel = new JLabel("Throughput: 0 v/h");
    private static final JLabel meanSpeedLabel = new JLabel("Mean Speed: 0.0 m/s");

    private static final JLabel tlStateLabel = new JLabel("TL State: -");

    public static void launch() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception e) { Logging.LOG.log(java.util.logging.Level.WARNING, "LookAndFeel set failed", e); }

        JFrame frame = new JFrame("Traffic Grid Simulation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1220, 680);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(BG_DARK);

        MapVisualisation.MapPanel mapPanel = new MapVisualisation.MapPanel(FILTER);
        mapPanel.setBackground(Color.WHITE);
        frame.add(mapPanel, BorderLayout.CENTER);

        Font titleFont = new Font("SansSerif", Font.BOLD, 13);

        // ===== Left controls =====
        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setBackground(BG_PANEL);
        styleTitledBorder(controls, "Simulation Controls");
        controls.setPreferredSize(new Dimension(400, 0));

        JButton startBtn = new JButton("Start");
        JButton stopBtn = new JButton("Stop");

        JLabel speedLabel = new JLabel("Speed");
        JSlider simSpeedSlider = new JSlider(1, 10, 1);
        simSpeedSlider.setMajorTickSpacing(1);
        simSpeedSlider.setPaintTicks(true);

        Hashtable<Integer, JLabel> speedLabelTable = new Hashtable<>();
        JLabel minL = new JLabel("1x"), maxL = new JLabel("10x");
        minL.setForeground(Color.WHITE); maxL.setForeground(Color.WHITE);
        speedLabelTable.put(1, minL); speedLabelTable.put(10, maxL);
        simSpeedSlider.setLabelTable(speedLabelTable);
        simSpeedSlider.setPaintLabels(true);
        simSpeedSlider.setBackground(BG_PANEL);
        simSpeedSlider.setForeground(Color.WHITE);
        simSpeedSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        simSpeedSlider.setMaximumSize(new Dimension(320, 44));

        JButton exportBtn = new JButton("Export CSV");
        JButton exportPdfBtn = new JButton("Export PDF");

        JLabel routeLabel = new JLabel("Select scenario (same destination, 4 long variants)");
        JComboBox<VehicleInjection.RouteDef> routeCombo = new JComboBox<>();
        routeCombo.setEnabled(false);
        routeCombo.setMaximumSize(new Dimension(360, 30));
        routeCombo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel numVehiclesLabel = new JLabel("Vehicles");
        JTextField numVehiclesField = new JTextField("10");
        numVehiclesField.setFont(new Font("SansSerif", Font.BOLD, 14));
        numVehiclesField.setAlignmentX(Component.LEFT_ALIGNMENT);
        numVehiclesField.setMaximumSize(new Dimension(160, 28));

        JLabel vehicleTypeLabel = new JLabel("Spawn type");
        JButton carBtn = new JButton("Car");
        JButton truckBtn = new JButton("Truck");
        JButton busBtn = new JButton("Bus");

        JPanel vehicleTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        vehicleTypePanel.setBackground(BG_PANEL);
        vehicleTypePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        vehicleTypePanel.add(carBtn);
        vehicleTypePanel.add(truckBtn);
        vehicleTypePanel.add(busBtn);

        // ===== Filter UI =====
        JLabel filterTitle = new JLabel("Map Filters");
        JCheckBox showCars = styleCheckBox(new JCheckBox("Show Cars", true));
        JCheckBox showTrucks = styleCheckBox(new JCheckBox("Show Trucks", true));
        JCheckBox showBuses = styleCheckBox(new JCheckBox("Show Buses", true));

        JLabel minSpeedTitle = new JLabel("Min Speed Filter");
        JLabel minSpeedValue = new JLabel(">= 0.0 m/s (0 km/h)");
        minSpeedValue.setForeground(new Color(220,220,220));
        minSpeedValue.setFont(new Font("SansSerif", Font.PLAIN, 12));
        minSpeedValue.setAlignmentX(Component.LEFT_ALIGNMENT);

        JSlider minSpeedSlider = new JSlider(0, 35, 0);
        minSpeedSlider.setMajorTickSpacing(5);
        minSpeedSlider.setPaintTicks(true);
        minSpeedSlider.setBackground(BG_PANEL);
        minSpeedSlider.setForeground(Color.WHITE);
        minSpeedSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        minSpeedSlider.setMaximumSize(new Dimension(320, 44));

        // ===== TLS UI =====
        JLabel tlLabel = new JLabel("Traffic Light");
        JComboBox<TrafficControl.TlsItem> tlCombo = new JComboBox<>();
        tlCombo.setEnabled(false);
        tlCombo.setMaximumSize(new Dimension(360, 30));
        tlCombo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton tlRedBtn = new JButton("Red");
        JButton tlGreenBtn = new JButton("Green");
        JButton tlResetBtn = new JButton("Reset");

        JPanel tlButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tlButtonPanel.setBackground(BG_PANEL);
        tlButtonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tlButtonPanel.add(tlRedBtn);
        tlButtonPanel.add(tlGreenBtn);
        tlButtonPanel.add(tlResetBtn);

        JCheckBox ruleBasedTlsCb = styleCheckBox(new JCheckBox("Rule-based TLS (stop/go)", false));

        // style
        styleSectionLabel(speedLabel, titleFont);
        styleSectionLabel(routeLabel, new Font("SansSerif", Font.BOLD, 12));
        styleSectionLabel(numVehiclesLabel, titleFont);
        styleSectionLabel(vehicleTypeLabel, titleFont);
        styleSectionLabel(filterTitle, titleFont);
        styleSectionLabel(minSpeedTitle, new Font("SansSerif", Font.BOLD, 12));
        styleSectionLabel(tlLabel, titleFont);

        stylePrimaryButton(startBtn, ACCENT_GREEN);
        stylePrimaryButton(stopBtn, ACCENT_RED);
        stylePrimaryButton(exportBtn, ACCENT_BLUE);
        stylePrimaryButton(exportPdfBtn, ACCENT_BLUE);

        stylePrimaryButton(carBtn, ACCENT_BLUE);
        stylePrimaryButton(truckBtn, ACCENT_BLUE);
        stylePrimaryButton(busBtn, ACCENT_BLUE);

        stylePrimaryButton(tlRedBtn, ACCENT_RED);
        stylePrimaryButton(tlGreenBtn, ACCENT_GREEN);
        stylePrimaryButton(tlResetBtn, ACCENT_BLUE);

        // ===== Compact layout =====
        controls.add(Box.createVerticalStrut(6));
        controls.add(rowPanel(startBtn, stopBtn));
        controls.add(Box.createVerticalStrut(6));
        controls.add(rowPanel(exportBtn, exportPdfBtn));

        controls.add(Box.createVerticalStrut(10));
        controls.add(speedLabel);
        controls.add(Box.createVerticalStrut(4));
        controls.add(simSpeedSlider);

        controls.add(Box.createVerticalStrut(10));
        controls.add(routeLabel);
        controls.add(Box.createVerticalStrut(4));
        controls.add(routeCombo);

        controls.add(Box.createVerticalStrut(10));
        controls.add(numVehiclesLabel);
        controls.add(Box.createVerticalStrut(4));
        controls.add(numVehiclesField);

        controls.add(Box.createVerticalStrut(10));
        controls.add(vehicleTypeLabel);
        controls.add(Box.createVerticalStrut(4));
        controls.add(vehicleTypePanel);

        controls.add(Box.createVerticalStrut(12));
        controls.add(filterTitle);
        controls.add(Box.createVerticalStrut(4));
        controls.add(showCars);
        controls.add(showTrucks);
        controls.add(showBuses);

        controls.add(Box.createVerticalStrut(8));
        controls.add(minSpeedTitle);
        controls.add(Box.createVerticalStrut(2));
        controls.add(minSpeedValue);
        controls.add(Box.createVerticalStrut(2));
        controls.add(minSpeedSlider);

        controls.add(Box.createVerticalStrut(12));
        controls.add(tlLabel);
        controls.add(Box.createVerticalStrut(4));
        controls.add(tlCombo);
        controls.add(Box.createVerticalStrut(4));
        controls.add(tlButtonPanel);

        controls.add(Box.createVerticalStrut(6));
        controls.add(ruleBasedTlsCb);

        controls.add(Box.createVerticalGlue());

        JScrollPane controlsScroll = new JScrollPane(controls,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        controlsScroll.setBorder(null);
        controlsScroll.getVerticalScrollBar().setUnitIncrement(14);
        controlsScroll.getViewport().setBackground(BG_PANEL);
        controlsScroll.setPreferredSize(new Dimension(420, 0));
        frame.add(controlsScroll, BorderLayout.WEST);

        // ===== Right metrics + chart =====
        JPanel metrics = new JPanel();
        metrics.setLayout(new BoxLayout(metrics, BoxLayout.Y_AXIS));
        metrics.setBackground(BG_PANEL);
        styleTitledBorder(metrics, "Metrics");

        Font metricsFont = new Font("SansSerif", Font.BOLD, 12);
        for (JLabel l : new JLabel[]{
                activeVehiclesLabel, visibleVehiclesLabel, byTypeLabel,
                avgWaitLabel, congestionLabel, throughputLabel, meanSpeedLabel,
                tlStateLabel
        }) {
            l.setForeground(Color.WHITE); l.setFont(metricsFont);
        }

        MapVisualisation.TrendChartPanel trendChart = new MapVisualisation.TrendChartPanel(120);
        trendChart.setAlignmentX(Component.LEFT_ALIGNMENT);

        metrics.add(Box.createVerticalStrut(6));
        metrics.add(trendChart);
        metrics.add(Box.createVerticalStrut(8));

        metrics.add(activeVehiclesLabel);
        metrics.add(Box.createVerticalStrut(4));
        metrics.add(visibleVehiclesLabel);
        metrics.add(Box.createVerticalStrut(4));
        metrics.add(byTypeLabel);

        metrics.add(Box.createVerticalStrut(8));
        metrics.add(avgWaitLabel);
        metrics.add(Box.createVerticalStrut(4));
        metrics.add(congestionLabel);
        metrics.add(Box.createVerticalStrut(4));
        metrics.add(throughputLabel);
        metrics.add(Box.createVerticalStrut(4));
        metrics.add(meanSpeedLabel);

        metrics.add(Box.createVerticalStrut(10));
        metrics.add(tlStateLabel);
        metrics.add(Box.createVerticalGlue());

        frame.add(metrics, BorderLayout.EAST);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        Runnable onStopped = () -> {
            frame.dispose();
            System.exit(0);
        };

        // traffic control + live connection
        TrafficControl trafficControl = new TrafficControl(tlCombo, tlStateLabel);

        LiveConnectionSumo live = new LiveConnectionSumo(
                frame,
                mapPanel,
                trendChart,
                FILTER,
                activeVehiclesLabel,
                visibleVehiclesLabel,
                byTypeLabel,
                avgWaitLabel,
                congestionLabel,
                throughputLabel,
                meanSpeedLabel,
                tlStateLabel,
                routeCombo,
                tlCombo,
                trafficControl,
                onStopped
        );

        // ===== UI actions =====
        startBtn.addActionListener(e -> { live.startSimulation(); startBtn.setEnabled(false); });
        stopBtn.addActionListener(e -> live.stopSimulation());

        simSpeedSlider.addChangeListener(e -> {
            int factor = simSpeedSlider.getValue();
            int delayMs = 110 - 10 * factor;
            live.setSpeedDelay(delayMs, factor);
            Logging.LOG.info("Sim speed set: " + factor + "x (delay=" + delayMs + "ms)");
        });

        exportBtn.addActionListener(e -> {
            Object sel = routeCombo.getSelectedItem();
            String routeName = (sel instanceof VehicleInjection.RouteDef) ? ((VehicleInjection.RouteDef) sel).name : "";
            live.exportMetricsCsv(frame, routeName);
        });

        exportPdfBtn.addActionListener(e -> {
            Object sel = routeCombo.getSelectedItem();
            String routeName = (sel instanceof VehicleInjection.RouteDef) ? ((VehicleInjection.RouteDef) sel).name : "";
            BufferedImage chartImg = MapVisualisation.renderComponentToImage(trendChart, 900, 320);
            live.exportSummaryPdf(frame, routeName, chartImg);
        });

        java.util.function.Supplier<Integer> numVehiclesSupplier = () -> {
            try { return Math.max(1, Integer.parseInt(numVehiclesField.getText().trim())); }
            catch (Exception ex) { return 1; }
        };

        carBtn.addActionListener(e -> VehicleInjection.injectVehicles(frame, Main.TYPE_CAR,
                (VehicleInjection.RouteDef) routeCombo.getSelectedItem(), numVehiclesSupplier.get()));
        truckBtn.addActionListener(e -> VehicleInjection.injectVehicles(frame, Main.TYPE_TRUCK,
                (VehicleInjection.RouteDef) routeCombo.getSelectedItem(), numVehiclesSupplier.get()));
        busBtn.addActionListener(e -> VehicleInjection.injectVehicles(frame, Main.TYPE_BUS,
                (VehicleInjection.RouteDef) routeCombo.getSelectedItem(), numVehiclesSupplier.get()));

        tlCombo.addActionListener(e -> {
            TrafficControl.TlsItem item = (TrafficControl.TlsItem) tlCombo.getSelectedItem();
            trafficControl.setSelectedTls(item == null ? null : item.id);
        });

        tlRedBtn.addActionListener(e -> {
            TrafficControl.TlsItem item = (TrafficControl.TlsItem) tlCombo.getSelectedItem();
            String tlsId = (item == null) ? null : item.id;
            if (tlsId != null) trafficControl.forceTrafficLightRed(tlsId);
        });
        tlGreenBtn.addActionListener(e -> {
            TrafficControl.TlsItem item = (TrafficControl.TlsItem) tlCombo.getSelectedItem();
            String tlsId = (item == null) ? null : item.id;
            if (tlsId != null) trafficControl.forceTrafficLightGreen(tlsId);
        });
        tlResetBtn.addActionListener(e -> trafficControl.resetAllForcedTrafficLights());

        ruleBasedTlsCb.addActionListener(e -> trafficControl.setRuleBasedTlsEnabled(ruleBasedTlsCb.isSelected()));

        // Filters
        showCars.addActionListener(e -> FILTER.showCars = showCars.isSelected());
        showTrucks.addActionListener(e -> FILTER.showTrucks = showTrucks.isSelected());
        showBuses.addActionListener(e -> FILTER.showBuses = showBuses.isSelected());

        minSpeedSlider.addChangeListener(e -> {
            int v = minSpeedSlider.getValue();
            FILTER.minSpeedMps = v;
            double kmh = v * 3.6;
            minSpeedValue.setText(String.format(Locale.US, ">= %.1f m/s (%.0f km/h)", (double)v, kmh));
        });

        Logging.LOG.info("UI ready. Scenarios + long variants will populate after SUMO starts.");
    }

    // ===================== UI HELPERS =====================
    private static void stylePrimaryButton(AbstractButton b, Color bg) {
        b.setBackground(bg);
        b.setForeground(Color.BLACK);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        b.setFont(new Font("SansSerif", Font.BOLD, 11));
        Dimension d = new Dimension(110, 26);
        b.setPreferredSize(d);
        b.setMinimumSize(d);
        b.setMaximumSize(d);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private static void styleSectionLabel(JLabel l, Font f) {
        l.setForeground(Color.WHITE);
        l.setFont(f);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private static void styleTitledBorder(JPanel panel, String title) {
        TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COL),
                title, TitledBorder.LEADING, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12), TEXT_MUTED
        );
        panel.setBorder(BorderFactory.createCompoundBorder(
                tb, BorderFactory.createEmptyBorder(8, 12, 10, 12)));
    }

    private static JCheckBox styleCheckBox(JCheckBox cb) {
        cb.setForeground(Color.WHITE);
        cb.setBackground(BG_PANEL);
        cb.setFocusPainted(false);
        cb.setAlignmentX(Component.LEFT_ALIGNMENT);
        cb.setFont(new Font("SansSerif", Font.BOLD, 12));
        return cb;
    }

    private static JPanel rowPanel(Component... comps) {
        JPanel p = new JPanel(new GridLayout(1, comps.length, 8, 0));
        p.setBackground(BG_PANEL);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Component c : comps) p.add(c);
        return p;
    }
}