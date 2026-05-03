package fr.varyon.ecotale.jobs.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.util.*;

/**
 * Mob-to-tier mappings config. Separate file for auto-merge without losing customizations.
 * Tier names: neutral, minor, moderate, major, elite, champion, boss (no {@code standard}).
 */
public class TierMappingsConfig {
    
    public static final int CURRENT_VERSION = 3;
    
    public static final BuilderCodec<TierMappingsConfig> CODEC = BuilderCodec.builder(TierMappingsConfig.class, TierMappingsConfig::new)
        .append(new KeyedCodec<>("Version", Codec.INTEGER),
            (c, v, e) -> c.version = v, (c, e) -> c.version).add()
        .append(new KeyedCodec<>("AutoMergeNewMobs", Codec.BOOLEAN),
            (c, v, e) -> c.autoMergeNewMobs = v, (c, e) -> c.autoMergeNewMobs).add()
        .append(new KeyedCodec<>("TierMappings", new MapCodec<>(Codec.STRING, HashMap::new)),
            (c, v, e) -> c.tierMappings = v, (c, e) -> c.tierMappings).add()
        .append(new KeyedCodec<>("Exclusions", Codec.STRING_ARRAY),
            (c, v, e) -> c.exclusions = new ArrayList<>(Arrays.asList(v)), 
            (c, e) -> c.exclusions.toArray(new String[0])).add()
        .append(new KeyedCodec<>("DefaultTier", Codec.STRING),
            (c, v, e) -> c.defaultTier = v, (c, e) -> c.defaultTier).add()
        .build();
    
    private int version = CURRENT_VERSION;
    private boolean autoMergeNewMobs = true;
    private Map<String, String> tierMappings = createDefaultMappings();
    private List<String> exclusions = createDefaultExclusions();
    private String defaultTier = "major";
    
    // Getters
    public int getVersion() { return version; }
    public boolean isAutoMergeNewMobs() { return autoMergeNewMobs; }
    public Map<String, String> getTierMappings() { return tierMappings; }
    public List<String> getExclusions() { return exclusions; }
    public String getDefaultTier() { return defaultTier; }
    
    /**
     * Safely add a mapping, handling potentially immutable maps from codec deserialization.
     * If the internal map is immutable, it will be replaced with a mutable copy.
     */
    public void addMapping(String mobName, String tier) {
        try {
            tierMappings.put(mobName, tier);
        } catch (UnsupportedOperationException e) {
            // Map was deserialized as immutable, replace with mutable copy
            tierMappings = new HashMap<>(tierMappings);
            tierMappings.put(mobName, tier);
        }
    }
    
    /**
     * Merge new default mappings into this config.
     * Only adds mappings that don't already exist.
     * @return Number of new mappings added
     */
    public int mergeDefaults() {
        if (!autoMergeNewMobs) {
            return 0;
        }
        
        Map<String, String> defaults = createDefaultMappings();
        int added = 0;
        
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            if (!tierMappings.containsKey(entry.getKey())) {
                tierMappings.put(entry.getKey(), entry.getValue());
                added++;
            }
        }
        
        // Also merge new exclusions
        List<String> defaultExclusions = createDefaultExclusions();
        for (String exclusion : defaultExclusions) {
            if (!exclusions.contains(exclusion)) {
                exclusions.add(exclusion);
            }
        }
        
        // Update version if we merged anything
        if (added > 0) {
            version = CURRENT_VERSION;
        }
        
        return added;
    }
    
    /**
     * Check if this config needs an update.
     */
    public boolean needsUpdate() {
        return version < CURRENT_VERSION;
    }
    
    // ==========================================================================
    // DEFAULT MAPPINGS - Auto-generated from game assets
    // Based on Danger Score = HP + (DMG * 4) * aggression_modifier
    // ==========================================================================
    
    private static Map<String, String> createDefaultMappings() {
        Map<String, String> m = new LinkedHashMap<>();
        
        // ============ WORLDBOSS (Danger 1000+) ============
        // Dragons are the ultimate bosses
        m.put("Dragon_*", "boss");
        m.put("*_Titan", "boss");
        
        // ============ MINIBOSS (Danger 700-1200) ============
        // These are the hardest non-dragon enemies
        m.put("Shadow_Knight", "champion");      // 400 HP, 119 DMG -> 1139
        m.put("Zombie_Aberrant", "champion");    // 400 HP, 119 DMG -> 1139  
        m.put("Zombie_Aberrant_Big", "champion"); // 341 HP, 86 DMG -> 890
        m.put("Rex_Cave", "champion");           // 400 HP, 68 DMG -> 874
        m.put("Werewolf", "champion");           // 283 HP, 66 DMG -> 711
        
        // ============ ELITE (Danger 300-700) ============
        // Strong enemies that require skill to defeat
        m.put("Emberwulf", "elite");             // 193 HP, 64 DMG -> 584
        m.put("Ghoul", "elite");                 // 193 HP, 48 DMG -> 500
        m.put("Crocodile", "elite");             // 145 HP, 48 DMG -> 438
        m.put("Tiger_Sabertooth", "elite");      // 124 HP, 46 DMG -> 400
        m.put("Whale_Humpback", "elite");        // 400 HP, peaceful giant
        m.put("Spawn_Void", "elite");            // 193 HP, 48 DMG -> 385
        m.put("Golem_Crystal_Sand", "elite");    // 193 HP, 47 DMG -> 381
        m.put("Wraith", "elite");                // 193 HP, 40 DMG -> 353
        m.put("Cow_Undead", "elite");            // 124 HP, 35 DMG -> 343
        m.put("Toad_Rhino*", "elite");           // 124 HP, 35 DMG -> 343
        m.put("Leopard_Snow", "elite");          // 103 HP, 36 DMG -> 321
        m.put("Goblin_Duke*", "elite");          // Boss phases
        m.put("Hound_Bleached", "elite");        // 126 HP, 30 DMG -> 320
        m.put("Zombie_Aberrant_Small", "elite"); // 126 HP, 30 DMG -> 320
        m.put("Yeti", "elite");                  // 226 HP, tough mythic
        m.put("Golem_*", "elite");               // All golems are elite
        m.put("*_Void", "elite");                // Void creatures
        
        // ============ HOSTILE (Danger 100-300) ============
        // Standard combat enemies
        m.put("Raptor_Cave", "major");         // 103 HP, 27 DMG -> 211
        m.put("Trork_Chieftain", "champion");     // 124 HP, 35 DMG -> 264
        m.put("Outlander_Brute", "major");     // 124 HP, 35 DMG
        m.put("Outlander_Berserker", "major"); // 103 HP, 27 DMG
        m.put("Outlander_*", "major");         // All outlanders
        m.put("Trork_*", "major");             // All trorks  
        m.put("Zombie*", "major");             // 49-126 HP, 18-30 DMG
        m.put("Scarak_Broodmother*", "major"); // 145 HP, no damage
        m.put("Scarak_Defender*", "major");    // 103 HP
        m.put("Skeleton_Burnt_*", "major");    
        m.put("Skeleton_Incandescent_*", "major");
        m.put("Skeleton_Pirate_*", "major");
        m.put("Molerat", "moderate");             // 61 HP, 23 DMG
        m.put("Fen_Stalker", "moderate");         // 74 HP, 29 DMG
        m.put("Bear_*", "major");              // 103-124 HP
        m.put("Wolf_Black", "major");
        m.put("Wolf_White", "major");
        m.put("Hyena", "major");
        m.put("Shark_*", "major");
        m.put("Snake_Cobra", "major");
        m.put("Scorpion", "major");
        m.put("Spider*", "major");
        m.put("Bison", "major");               // 126 HP
        m.put("Camel", "major");               // 126 HP
        m.put("Horse", "major");               // 124 HP, 12 DMG
        m.put("Ram", "major");                 // 124 HP
        m.put("Cow", "moderate");                 // 103 HP, 9 DMG
        m.put("Boar", "moderate");                // 81 HP
        m.put("Warthog", "moderate");
        m.put("Antelope", "moderate");
        m.put("Moose_*", "major");
        m.put("Mosshorn*", "major");
        m.put("Deer_Stag", "major");
        m.put("Kweebec_Razorleaf*", "major");  // 105 HP
        m.put("Hedera", "major");              // 226 HP
        m.put("Trillodon", "major");           // 145 HP
        m.put("Snapdragon", "major");          // 103 HP
        m.put("Spirit_Thunder", "major");      // 249 HP
        m.put("Spirit_Ember", "major");        // 126 HP
        m.put("Lizard_Sand", "moderate");
        m.put("Tortoise", "moderate");
        m.put("Armadillo", "moderate");
        m.put("Slug_Magma", "moderate");
        
        // ============ PASSIVE (Danger 50-100) ============
        // Non-aggressive or weak enemies
        m.put("Skeleton", "minor");            // 92 HP
        m.put("Skeleton_Archer", "minor");
        m.put("Skeleton_*", "minor");          
        m.put("Scarak_Fighter*", "minor");     // 81 HP
        m.put("Scarak_Seeker*", "minor");      // 61 HP
        m.put("Dungeon_Scarak_*", "minor");    
        m.put("Feran_*", "minor");             
        m.put("Kweebec_Sapling*", "minor");
        m.put("Kweebec_Rootling", "minor");
        m.put("Goblin_Scavenger*", "minor");   // 54 HP
        m.put("Goblin_Ogre", "minor");         // 124 HP but slow
        m.put("Klops_*", "minor");             
        m.put("Sheep", "minor");
        m.put("Mouflon", "minor");
        m.put("Goat", "minor");
        m.put("Deer_Doe", "minor");
        m.put("Pig_Wild", "minor");
        m.put("Cow_Calf", "minor");
        m.put("Camel_Calf", "minor");
        m.put("Bison_Calf", "minor");
        m.put("Ram_Lamb", "minor");
        m.put("Warthog_Piglet", "minor");
        m.put("Spirit_Frost", "minor");
        m.put("Spirit_Root", "minor");
        m.put("Cactee", "minor");
        m.put("Spark_Living", "minor");
        m.put("Snail_*", "minor");
        m.put("Snake_*", "minor");
        m.put("Eel_*", "minor");
        m.put("Trilobite*", "minor");
        m.put("Lobster", "minor");
        m.put("Jellyfish_Man_Of_War", "minor");
        m.put("Frostgill", "minor");
        m.put("Snapjaw", "minor");
        m.put("Archaeopteryx", "minor");
        m.put("Vulture", "minor");
        m.put("Pterodactyl", "minor");
        m.put("Wraith_Lantern", "minor");
        m.put("Crawler_Void", "minor");
        m.put("Eye_Void", "minor");
        m.put("Larva_Silk", "minor");
        
        // ============ CRITTER (Danger 0-50) ============
        // Tiny creatures, babies, passive wildlife
        m.put("*_Chick", "neutral");
        m.put("*_Cub", "neutral");
        m.put("*_Baby", "neutral");
        m.put("*_Piglet", "neutral");
        m.put("*_Lamb", "neutral");
        m.put("*_Foal", "neutral");
        m.put("*_Kid", "neutral");
        m.put("*_Seedling", "neutral");
        m.put("*_Sproutling", "neutral");
        m.put("Chicken", "neutral");
        m.put("Chicken_Desert", "neutral");
        m.put("Pig", "neutral");
        m.put("Bunny", "neutral");
        m.put("Mouse", "neutral");
        m.put("Squirrel", "neutral");
        m.put("Meerkat", "neutral");
        m.put("Gecko", "neutral");
        m.put("Rat", "neutral");
        m.put("Fox", "neutral");
        m.put("Rabbit", "neutral");
        m.put("Hatworm", "neutral");
        m.put("Frog_*", "neutral");
        m.put("Bat*", "neutral");
        m.put("Skrill*", "neutral");
        m.put("Turkey*", "neutral");
        m.put("Penguin", "neutral");
        m.put("Parrot", "neutral");
        m.put("Owl_*", "neutral");
        m.put("Crow", "neutral");
        m.put("Raven", "neutral");
        m.put("Bluebird", "neutral");
        m.put("Finch_*", "neutral");
        m.put("Sparrow", "neutral");
        m.put("Woodpecker", "neutral");
        m.put("Pigeon", "neutral");
        m.put("Duck", "neutral");
        m.put("Flamingo", "neutral");
        m.put("Hawk", "neutral");
        m.put("Tetrabird", "neutral");
        m.put("Crab", "neutral");
        m.put("Jellyfish_*", "neutral");
        m.put("Pufferfish", "neutral");
        m.put("Clownfish", "neutral");
        m.put("Minnow", "neutral");
        m.put("Tang_*", "neutral");
        m.put("Pike", "neutral");
        m.put("Piranha*", "neutral");
        m.put("Salmon", "neutral");
        m.put("Bluegill", "neutral");
        m.put("Catfish", "neutral");
        m.put("Trout_*", "neutral");
        m.put("Shellfish_*", "neutral");
        m.put("Scarak_Louse", "neutral");
        m.put("Larva_Void", "neutral");
        m.put("Goblin_Hermit", "neutral");
        m.put("Goblin_Miner*", "neutral");
        m.put("Goblin_Scrapper*", "neutral");
        m.put("Goblin_Thief*", "neutral");
        m.put("Goblin_Lobber*", "neutral");
        m.put("Temple_*", "neutral");
        m.put("Snake_Marsh", "neutral");
        
        return m;
    }
    
    private static List<String> createDefaultExclusions() {
        List<String> ex = new ArrayList<>();
        // Quest NPCs and friendly characters
        ex.add("Quest_Master");
        ex.add("Tuluk_Fisherman");
        ex.add("Klops_Gentleman");
        ex.add("Klops_Miner");
        ex.add("Kweebec_Prisoner");
        ex.add("Kweebec_Elder");
        
        // Test entities (development)
        ex.add("Test_*");
        ex.add("Edible_*");
        
        // Wolf companions (tamed)
        ex.add("Wolf_Trork_*");
        ex.add("Wolf_Outlander_*");
        
        return ex;
    }
}
