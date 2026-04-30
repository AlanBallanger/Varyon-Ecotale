package fr.varyon.ecotale.jobs.config;

import com.google.gson.Gson;
import com.hypixel.hytale.codec.EmptyExtraInfo;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.logger.HytaleLogger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public final class EarningsConfigLoader {

    private static final Gson GSON = new Gson();

    private EarningsConfigLoader() {}

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
