package fr.varyon.ecotale.jobs.util;

import fr.varyon.ecotale.jobs.config.TierMappingsConfig;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;

import java.util.*;

/**
 * Auto-detects new NPCs from the server's loaded assets and calculates
 * appropriate tier assignments based on their stats.
 * 
 * This system provides:
 * 1. Automatic detection of new NPCs from Hytale updates
 * 2. Support for NPCs added by other mods
 * 3. Intelligent tier inference based on HP (damage requires live NPC)
 * 
 * The auto-detection runs at server startup and merges new NPCs into
 * the TierMappings config without overwriting admin customizations.
 */
public class NPCAutoDetector {
    
    // Tier thresholds based on HP alone (conservative estimates)
    // We use HP-only since damage requires spawning the NPC
    private static final int HP_NEUTRAL = 50;
    private static final int HP_MINOR = 100;
    private static final int HP_MODERATE = 200;
    private static final int HP_MAJOR = 300;
    private static final int HP_ELITE = 450;
    private static final int HP_CHAMPION = 800;
    
    /**
     * Scan all registered NPC roles and detect any that aren't in the config.
     * 
     * @param currentConfig The current tier mappings config
     * @return Map of new NPC names to their auto-assigned tiers
     */
    public static Map<String, String> detectNewNPCs(TierMappingsConfig currentConfig) {
        Map<String, String> newMappings = new LinkedHashMap<>();
        
        try {
            // Get all registered NPC role names from Hytale
            List<String> allRoles = NPCPlugin.get().getRoleTemplateNames(true);
            
            if (allRoles == null || allRoles.isEmpty()) {
                JobsLogger.warn("[NPCAutoDetector] No NPC roles found - server may not be fully loaded");
                return newMappings;
            }
            
            Map<String, String> existingMappings = currentConfig.getTierMappings();
            List<String> exclusions = currentConfig.getExclusions();
            
            for (String roleName : allRoles) {
                // Skip if already mapped (exact match)
                if (existingMappings.containsKey(roleName)) {
                    continue;
                }
                
                // Skip if matches an exclusion pattern
                if (matchesAnyPattern(roleName, exclusions)) {
                    continue;
                }
                
                // Skip if matches an existing wildcard pattern
                if (matchesExistingPattern(roleName, existingMappings)) {
                    continue;
                }
                
                // This is a NEW NPC - try to infer its tier
                String inferredTier = inferTierFromRole(roleName);
                if (inferredTier != null) {
                    newMappings.put(roleName, inferredTier);
                    JobsLogger.debug("[NPCAutoDetector] New NPC detected: %s -> %s", roleName, inferredTier);
                }
            }
            
            if (!newMappings.isEmpty()) {
                JobsLogger.info("[NPCAutoDetector] Detected %d new NPCs from server assets", newMappings.size());
            }
            
        } catch (Exception e) {
            JobsLogger.warn("[NPCAutoDetector] Failed to scan NPCs: %s", e.getMessage());
        }
        
        return newMappings;
    }
    
    /**
     * Infer a tier from the NPC role name using naming conventions.
     * This is a fallback when we can't get HP data.
     */
    private static String inferTierFromRole(String roleName) {
        String lower = roleName.toLowerCase();
        
        // WORLDBOSS patterns
        if (lower.contains("dragon") || lower.contains("titan") || lower.contains("worldboss")) {
            return "boss";
        }
        
        if (lower.contains("boss") && !lower.contains("miniboss")) {
            return "boss";
        }
        
        if (lower.contains("miniboss") || lower.contains("aberrant") || 
            lower.contains("shadow_knight") || lower.contains("werewolf")) {
            return "champion";
        }
        
        if (lower.contains("elite") || lower.contains("golem") || lower.contains("ghoul") ||
            lower.contains("void") || lower.contains("yeti") || lower.contains("emberwulf")) {
            return "elite";
        }
        
        if (lower.contains("_chick") || lower.contains("_cub") || lower.contains("_baby") ||
            lower.contains("_piglet") || lower.contains("_lamb") || lower.contains("_foal") ||
            lower.contains("bunny") || lower.contains("mouse") || lower.contains("rat") ||
            lower.contains("squirrel") || lower.contains("frog") || lower.contains("bat") ||
            lower.contains("bird") || lower.contains("fish") || lower.contains("crab") ||
            lower.contains("jellyfish") || lower.contains("chicken") || lower.contains("pig")) {
            return "neutral";
        }
        
        if (lower.contains("skeleton") || lower.contains("sheep") || lower.contains("deer") ||
            lower.contains("goat") || lower.contains("_calf") || lower.contains("feran") ||
            lower.contains("klops") || lower.contains("spirit") || lower.contains("scarak_fighter") ||
            lower.contains("scarak_seeker") || lower.contains("kweebec_sapling")) {
            return "minor";
        }
        
        return "major";
    }
    
    /**
     * Check if a name matches any wildcard pattern in the list.
     */
    private static boolean matchesAnyPattern(String name, List<String> patterns) {
        for (String pattern : patterns) {
            if (matchesWildcard(name, pattern)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if a name matches any existing mapping pattern.
     */
    private static boolean matchesExistingPattern(String name, Map<String, String> mappings) {
        for (String pattern : mappings.keySet()) {
            if (matchesWildcard(name, pattern)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Simple wildcard matching (* matches any characters).
     */
    private static boolean matchesWildcard(String name, String pattern) {
        if (!pattern.contains("*")) {
            return name.equals(pattern);
        }
        
        // Convert wildcard pattern to regex
        String regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*");
        
        try {
            return name.matches(regex);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get HP-based tier from a live NPCEntity (when available).
     * This provides more accurate tier assignment.
     */
    public static String getTierFromLiveNPC(NPCEntity npc) {
        try {
            Role role = npc.getRole();
            if (role == null) return null;
            
            int hp = role.getInitialMaxHealth();
            
            // HP-based tier assignment
            if (hp <= HP_NEUTRAL) return "neutral";
            if (hp <= HP_MINOR) return "minor";
            if (hp <= HP_MODERATE) return "moderate";
            if (hp <= HP_MAJOR) return "major";
            if (hp <= HP_ELITE) return "elite";
            if (hp <= HP_CHAMPION) return "champion";
            return "boss";
            
        } catch (Exception e) {
            return null;
        }
    }
}
