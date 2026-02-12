// ===================== TrafficControl.java =====================
package org.example;

import org.eclipse.sumo.libtraci.*;

import javax.swing.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TrafficControl {

    public static class TlsItem {
        public final String id;
        public final String tag;
        public TlsItem(String id, String tag) { this.id = id; this.tag = tag; }
        @Override public String toString() {
            if (tag == null || tag.isBlank() || tag.equals(id)) return id;
            return tag + "  (" + id + ")";
        }
    }

    private final JComboBox<TlsItem> tlComboRef;
    private final JLabel tlStateLabel;

    private volatile java.util.List<String> tlsIdsCached = new ArrayList<>();
    private volatile String selectedTlsId = null;

    // --- TLS manual force tracking ---
    public enum ManualTlsMode { NONE, FORCE_RED, FORCE_GREEN }
    private final ConcurrentHashMap<String, ManualTlsMode> manualTlsMode = new ConcurrentHashMap<>();
    private final Map<String, String> originalTlsPrograms = new ConcurrentHashMap<>();

    // Rule-based TLS
    private volatile boolean ruleBasedTlsEnabled = false;
    private static final double RULE_STOP_SEC = 6.0;
    private static final double RULE_GO_SEC   = 12.0;

    enum RulePhase { AUTO, HOLD_RED, HOLD_GREEN }
    static class RuleState {
        volatile RulePhase phase = RulePhase.AUTO;
        volatile double untilSimTime = -1.0;
    }
    private final ConcurrentHashMap<String, RuleState> ruleStates = new ConcurrentHashMap<>();
    private final Map<String, String> ruleOriginalPrograms = new ConcurrentHashMap<>();
    private final Set<String> ruleTouchedTls = ConcurrentHashMap.newKeySet();

    public TrafficControl(JComboBox<TlsItem> tlComboRef, JLabel tlStateLabel) {
        this.tlComboRef = tlComboRef;
        this.tlStateLabel = tlStateLabel;
    }

    public boolean isRuleBasedTlsEnabled() { return ruleBasedTlsEnabled; }

    public void setSelectedTls(String tlsId) { this.selectedTlsId = tlsId; }

    public void setRuleBasedTlsEnabled(boolean enabled) {
        this.ruleBasedTlsEnabled = enabled;
        if (!enabled) restoreRuleBasedToAuto();
        SwingUtilities.invokeLater(() -> tlStateLabel.setText(enabled ? "Rule-based TLS: ON" : "Rule-based TLS: OFF"));
    }

    // ===================== TLS dropdown populate =====================
    public void rebuildTrafficLightDropdown() {
        try {
            StringVector ids = TrafficLight.getIDList();
            final java.util.List<String> list = new ArrayList<>();
            for (int i = 0; i < ids.size(); i++) list.add(ids.get(i));

            tlsIdsCached = new ArrayList<>(list);

            SwingUtilities.invokeLater(() -> {
                tlComboRef.removeAllItems();
                Map<String, String> labels = MapVisualisation.getTlsLabels();
                for (String id : list) {
                    String tag = (labels != null) ? labels.getOrDefault(id, id) : id;
                    tlComboRef.addItem(new TlsItem(id, tag));
                }
                tlComboRef.setEnabled(!list.isEmpty());
                if (!list.isEmpty()) tlComboRef.setSelectedIndex(0);

                selectedTlsId = list.isEmpty() ? null : list.get(0);
                tlStateLabel.setText(list.isEmpty() ? "TL State: none" : "TL State: ready (" + list.size() + ")");
            });
        } catch (Exception ex) {
            Logging.LOG.log(java.util.logging.Level.WARNING, "Failed to read traffic light IDs", ex);
            SwingUtilities.invokeLater(() -> {
                tlComboRef.removeAllItems();
                tlComboRef.setEnabled(false);
                tlStateLabel.setText("TL State: error");
            });
        }
    }

    // ===================== Per-step apply =====================
    public void applyPerStep(double simTime) {
        applyRuleBasedTls(simTime);
        applyManualOverrideIfNeeded();
    }

    // ===================== Manual TLS forcing =====================
    private void rememberOriginalProgram(String tlsId) {
        if (tlsId == null) return;
        if (originalTlsPrograms.containsKey(tlsId)) return;
        try {
            String prog = TrafficLight.getProgram(tlsId);
            originalTlsPrograms.put(tlsId, (prog == null || prog.isBlank()) ? "0" : prog);
        } catch (Exception ex) {
            originalTlsPrograms.put(tlsId, "0");
        }
    }

    private void applyManualOverrideIfNeeded() {
        for (Map.Entry<String, ManualTlsMode> e : manualTlsMode.entrySet()) {
            String tlsId = e.getKey();
            ManualTlsMode mode = e.getValue();
            if (mode == null || mode == ManualTlsMode.NONE) continue;

            try {
                String current = TrafficLight.getRedYellowGreenState(tlsId);
                int n = current.length();
                StringBuilder sb = new StringBuilder(n);

                if (mode == ManualTlsMode.FORCE_RED) for (int i = 0; i < n; i++) sb.append('r');
                else if (mode == ManualTlsMode.FORCE_GREEN) for (int i = 0; i < n; i++) sb.append('G');

                TrafficLight.setRedYellowGreenState(tlsId, sb.toString());
            } catch (Exception ignored) {}
        }
    }

    public void forceTrafficLightRed(String tlsId) {
        try {
            rememberOriginalProgram(tlsId);
            manualTlsMode.put(tlsId, ManualTlsMode.FORCE_RED);

            String current = TrafficLight.getRedYellowGreenState(tlsId);
            int n = current.length();
            StringBuilder sb = new StringBuilder(n);
            for (int i = 0; i < n; i++) sb.append('r');
            TrafficLight.setRedYellowGreenState(tlsId, sb.toString());

            SwingUtilities.invokeLater(() -> tlStateLabel.setText("TL " + tlsId + " forced RED (persistent)"));
        } catch (Exception ex) {
            Logging.LOG.log(java.util.logging.Level.WARNING, "forceTrafficLightRed failed for " + tlsId, ex);
            SwingUtilities.invokeLater(() -> tlStateLabel.setText("TL " + tlsId + " error (red)"));
        }
    }

    public void forceTrafficLightGreen(String tlsId) {
        try {
            rememberOriginalProgram(tlsId);
            manualTlsMode.put(tlsId, ManualTlsMode.FORCE_GREEN);

            String current = TrafficLight.getRedYellowGreenState(tlsId);
            int n = current.length();
            StringBuilder sb = new StringBuilder(n);
            for (int i = 0; i < n; i++) sb.append('G');
            TrafficLight.setRedYellowGreenState(tlsId, sb.toString());

            SwingUtilities.invokeLater(() -> tlStateLabel.setText("TL " + tlsId + " forced GREEN (persistent)"));
        } catch (Exception ex) {
            Logging.LOG.log(java.util.logging.Level.WARNING, "forceTrafficLightGreen failed for " + tlsId, ex);
            SwingUtilities.invokeLater(() -> tlStateLabel.setText("TL " + tlsId + " error (green)"));
        }
    }

    public void resetAllForcedTrafficLights() {
        int ok = 0, fail = 0;

        java.util.List<String> ids = new java.util.ArrayList<>(manualTlsMode.keySet());
        for (String tlsId : ids) {
            try {
                String prog = originalTlsPrograms.get(tlsId);
                if (prog == null || prog.isBlank()) prog = "0";

                TrafficLight.setProgram(tlsId, prog);
                try { TrafficLight.setPhaseDuration(tlsId, 0.0); } catch (Exception ignore) {}

                manualTlsMode.put(tlsId, ManualTlsMode.NONE);
                ok++;
            } catch (Exception ex) {
                fail++;
                Logging.LOG.log(java.util.logging.Level.WARNING, "Reset failed for TLS " + tlsId, ex);
            }
        }

        manualTlsMode.entrySet().removeIf(e -> e.getValue() == ManualTlsMode.NONE);
        originalTlsPrograms.clear();

        final int okF = ok, failF = fail;
        SwingUtilities.invokeLater(() -> {
            if (okF > 0 && failF == 0) tlStateLabel.setText("TL Reset: all back to NORMAL (" + okF + ")");
            else if (okF > 0) tlStateLabel.setText("TL Reset: normal=" + okF + ", failed=" + failF);
            else tlStateLabel.setText("TL Reset: nothing to reset");
        });
    }

    // ===================== Rule-based TLS =====================
    private void rememberRuleOriginalProgram(String tlsId) {
        if (tlsId == null) return;
        if (ruleOriginalPrograms.containsKey(tlsId)) return;
        try {
            String prog = TrafficLight.getProgram(tlsId);
            ruleOriginalPrograms.put(tlsId, (prog == null || prog.isBlank()) ? "0" : prog);
        } catch (Exception ex) {
            ruleOriginalPrograms.put(tlsId, "0");
        }
    }

    private void restoreRuleBasedToAuto() {
        for (String tlsId : new ArrayList<>(ruleTouchedTls)) {
            try {
                String prog = ruleOriginalPrograms.get(tlsId);
                if (prog == null || prog.isBlank()) prog = "0";
                TrafficLight.setProgram(tlsId, prog);
                try { TrafficLight.setPhaseDuration(tlsId, 0.0); } catch (Exception ignore) {}
            } catch (Exception ignore) {}
        }
        ruleTouchedTls.clear();
        ruleOriginalPrograms.clear();
        ruleStates.clear();
    }

    private StringVector safeGetControlledLanes(String tlsId) {
        try {
            Method m = TrafficLight.class.getMethod("getControlledLanes", String.class);
            Object o = m.invoke(null, tlsId);
            if (o instanceof StringVector) return (StringVector) o;
        } catch (Exception ignore) {}
        return null;
    }

    private int haltingVehiclesNearTls(String tlsId) {
        StringVector lanes = safeGetControlledLanes(tlsId);
        if (lanes == null || lanes.size() == 0) return 0;

        int sum = 0;
        int any = 0;

        for (int i = 0; i < lanes.size(); i++) {
            String laneId = lanes.get(i);

            Integer h = tryInvokeInt(Lane.class, "getLastStepHaltingNumber",
                    new Class<?>[]{String.class}, new Object[]{laneId});
            if (h != null) { sum += h; any++; continue; }

            Integer v = tryInvokeInt(Lane.class, "getLastStepVehicleNumber",
                    new Class<?>[]{String.class}, new Object[]{laneId});
            if (v != null) { sum += v; any++; }
        }

        if (any == 0) return 0;
        return sum;
    }

    private static Integer tryInvokeInt(Class<?> clazz, String methodName, Class<?>[] sig, Object[] args) {
        try {
            Method m = clazz.getMethod(methodName, sig);
            Object o = m.invoke(null, args);
            if (o instanceof Number) return ((Number) o).intValue();
        } catch (Exception ignored) {}
        return null;
    }

    private void setTlsAll(String tlsId, char c) {
        try {
            String current = TrafficLight.getRedYellowGreenState(tlsId);
            if (current == null) return;
            int n = current.length();
            StringBuilder sb = new StringBuilder(n);
            for (int i = 0; i < n; i++) sb.append(c);
            TrafficLight.setRedYellowGreenState(tlsId, sb.toString());
        } catch (Exception ignore) {}
    }

    private void applyRuleBasedTls(double simTime) {
        if (!ruleBasedTlsEnabled) return;
        if (tlsIdsCached == null || tlsIdsCached.isEmpty()) return;

        for (String tlsId : tlsIdsCached) {
            if (tlsId == null || tlsId.isBlank()) continue;

            ManualTlsMode mm = manualTlsMode.getOrDefault(tlsId, ManualTlsMode.NONE);
            if (mm != ManualTlsMode.NONE) continue;

            int demand = haltingVehiclesNearTls(tlsId);
            RuleState rs = ruleStates.computeIfAbsent(tlsId, k -> new RuleState());

            if (demand <= 0) {
                if (rs.phase != RulePhase.AUTO) {
                    try {
                        String prog = ruleOriginalPrograms.get(tlsId);
                        if (prog == null || prog.isBlank()) prog = "0";
                        TrafficLight.setProgram(tlsId, prog);
                        try { TrafficLight.setPhaseDuration(tlsId, 0.0); } catch (Exception ignore) {}
                    } catch (Exception ignore) {}
                    rs.phase = RulePhase.AUTO;
                    rs.untilSimTime = -1.0;
                }
                continue;
            }

            rememberRuleOriginalProgram(tlsId);
            ruleTouchedTls.add(tlsId);

            if (rs.phase == RulePhase.AUTO) {
                rs.phase = RulePhase.HOLD_RED;
                rs.untilSimTime = simTime + RULE_STOP_SEC;
                setTlsAll(tlsId, 'r');
                continue;
            }

            if (rs.untilSimTime > 0 && simTime >= rs.untilSimTime) {
                if (rs.phase == RulePhase.HOLD_RED) {
                    rs.phase = RulePhase.HOLD_GREEN;
                    rs.untilSimTime = simTime + RULE_GO_SEC;
                    setTlsAll(tlsId, 'G');
                } else if (rs.phase == RulePhase.HOLD_GREEN) {
                    rs.phase = RulePhase.HOLD_RED;
                    rs.untilSimTime = simTime + RULE_STOP_SEC;
                    setTlsAll(tlsId, 'r');
                }
            }
        }
    }

    // ===================== Status text helper =====================
    public String buildTlsStatusString() {
        String tlsShow = selectedTlsId;

        // keep these simple (no multiline needed)
        if (tlsShow == null || tlsShow.isBlank()) {
            return ruleBasedTlsEnabled ? "TL: none | RULE=ON" : "TL: none";
        }

        try {
            String ry = TrafficLight.getRedYellowGreenState(tlsShow);
            ManualTlsMode mm = manualTlsMode.getOrDefault(tlsShow, ManualTlsMode.NONE);
            String mmTxt = (mm == ManualTlsMode.NONE) ? "AUTO" : (mm == ManualTlsMode.FORCE_RED ? "FORCED RED" : "FORCED GREEN");
            String rbTxt = ruleBasedTlsEnabled ? "RULE=ON" : "RULE=OFF";

            // ✅ MULTI-LINE LABEL: number line 1, signal state line 2
            // JLabel needs HTML for new lines
            return "<html>"
                    + "TL: " + tlsShow
                    + "<br>"
                    + ry + " | " + mmTxt + " | " + rbTxt
                    + "</html>";

        } catch (Exception ignore) {
            // also multiline on error (optional but consistent)
            return "<html>TL: " + tlsShow + "<br>(read error)</html>";
        }
    }
}