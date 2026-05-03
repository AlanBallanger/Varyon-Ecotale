package fr.varyon.ecotale.jobs.config;

import com.google.gson.Gson;
import com.hypixel.hytale.codec.EmptyExtraInfo;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.logger.HytaleLogger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;

public final class EarningsConfigLoader {

    private static final Gson GSON = new Gson();

    private EarningsConfigLoader() {}

    /**
     * If {@code earnings_config.yml} is missing, copies {@code /earnings_config.default.yml}
     * from the plugin jar (documented tier ranges and formulas).
     */
    public static void installDefaultYamlIfMissing(Path path, HytaleLogger logger, Class<?> resourceAnchor) {
        if (Files.isRegularFile(path)) {
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            try (InputStream in = resourceAnchor.getResourceAsStream("/earnings_config.default.yml")) {
                if (in == null) {
                    logger.at(Level.WARNING).log("[Varyon-Ecotale] earnings_config.default.yml not on classpath; using built-in defaults.");
                    return;
                }
                Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
                logger.at(Level.INFO).log("[Varyon-Ecotale] Created earnings_config.yml from packaged defaults (tier→copper table + payout formula).");
            }
        } catch (IOException e) {
            logger.at(Level.WARNING).withCause(e).log("[Varyon-Ecotale] Failed to write default earnings_config.yml");
        }
    }

    public static EcotaleJobsConfig load(Path path, HytaleLogger logger) {
        if (!Files.isRegularFile(path)) {
            logger.at(Level.INFO).log("[Varyon-Ecotale] Missing earnings_config.yml, using built-in job reward defaults.");
            return new EcotaleJobsConfig();
        }
        try {
            String yamlText = Files.readString(path, StandardCharsets.UTF_8);
            Yaml yaml = new Yaml();
            Object root = yaml.load(yamlText);
            if (root == null) {
                return new EcotaleJobsConfig();
            }
            String json = GSON.toJson(root);
            try (var sr = new StringReader(json);
                 var reader = new RawJsonReader(sr, RawJsonReader.READ_BUFFER.get())) {
                return EcotaleJobsConfig.CODEC.decodeJson(reader, EmptyExtraInfo.EMPTY);
            }
        } catch (IOException | RuntimeException e) {
            logger.at(Level.SEVERE).withCause(e).log("[Varyon-Ecotale] Failed to parse earnings_config.yml, using defaults.");
            return new EcotaleJobsConfig();
        }
    }
}
