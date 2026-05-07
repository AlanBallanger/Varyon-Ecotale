package fr.varyon.ecotale.coins.currency;

import java.util.Locale;

public enum TokenType {
    COINCOIN("coincoin", "Token_CoinCoin", "CoinCoin"),
    BUILDING("building", "Token_Building", "Building"),
    FACTION("faction", "Token_Faction", "Faction");

    private final String configKey;
    private final String itemId;
    private final String displayName;

    TokenType(String configKey, String itemId, String displayName) {
        this.configKey = configKey;
        this.itemId = itemId;
        this.displayName = displayName;
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getItemId() {
        return itemId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TokenType fromItemId(String itemId) {
        if (itemId == null) return null;
        for (TokenType t : values()) {
            if (t.itemId.equals(itemId)) return t;
        }
        return null;
    }

    public static TokenType fromKey(String key) {
        if (key == null) return null;
        String norm = key.trim().toLowerCase(Locale.ROOT);
        if (norm.startsWith("token_")) {
            norm = norm.substring("token_".length());
        }
        for (TokenType t : values()) {
            if (t.configKey.equals(norm)) return t;
        }
        return null;
    }

    public static boolean isToken(String itemId) {
        return fromItemId(itemId) != null;
    }
}
