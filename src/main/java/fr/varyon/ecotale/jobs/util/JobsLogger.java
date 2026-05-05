package fr.varyon.ecotale.jobs.util;

import fr.varyon.ecotale.VaryonEcotalePlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class JobsLogger {

    private static final Logger LOGGER = Logger.getLogger("EcotaleJobs");

    private JobsLogger() {}

    public static boolean isDebugEnabled() {
        try {
            VaryonEcotalePlugin p = VaryonEcotalePlugin.getInstance();
            return p != null && p.getJobsModule() != null && p.getJobsModule().getConfig().isDebugMode();
        } catch (Exception e) {
            return false;
        }
    }

    public static void debug(String message) {
        if (isDebugEnabled()) {
            LOGGER.log(Level.INFO, message);
        }
    }

    public static void debug(String format, Object... args) {
        if (isDebugEnabled()) {
            LOGGER.log(Level.INFO, String.format(format, args));
        }
    }

    public static void info(String message) {
        LOGGER.log(Level.INFO, message);
    }

    public static void info(String format, Object... args) {
        LOGGER.log(Level.INFO, String.format(format, args));
    }

    public static void warn(String message) {
        LOGGER.log(Level.WARNING, message);
    }

    public static void warn(String format, Object... args) {
        LOGGER.log(Level.WARNING, String.format(format, args));
    }

    public static void error(String message) {
        LOGGER.log(Level.SEVERE, message);
    }

    public static void error(String format, Object... args) {
        LOGGER.log(Level.SEVERE, String.format(format, args));
    }

    public static void error(String message, Throwable t) {
        LOGGER.log(Level.SEVERE, message, t);
    }
}


