package fr.varyon.ecotale.jobs.util;

import fr.varyon.ecotale.VaryonEcotalePlugin;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.Locale;
import java.util.logging.Level;

public final class JobsLogger {

    private static final HytaleLogger LOG = HytaleLogger.get("EcotaleJobs");

    private JobsLogger() {}

    private static String fmt(String format, Object... args) {
        return args.length == 0 ? format : String.format(Locale.ROOT, format, args);
    }

    public static boolean isDebugEnabled() {
        try {
            VaryonEcotalePlugin plugin = VaryonEcotalePlugin.getInstance();
            if (plugin == null) {
                return false;
            }
            boolean fromMain = plugin.getEconomyConfig() != null && plugin.getEconomyConfig().isDebugMode();
            boolean fromEarnings =
                plugin.getJobsModule() != null
                    && plugin.getJobsModule().getConfig() != null
                    && plugin.getJobsModule().getConfig().isDebugMode();
            return fromMain || fromEarnings;
        } catch (Exception e) {
            return false;
        }
    }

    public static void debug(String message) {
        if (isDebugEnabled()) {
            LOG.at(Level.INFO).log(message);
        }
    }

    public static void debug(String format, Object... args) {
        if (isDebugEnabled()) {
            LOG.at(Level.INFO).log(fmt(format, args));
        }
    }

    public static void info(String message) {
        LOG.at(Level.INFO).log(message);
    }

    public static void info(String format, Object... args) {
        LOG.at(Level.INFO).log(fmt(format, args));
    }

    public static void warn(String message) {
        LOG.at(Level.WARNING).log(message);
    }

    public static void warn(String format, Object... args) {
        LOG.at(Level.WARNING).log(fmt(format, args));
    }

    public static void error(String message) {
        LOG.at(Level.SEVERE).log(message);
    }

    public static void error(String format, Object... args) {
        LOG.at(Level.SEVERE).log(fmt(format, args));
    }

    public static void error(String message, Throwable t) {
        LOG.at(Level.SEVERE).withCause(t).log(message);
    }

    public static void bannerInfo(String format, Object... args) {
        LOG.at(Level.INFO).log(fmt(format, args));
    }
}

