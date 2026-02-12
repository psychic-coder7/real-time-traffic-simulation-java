// ===================== Logging.java =====================
package org.example;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;

public final class Logging {

    public static final Logger LOG = Logger.getLogger("TrafficSim");

    static {
        setupLogging();
    }

    private Logging() {}

    private static void setupLogging() {
        try {
            System.setErr(System.out);
            System.setProperty("java.util.logging.SimpleFormatter.format",
                    "%1$tF %1$tT | %4$-7s | %2$s | %3$s | %5$s%6$s%n");

            LOG.setUseParentHandlers(false);
            LOG.setLevel(Level.ALL);

            ConsoleHandler ch = new ConsoleHandler();
            ch.setLevel(Level.INFO);
            ch.setFormatter(new SimpleFormatter());
            LOG.addHandler(ch);

            FileHandler fh = new FileHandler("traffic_sim.log", 1_000_000, 3, true);
            fh.setLevel(Level.ALL);
            fh.setFormatter(new SimpleFormatter());
            LOG.addHandler(fh);

            LOG.info("Logger ready (console + traffic_sim.log).");
        } catch (Exception e) {
            System.err.println("Logging init failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String nowTag() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}