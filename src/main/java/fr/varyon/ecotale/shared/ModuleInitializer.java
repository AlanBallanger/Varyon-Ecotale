package fr.varyon.ecotale.shared;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;

public interface ModuleInitializer {
    void setup(JavaPlugin plugin);
    void shutdown();
}
