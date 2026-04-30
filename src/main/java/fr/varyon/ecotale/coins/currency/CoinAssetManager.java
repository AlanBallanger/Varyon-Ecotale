package fr.varyon.ecotale.coins.currency;

import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;

/**
 * Manages coin textures and models as a standalone asset pack.
 * Packaged from {@code src/main/resources/Common} and {@code Server} into the mod JAR;
 * at runtime extracted under the server asset pack folder (e.g. {@code mods/Varyon_Varyon-Ecotale/}).
 */
public class CoinAssetManager {
    
    private static final String COMMON_COINS_PATH = "Common/Items/Currency/Coins";
    private static final String ICONS_PATH = "Common/Icons/Items/Coins";
    private static final String UI_PATH = "Common/UI/Custom/Pages";
    private static final String SERVER_ITEMS_PATH = "Server/Item/Items";
    private static final String LANGUAGES_PATH = "Server/Languages";
    private static final String MODEL_DROPPED = "Coin.blockymodel";
    private static final String MODEL_HELD = "Coin_Held.blockymodel";
    // Classpath roots under the JAR (resources/Common, resources/Server)
    private static final String JAR_RESOURCE_BASE = "/Common/Items/Currency/Coins/";
    private static final String JAR_ICONS_BASE = "/Common/Icons/Items/Coins/";
    private static final String JAR_UI_BASE = "/Common/UI/Custom/Pages/";
    private static final String JAR_ITEMS_BASE = "/Server/Item/Items/";
    
    // Language file locales to extract
    private static final String[] LOCALES = {
        "en-US", "es-ES", "pt-BR", "de-DE", "fr-FR", "ru-RU", "ja-JP", "zh-CN", "tr-TR"
    };
    
    private final Path assetPackRoot;
    private final Path coinsFolder;
    private final Path iconsFolder;
    private final Path uiFolder;
    private final Path itemsFolder;
    private final HytaleLogger logger;
    
    private boolean initialized = false;
    private boolean firstTimeSetup = false;
    
    public CoinAssetManager(Path assetPackRoot, HytaleLogger logger) {
        this.assetPackRoot = assetPackRoot;
        this.coinsFolder = assetPackRoot.resolve(COMMON_COINS_PATH);
        this.iconsFolder = assetPackRoot.resolve(ICONS_PATH);
        this.uiFolder = assetPackRoot.resolve(UI_PATH);
        this.itemsFolder = assetPackRoot.resolve(SERVER_ITEMS_PATH);
        this.logger = logger;
    }
    
    public boolean initialize() {
        if (initialized) {
            return true;
        }
        
        try {
            Path copperSample = coinsFolder.resolve("Coin_Copper.png");
            boolean hadAssets = Files.isRegularFile(copperSample);

            if (!Files.exists(coinsFolder)) {
                Files.createDirectories(coinsFolder);
                logger.at(Level.INFO).log("[EcotaleCoins] Created asset pack structure: %s", coinsFolder);
            }
            
            if (!Files.exists(itemsFolder)) {
                Files.createDirectories(itemsFolder);
                logger.at(Level.INFO).log("[EcotaleCoins] Created items folder: %s", itemsFolder);
            }
            
            if (!Files.exists(iconsFolder)) {
                Files.createDirectories(iconsFolder);
                logger.at(Level.INFO).log("[EcotaleCoins] Created icons folder: %s", iconsFolder);
            }
            
            // Extract textures and models
            for (CoinType type : CoinType.values()) {
                String textureName = "Coin_" + capitalizeFirst(type.name().toLowerCase()) + ".png";
                extractResourceIfMissing(textureName);
            }
            
            extractResourceIfMissing(MODEL_DROPPED);
            extractResourceIfMissing(MODEL_HELD);
            
            // Extract icons for inventory display
            for (CoinType type : CoinType.values()) {
                String iconName = "Coin_" + capitalizeFirst(type.name().toLowerCase()) + ".png";
                extractIconIfMissing(iconName);
            }
            
            // Extract item definitions (critical for items to work!)
            for (CoinType type : CoinType.values()) {
                String itemName = "Coin_" + capitalizeFirst(type.name().toLowerCase()) + ".json";
                extractItemDefinitionIfMissing(itemName);
            }
            
            // Extract language files (so admins can customize coin names and descriptions)
            for (String locale : LOCALES) {
                extractLanguageFileIfMissing(locale);
            }
            
            this.firstTimeSetup = !hadAssets;
            
            initialized = true;
            logger.at(Level.INFO).log("[EcotaleCoins] Asset pack initialized at: %s", assetPackRoot);
            
            return true;
            
        } catch (IOException e) {
            logger.at(Level.SEVERE).withCause(e).log("[EcotaleCoins] Failed to initialize asset pack");
            return false;
        }
    }
    
    private static String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
    
    private void extractResourceIfMissing(String resourceName) throws IOException {
        Path targetPath = coinsFolder.resolve(resourceName);
        
        if (Files.exists(targetPath)) {
            return;
        }
        
        String resourcePath = JAR_RESOURCE_BASE + resourceName;
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                logger.at(Level.WARNING).log("[EcotaleCoins] Resource not found in JAR: %s", resourcePath);
                return;
            }
            
            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            logger.at(Level.INFO).log("[EcotaleCoins] Extracted: %s", resourceName);
        }
    }
    
    private void extractItemDefinitionIfMissing(String itemFileName) throws IOException {
        Path targetPath = itemsFolder.resolve(itemFileName);
        
        if (Files.exists(targetPath)) {
            return;
        }
        
        String resourcePath = JAR_ITEMS_BASE + itemFileName;
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                logger.at(Level.WARNING).log("[EcotaleCoins] Item definition not found in JAR: %s", resourcePath);
                return;
            }
            
            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            logger.at(Level.INFO).log("[EcotaleCoins] Extracted item definition: %s", itemFileName);
        }
    }
    
    private void extractIconIfMissing(String iconFileName) throws IOException {
        Path targetPath = iconsFolder.resolve(iconFileName);
        
        if (Files.exists(targetPath)) {
            return;
        }
        
        String resourcePath = JAR_ICONS_BASE + iconFileName;
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                logger.at(Level.WARNING).log("[EcotaleCoins] Icon not found in JAR: %s", resourcePath);
                return;
            }
            
            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            logger.at(Level.INFO).log("[EcotaleCoins] Extracted icon: %s", iconFileName);
        }
    }
    
    private void extractUiIfMissing(String uiFileName) throws IOException {
        Path targetPath = uiFolder.resolve(uiFileName);
        
        if (Files.exists(targetPath)) {
            return;
        }
        
        String resourcePath = JAR_UI_BASE + uiFileName;
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                logger.at(Level.WARNING).log("[EcotaleCoins] UI file not found in JAR: %s", resourcePath);
                return;
            }
            
            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            logger.at(Level.INFO).log("[EcotaleCoins] Extracted UI: %s", uiFileName);
        }
    }
    
    private void extractLanguageFileIfMissing(String locale) throws IOException {
        // Create locale directory if needed
        Path localeDir = assetPackRoot.resolve(LANGUAGES_PATH).resolve(locale);
        if (!Files.exists(localeDir)) {
            Files.createDirectories(localeDir);
        }
        
        Path targetPath = localeDir.resolve("ecotalecoins.lang");
        
        if (Files.exists(targetPath)) {
            return; // Don't overwrite existing customizations
        }
        
        String resourcePath = "/Server/Languages/" + locale + "/ecotalecoins.lang";
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                logger.at(Level.FINE).log("[EcotaleCoins] Language file not found in JAR: %s", resourcePath);
                return;
            }
            
            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            logger.at(Level.INFO).log("[EcotaleCoins] Extracted language file: %s/ecotalecoins.lang", locale);
        }
    }
    
    
    public Path getAssetPackRoot() {
        return assetPackRoot;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public boolean isFirstTimeSetup() {
        return firstTimeSetup;
    }
}
