package fr.varyon.ecotale.economy.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Configuration for individual coin appearance.
 * Allows admins to customize each coin type independently.
 * 
 * <p>Example configuration in ecotale.json:</p>
 * <pre>
 * "CoinConfigs": {
 *   "GOLD": {
 *     "UseNativeTint": true,
 *     "Texture": "Coin_Base.png",
 *     "TintColor": "#FFD700",
 *     "Model": "Coin.blockymodel",
 *     "HeldModel": "Coin_Held.blockymodel",
 *     "ParticleSystem": "Drop/Epic/Drop_Epic",
 *     "ParticleColor": "#FFD700",
 *     "ModelScale": 0.3
 *   }
 * }
 * </pre>
 * 
 * @author Ecotale Team
 * @since 1.1.0
 */
public class CoinAppearanceConfig {
    
    /**
     * Codec for serializing/deserializing CoinAppearanceConfig.
     */
    public static final BuilderCodec<CoinAppearanceConfig> CODEC = BuilderCodec.builder(CoinAppearanceConfig.class, CoinAppearanceConfig::new)
        .append(new KeyedCodec<>("UseNativeTint", Codec.BOOLEAN),
            (c, v, e) -> c.useNativeTint = v, (c, e) -> c.useNativeTint).add()
        .append(new KeyedCodec<>("Texture", Codec.STRING),
            (c, v, e) -> c.texture = v, (c, e) -> c.texture).add()
        .append(new KeyedCodec<>("TintColor", Codec.STRING),
            (c, v, e) -> c.tintColor = v, (c, e) -> c.tintColor).add()
        .append(new KeyedCodec<>("Model", Codec.STRING),
            (c, v, e) -> c.model = v, (c, e) -> c.model).add()
        .append(new KeyedCodec<>("HeldModel", Codec.STRING),
            (c, v, e) -> c.heldModel = v, (c, e) -> c.heldModel).add()
        .append(new KeyedCodec<>("ParticleSystem", Codec.STRING),
            (c, v, e) -> c.particleSystem = v, (c, e) -> c.particleSystem).add()
        .append(new KeyedCodec<>("ParticleColor", Codec.STRING),
            (c, v, e) -> c.particleColor = v, (c, e) -> c.particleColor).add()
        .append(new KeyedCodec<>("ModelScale", Codec.DOUBLE),
            (c, v, e) -> c.modelScale = v, (c, e) -> c.modelScale).add()
        .build();
    
    // ========== Fields ==========
    
    /** Whether to use the native Hytale Tint system (true) or custom texture (false) */
    private boolean useNativeTint = true;
    
    /** Texture file name (relative to coins folder). Used as base for tinting or as-is if tint disabled */
    private String texture = "Coin_Base.png";
    
    /** Tint color in hex format (e.g., "#FFD700"). Only used if useNativeTint is true */
    private String tintColor = "#FFFFFF";
    
    /** 3D model file name for dropped coins (relative to coins folder) */
    private String model = "Coin.blockymodel";
    
    /** 3D model file name for held coins (relative to coins folder) */
    private String heldModel = "Coin_Held.blockymodel";
    
    /** Particle system ID for drop effects (e.g., "Drop/Legendary/Drop_Legendary") */
    private String particleSystem = "Drop/Item/Item";
    
    /** Particle color in hex format (e.g., "#FFD700"). Usually matches tintColor */
    private String particleColor = "#FFFFFF";
    
    /** Scale of the 3D model when dropped */
    private double modelScale = 0.3;
    
    // ========== Constructors ==========
    
    public CoinAppearanceConfig() {
        // Default constructor for codec
    }
    
    /**
     * Create a coin appearance config with all parameters.
     */
    public CoinAppearanceConfig(boolean useNativeTint, String texture, String tintColor,
                                 String model, String heldModel, String particleSystem,
                                 String particleColor, double modelScale) {
        this.useNativeTint = useNativeTint;
        this.texture = texture;
        this.tintColor = tintColor;
        this.model = model;
        this.heldModel = heldModel;
        this.particleSystem = particleSystem;
        this.particleColor = particleColor;
        this.modelScale = modelScale;
    }
    
    // ========== Factory Methods ==========
    
    /**
     * Create a default config for a coin type with the specified color.
     * Uses native tint system with the base texture.
     */
    public static CoinAppearanceConfig withColor(String tintColor, String particleSystem) {
        CoinAppearanceConfig config = new CoinAppearanceConfig();
        config.tintColor = tintColor;
        config.particleColor = tintColor;
        config.particleSystem = particleSystem;
        return config;
    }
    
    // ========== Getters ==========
    
    public boolean isUseNativeTint() { return useNativeTint; }
    public String getTexture() { return texture; }
    public String getTintColor() { return tintColor; }
    public String getModel() { return model; }
    public String getHeldModel() { return heldModel; }
    public String getParticleSystem() { return particleSystem; }
    public String getParticleColor() { return particleColor; }
    public double getModelScale() { return modelScale; }
    
    // ========== Setters ==========
    
    public void setUseNativeTint(boolean useNativeTint) { this.useNativeTint = useNativeTint; }
    public void setTexture(String texture) { this.texture = texture; }
    public void setTintColor(String tintColor) { this.tintColor = tintColor; }
    public void setModel(String model) { this.model = model; }
    public void setHeldModel(String heldModel) { this.heldModel = heldModel; }
    public void setParticleSystem(String particleSystem) { this.particleSystem = particleSystem; }
    public void setParticleColor(String particleColor) { this.particleColor = particleColor; }
    public void setModelScale(double modelScale) { this.modelScale = modelScale; }
    
    @Override
    public String toString() {
        return "CoinAppearanceConfig{" +
                "useNativeTint=" + useNativeTint +
                ", texture='" + texture + '\'' +
                ", tintColor='" + tintColor + '\'' +
                ", model='" + model + '\'' +
                ", particleSystem='" + particleSystem + '\'' +
                ", modelScale=" + modelScale +
                '}';
    }
}
