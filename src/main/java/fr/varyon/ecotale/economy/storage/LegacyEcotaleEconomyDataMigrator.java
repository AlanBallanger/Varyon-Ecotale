package fr.varyon.ecotale.economy.storage;

import fr.varyon.ecotale.VaryonEcotalePlugin;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.stream.Stream;

/**
 * One-time copy from legacy {@code mods/Ecotale_Ecotale/} into the plugin data directory
 * (e.g. {@code mods/Varyon_Varyon-Ecotale/}) when the destination has no data yet.
 */
public final class LegacyEcotaleEconomyDataMigrator {

    public static final Path LEGACY_MODS_ECOTALE = Path.of("mods", "Ecotale_Ecotale");

    private LegacyEcotaleEconomyDataMigrator() {}

    public static void migrateIfNeeded(HytaleLogger log) {
        Path destRoot = VaryonEcotalePlugin.getInstance().getDataDirectory();
        try {
            Files.createDirectories(destRoot);
        } catch (IOException e) {
            log.at(Level.WARNING).log("Could not create plugin data directory: %s", e.getMessage());
            return;
        }
        if (!Files.isDirectory(LEGACY_MODS_ECOTALE)) {
            return;
        }
        try {
            migrateJsonPlayers(destRoot, log);
            migrateJsonLegacyBalancesFile(destRoot, log);
            migrateH2Files(destRoot, log);
        } catch (IOException e) {
            log.at(Level.WARNING).withCause(e).log("Legacy Ecotale_Ecotale migration failed (non-fatal)");
        }
    }

    private static void migrateJsonPlayers(Path destRoot, HytaleLogger log) throws IOException {
        Path srcPlayers = LEGACY_MODS_ECOTALE.resolve("players");
        Path destPlayers = destRoot.resolve("players");
        if (!Files.isDirectory(srcPlayers) || !isDirEmptyOrMissing(destPlayers)) {
            return;
        }
        Files.createDirectories(destPlayers);
        int n = 0;
        try (Stream<Path> stream = Files.list(srcPlayers)) {
            for (Path p : stream.toList()) {
                if (!Files.isRegularFile(p)) {
                    continue;
                }
                Path target = destPlayers.resolve(p.getFileName());
                if (!Files.exists(target)) {
                    Files.copy(p, target, StandardCopyOption.COPY_ATTRIBUTES);
                    n++;
                }
            }
        }
        if (n > 0) {
            log.at(Level.INFO).log("[Varyon-Ecotale] Migrated %d JSON player file(s) from %s to plugin data directory", n, LEGACY_MODS_ECOTALE);
        }
    }

    private static void migrateJsonLegacyBalancesFile(Path destRoot, HytaleLogger log) throws IOException {
        Path src = LEGACY_MODS_ECOTALE.resolve("balances.json");
        Path dest = destRoot.resolve("balances.json");
        if (Files.isRegularFile(src) && !Files.exists(dest)) {
            Files.copy(src, dest, StandardCopyOption.COPY_ATTRIBUTES);
            log.at(Level.INFO).log("[Varyon-Ecotale] Migrated balances.json from %s to plugin data directory", LEGACY_MODS_ECOTALE);
        }
    }

    private static void migrateH2Files(Path destRoot, HytaleLogger log) throws IOException {
        String base = "ecotale";
        for (String suffix : new String[] { ".mv.db", ".trace.db" }) {
            Path src = LEGACY_MODS_ECOTALE.resolve(base + suffix);
            Path dest = destRoot.resolve(base + suffix);
            if (Files.isRegularFile(src) && !Files.exists(dest)) {
                Files.copy(src, dest, StandardCopyOption.COPY_ATTRIBUTES);
                log.at(Level.INFO).log("[Varyon-Ecotale] Migrated H2 file %s from legacy folder", src.getFileName());
            }
        }
    }

    private static boolean isDirEmptyOrMissing(Path dir) {
        if (!Files.exists(dir)) {
            return true;
        }
        if (!Files.isDirectory(dir)) {
            return false;
        }
        try (Stream<Path> s = Files.list(dir)) {
            return s.findAny().isEmpty();
        } catch (IOException e) {
            return false;
        }
    }
}
