package fr.varyon.ecotale;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.logging.Level;

public class VaryonEcotalePlugin extends JavaPlugin {

    private static VaryonEcotalePlugin instance;

    public VaryonEcotalePlugin(@NonNullDecl JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();
        instance = this;
        this.getLogger().at(Level.INFO).log("[Varyon-Ecotale] Plugin loaded (skeleton).");
    }

    @Override
    protected void shutdown() {
        this.getLogger().at(Level.INFO).log("[Varyon-Ecotale] Plugin shutting down.");
    }

    public static VaryonEcotalePlugin getInstance() {
        return instance;
    }
}
