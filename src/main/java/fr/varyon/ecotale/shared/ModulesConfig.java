package fr.varyon.ecotale.shared;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class ModulesConfig {

    public static final BuilderCodec<ModulesConfig> CODEC = BuilderCodec.builder(ModulesConfig.class, ModulesConfig::new)
        .append(new KeyedCodec<>("EnableCoins", Codec.BOOLEAN),
            (c, v, e) -> c.enableCoins = v, (c, e) -> c.enableCoins).add()
        .append(new KeyedCodec<>("EnableJobs", Codec.BOOLEAN),
            (c, v, e) -> c.enableJobs = v, (c, e) -> c.enableJobs).add()
        .build();

    private boolean enableCoins = true;
    private boolean enableJobs = true;

    public boolean isEnableCoins() { return enableCoins; }
    public boolean isEnableJobs() { return enableJobs; }
}
