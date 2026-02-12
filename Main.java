// ===================== Main.java =====================
package org.example;

import javax.swing.*;
import java.io.File;

public class Main {

    // ===================== CONFIG =====================
    public static final String TYPE_CAR = "car";
    public static final String TYPE_TRUCK = "truck";
    public static final String TYPE_BUS = "bus";

    public static final String SUMOCFG_PATH = "final.sumocfg";

    // IMPORTANT: previously you forced dropping 2nd route. Turn it OFF.
    public static final boolean DROP_SECOND_ROUTE = false;

    public static final double THROUGHPUT_WINDOW_SEC = 300.0; // 5 minutes

    // ===================== VALIDATION =====================
    public static class Milestone3Exception extends Exception {
        public Milestone3Exception(String message) { super(message); }
        public Milestone3Exception(String message, Throwable cause) { super(message, cause); }
    }

    private static void validateProjectSetup() throws Milestone3Exception {
        File cfg = new File(SUMOCFG_PATH);
        if (!cfg.exists()) {
            throw new Milestone3Exception(
                    "Missing SUMO config file: '" + SUMOCFG_PATH + "'. " +
                            "Place it next to the program (working directory: " + new File(".").getAbsolutePath() + ")");
        }
        if (!cfg.isFile() || !cfg.canRead()) {
            throw new Milestone3Exception("SUMO config file is not readable: " + cfg.getAbsolutePath());
        }
    }

    // ===================== MAIN (tiny) =====================
    public static void main(String[] args) {
        try {
            validateProjectSetup();
        } catch (Milestone3Exception ex) {
            Logging.LOG.severe("Project setup error: " + ex.getMessage());
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Project setup error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Logging.LOG.info("App boot @ " + Logging.nowTag());

        // Init map + trips (file parsing part)
        VehicleInjection.loadTripRoutesFromRou();
        MapVisualisation.initBoundsFromFiles();

        GUI.launch();
    }
}