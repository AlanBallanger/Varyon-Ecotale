package fr.varyon.ecotale.jobs.systems;

import fr.varyon.ecotale.shared.EconomyBridge;
import fr.varyon.ecotale.shared.CoinsBridge;
import fr.varyon.ecotale.economy.util.RateLimiter;
import fr.varyon.ecotale.jobs.integration.VaryonMobScalingBridge;
import fr.varyon.ecotale.jobs.config.EcotaleJobsConfig.MobKillsConfig;
import fr.varyon.ecotale.jobs.config.EcotaleJobsConfig.SecurityConfig;
import fr.varyon.ecotale.jobs.config.TierConfig;
import fr.varyon.ecotale.jobs.config.TierMappingsConfig;
import fr.varyon.ecotale.VaryonEcotalePlugin;
import fr.varyon.ecotale.jobs.security.AntiFarmSystem;
import fr.varyon.ecotale.jobs.security.EconomyCap;
import fr.varyon.ecotale.jobs.util.TierMatcher;
import fr.varyon.ecotale.jobs.util.RewardNotifier;
import fr.varyon.ecotale.jobs.util.JobsLogger;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mob reward system - uses DeathComponent to detect kills.
 */
public class MobRewardSystem extends RefChangeSystem<EntityStore, DeathComponent> {

    private static final float VARYON_HP_CLAMP_MIN = 0.25f;
    private static final float VARYON_HP_CLAMP_MAX = 40.0f;

    // Configuration - set via init()
    private MobKillsConfig config;
    private TierMappingsConfig mappingsConfig;
    
    private final VaryonMobScalingBridge varyonMobScalingBridge = new VaryonMobScalingBridge();

    // Core subsystems
    private final TierMatcher tierMatcher = new TierMatcher();
    private final AntiFarmSystem antiFarm = new AntiFarmSystem();
    private final EconomyCap economyCap = new EconomyCap();
    private final RateLimiter rateLimiter;

    @Nullable
    private final MobLastAttackerSystem lastAttackerSystem;

    // Cached exclusions for O(1) lookup - populated on init()
    private volatile Set<String> exclusionSet = new HashSet<>();
    
    // Thread-safe statistics for monitoring
    private final AtomicLong totalRewardsGiven = new AtomicLong(0);
    private final AtomicLong totalValueInjected = new AtomicLong(0);
    private final AtomicLong rewardsBlocked = new AtomicLong(0);

    public MobRewardSystem(@Nullable MobLastAttackerSystem lastAttackerSystem) {
        // RateLimiter: 30 burst capacity, 5 tokens/sec refill
        // This allows 30 rapid kills, then ~5 kills/sec sustained
        this.rateLimiter = new RateLimiter(30, 5);
        this.lastAttackerSystem = lastAttackerSystem;
    }
    
    /**
     * Initialize the reward system with configuration.
     * Must be called after plugin load, before any events fire.
     * 
     * @param config The mob kills configuration (nullable - disables system if null)
     * @param mappings The tier mappings configuration
     */
    public void init(MobKillsConfig config, TierMappingsConfig mappings) {
        this.config = config;
        this.mappingsConfig = mappings;
        
        if (config == null || mappings == null) {
            JobsLogger.warn("[MobRewardSystem] Config is null - system DISABLED");
            return;
        }
        
        // Initialize tier matcher with pattern mappings from TierMappingsConfig
        tierMatcher.configure(
            mappings.getTierMappings(),
            new HashSet<>(mappings.getExclusions()),
            mappings.getDefaultTier()
        );
        
        // Cache exclusions for O(1) lookup (volatile for thread-safety)
        this.exclusionSet = new HashSet<>(mappings.getExclusions());
        
        // Configure anti-farm subsystem
        SecurityConfig security = config.getSecurity();
        antiFarm.configure(
            security.getAntiFarmThreshold(),
            security.getAntiFarmDecayPerKill(),
            0.1f,  // Minimum multiplier (10% of reward at worst)
            30,    // TTL in minutes for player tracking
            security.isAntiFarmEnabled()
        );
        
        // Configure global economy cap
        economyCap.configure(
            security.getMaxGlobalInjectionPerHour(),
            true   // Enabled
        );
        
        JobsLogger.info("[MobRewardSystem] Initialized: %d tiers, %d mappings, %d exclusions | AntiFarm=%s | VaryonHpRewardScale=%s",
            config.getTiers().size(),
            mappings.getTierMappings().size(),
            mappings.getExclusions().size(),
            security.isAntiFarmEnabled() ? "ON" : "OFF",
            config.isApplyVaryonHealthScaling() && varyonMobScalingBridge.isReflectionReady() ? "ON" : "OFF");
    }
    
    // =========================================================================
    // RefChangeSystem Implementation - DeathComponent
    // =========================================================================
    
    @Nonnull
    @Override
    public ComponentType<EntityStore, DeathComponent> componentType() {
        return DeathComponent.getComponentType();
    }
    
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        // Only process entities that also have NPCEntity component
        return NPCEntity.getComponentType();
    }

    /**
     * Called when a DeathComponent is ADDED to an entity.
     * This is the death event - the entity just died.
     */
    @Override
    public void onComponentAdded(
        @Nonnull Ref<EntityStore> ref, 
        @Nonnull DeathComponent deathComponent, 
        @Nonnull Store<EntityStore> store, 
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // Get the NPCEntity component (guaranteed by our query)
        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        
        if (npc == null) {
            return;
        }
        
        String mobId = npc.getNPCTypeId();

        JobsLogger.debug("=== NPC DEATH: %s ===", mobId != null ? mobId : "NULL_ID");

        // Always clean up last-attacker entry to prevent memory leaks
        MobLastAttackerSystem.AttackerEntry lastAttackerEntry =
            lastAttackerSystem != null ? lastAttackerSystem.removeAndGet(ref) : null;

        if (config == null || !config.isEnabled()) {
            JobsLogger.debug("SKIP: Config null or disabled");
            return;
        }

        // Primary: killer from DeathComponent (final blow)
        Player killer = null;
        PlayerRef killerPlayerRef = null;

        Damage deathInfo = deathComponent.getDeathInfo();
        if (deathInfo != null) {
            Damage.Source source = deathInfo.getSource();
            if (source instanceof Damage.EntitySource entitySource) {
                Ref<EntityStore> killerRef = entitySource.getRef();
                if (killerRef != null && killerRef.isValid()) {
                    killer = store.getComponent(killerRef, Player.getComponentType());
                    killerPlayerRef = store.getComponent(killerRef, PlayerRef.getComponentType());
                }
            }
        }

        // Fallback: last player who hit the mob (even if not the killer)
        if ((killer == null || killerPlayerRef == null) && lastAttackerEntry != null
                && lastAttackerEntry.attackerRef().isValid()) {
            killer = store.getComponent(lastAttackerEntry.attackerRef(), Player.getComponentType());
            killerPlayerRef = store.getComponent(lastAttackerEntry.attackerRef(), PlayerRef.getComponentType());
            if (killer != null && killerPlayerRef != null) {
                JobsLogger.debug("Using last attacker fallback for %s", mobId);
            }
        }

        if (killer == null || killerPlayerRef == null) {
            JobsLogger.debug("SKIP: No player killer for %s", mobId);
            return;
        }

        processKill(killer, killerPlayerRef, npc, ref, store, commandBuffer);
    }

    @Override
    public void onComponentSet(
        @Nonnull Ref<EntityStore> ref, 
        @Nullable DeathComponent oldComponent, 
        @Nonnull DeathComponent newComponent, 
        @Nonnull Store<EntityStore> store, 
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // DeathComponent is typically only added, not set
        // But handle it just in case
    }

    @Override
    public void onComponentRemoved(
        @Nonnull Ref<EntityStore> ref, 
        @Nonnull DeathComponent component, 
        @Nonnull Store<EntityStore> store, 
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // DeathComponent removal = respawn, not relevant for rewards
    }
    
    // =========================================================================
    // Reward Processing Pipeline
    // =========================================================================
    
    /**
     * Process a mob kill through all security layers.
     * 
     * <p>This method is optimized for minimal allocations:
     * <ul>
     *   <li>No String concatenation in hot path</li>
     *   <li>ThreadLocalRandom (no contention)</li>
     *   <li>Primitive operations where possible</li>
     * </ul>
     */
    private void processKill(
        Player killer,
        PlayerRef killerPlayerRef, 
        NPCEntity npc,
        Ref<EntityStore> mobRef, 
        Store<EntityStore> store, 
        CommandBuffer<EntityStore> commandBuffer
    ) {
        String mobId = npc.getNPCTypeId();
        if (mobId == null) {
            mobId = "unknown";
        }
        
        UUID playerUuid = killerPlayerRef.getUuid();
        
        // ─────────────────────────────────────────────────────────────
        // LAYER 1: EXCLUSION CHECK
        // O(1) HashSet lookup for NPCs that should never give rewards
        // ─────────────────────────────────────────────────────────────
        if (exclusionSet.contains(mobId)) {
            JobsLogger.debug("BLOCKED [Exclusion]: %s", mobId);
            return;
        }
        
        // ─────────────────────────────────────────────────────────────
        // LAYER 2: TIER LOOKUP
        // Pattern matching with O(1) cache for known mobs
        // ─────────────────────────────────────────────────────────────
        String tierName = tierMatcher.findTier(mobId);
        
        if ("NONE".equals(tierName)) {
            JobsLogger.debug("BLOCKED [Tier=NONE]: %s", mobId);
            return;
        }
        
        TierConfig tier = config.getTierSafe(tierName, mappingsConfig.getDefaultTier());
        
        // ─────────────────────────────────────────────────────────────
        // LAYER 3: DROP CHANCE
        // Random roll - skip if tier has <100% chance
        // VIP players get bonus chance added to base drop chance
        // ─────────────────────────────────────────────────────────────
        int baseDropChance = tier.getDropChance();
        int vipChanceBonus = VaryonEcotalePlugin.getInstance().getJobsModule().getConfig().getVipMultipliers().calculateChanceBonus(killer);
        int effectiveDropChance = Math.min(baseDropChance + vipChanceBonus, 100);
        
        if (effectiveDropChance < 100) {
            int roll = ThreadLocalRandom.current().nextInt(100);
            if (roll >= effectiveDropChance) {
                JobsLogger.debug("BLOCKED [Chance %d >= %d%% (base=%d, vip=+%d)]: %s", 
                    roll, effectiveDropChance, baseDropChance, vipChanceBonus, mobId);
                return;
            }
        }
        
        // ─────────────────────────────────────────────────────────────
        // LAYER 4: RATE LIMITING
        // Per-player burst protection using token bucket
        // ─────────────────────────────────────────────────────────────
        if (!rateLimiter.tryAcquire(playerUuid)) {
            rewardsBlocked.incrementAndGet();
            JobsLogger.debug("BLOCKED [RateLimit]: Player %s", playerUuid);
            return;
        }
        
        // ─────────────────────────────────────────────────────────────
        // LAYER 5: ANTI-FARM
        // Diminishing returns for killing same mob type repeatedly
        // ─────────────────────────────────────────────────────────────
        float antiFarmMultiplier = antiFarm.getMultiplierAndRecord(playerUuid, mobId);
        
        // ─────────────────────────────────────────────────────────────
        // LAYER 6: REWARD CALCULATION
        // Random amount within tier range, adjusted by anti-farm
        // ─────────────────────────────────────────────────────────────
        int baseCoins = tier.getMinCoins();
        int range = tier.getMaxCoins() - tier.getMinCoins();
        if (range > 0) {
            baseCoins += ThreadLocalRandom.current().nextInt(range + 1);
        }
        
        // VIP Multiplier (killer implements CommandSender which has hasPermission)
        float vipMultiplier = VaryonEcotalePlugin.getInstance().getJobsModule().getConfig().getVipMultipliers().calculateMultiplier(killer);

        float varyonHpMult = 1.0f;
        float rawVaryonHp = Float.NaN;
        if (config.isApplyVaryonHealthScaling() && varyonMobScalingBridge.isReflectionReady()) {
            rawVaryonHp = varyonMobScalingBridge.readVictimHealthMultiplier(store, mobRef);
            varyonHpMult = clampVaryonHpMultiplier(rawVaryonHp);
        }

        float exactCoins = baseCoins * antiFarmMultiplier * vipMultiplier * varyonHpMult;
        int finalCoins = (int) exactCoins;
        
        // Probabilistic rounding: 1.2 coins = 1 coin + 20% chance of extra coin
        // This ensures even small drops benefit from multipliers over time
        if (ThreadLocalRandom.current().nextFloat() < (exactCoins - finalCoins)) {
            finalCoins++;
        }
        
        if (finalCoins < 1) {
            rewardsBlocked.incrementAndGet();
            JobsLogger.debug("BLOCKED [AntiFarm=%.0f%%]: %s -> 0 coins", 
                antiFarmMultiplier * 100, mobId);
            return;
        }
        
        long totalValue = (long) finalCoins * tier.getCoinValue();
        
        // ─────────────────────────────────────────────────────────────
        // LAYER 7: GLOBAL ECONOMY CAP
        // Server-wide limit on currency injection per hour
        // ─────────────────────────────────────────────────────────────
        if (!economyCap.tryInject(totalValue)) {
            rewardsBlocked.incrementAndGet();
            JobsLogger.debug("BLOCKED [EconomyCap]: Tried to inject %d, cap full", totalValue);
            return;
        }
        
        // ─────────────────────────────────────────────────────────────
        // SUCCESS: GIVE REWARD
        // Uses physical coins if addon is available, otherwise direct balance
        // ─────────────────────────────────────────────────────────────
        if (CoinsBridge.isAvailable()) {
            // Physical coins addon installed - drop coins in world
            
            CoinsBridge.dropCoinsAtEntity(mobRef, store, commandBuffer, totalValue);
        } else {
            // No coins addon - deposit directly to player's balance
            EconomyBridge.deposit(playerUuid, (double) totalValue, "Mob kill: " + mobId);
        }
        
        // Update statistics (atomic for thread-safety)
        totalRewardsGiven.incrementAndGet();
        totalValueInjected.addAndGet(totalValue);
        
        JobsLogger.debug("SUCCESS: %s -> %d coins (exact=%.2f, antiFarm=%.0f%%, vip=%.2fx, varyonHp=%.3fx, mode=%s)",
            mobId, finalCoins, exactCoins, antiFarmMultiplier * 100, vipMultiplier, varyonHpMult,
            CoinsBridge.isAvailable() ? "COINS" : "BALANCE");

        if (RewardNotifier.shouldShowMobKillBreakdown(totalValue)) {
            sendMobKillBreakdown(killerPlayerRef, mobId, tierName, tier, baseCoins, finalCoins, exactCoins,
                antiFarmMultiplier, vipMultiplier, varyonHpMult, rawVaryonHp,
                config.isApplyVaryonHealthScaling(), varyonMobScalingBridge.isReflectionReady());
        }
    }
    
    // =========================================================================
    // Maintenance
    // =========================================================================
    
    /**
     * Periodic cleanup of expired tracking data.
     * Should be called every 5-10 minutes via a scheduled task.
     */
    public void performCleanup() {
        int antiFarmCleaned = antiFarm.cleanup();
        rateLimiter.cleanup(); // RateLimiter.cleanup() returns void
        
        if (antiFarmCleaned > 0) {
            JobsLogger.debug("Cleanup: removed %d anti-farm trackers", antiFarmCleaned);
        }
    }
    
    // =========================================================================
    // Monitoring API
    // =========================================================================
    
    /** Total rewards successfully given since server start */
    public long getTotalRewardsGiven() {
        return totalRewardsGiven.get();
    }
    
    /** Total currency value injected into the economy (in base units) */
    public long getTotalValueInjected() {
        return totalValueInjected.get();
    }
    
    /** Number of rewards blocked by security layers */
    public long getRewardsBlocked() {
        return rewardsBlocked.get();
    }
    
    /** Current size of the tier matching cache */
    public int getTierCacheSize() {
        return tierMatcher.getCacheSize();
    }
    
    /** Number of active player anti-farm trackers */
    public int getActiveAntiFarmTrackers() {
        return antiFarm.getActiveTrackerCount();
    }
    
    /** Remaining economy cap capacity for this hour */
    public long getRemainingEconomyCap() {
        return economyCap.getRemainingCapacity();
    }
    
    /** Get current configuration (for admin inspection) */
    @Nullable
    public MobKillsConfig getConfig() {
        return config;
    }

    private static float clampVaryonHpMultiplier(float rawHp) {
        if (!(rawHp > 0 && Float.isFinite(rawHp))) {
            return 1.0f;
        }
        return Math.max(VARYON_HP_CLAMP_MIN, Math.min(VARYON_HP_CLAMP_MAX, rawHp));
    }

    private static void sendMobKillBreakdown(
        PlayerRef playerRef,
        String mobId,
        String tierName,
        TierConfig tier,
        int baseCoins,
        int finalCoins,
        float exactCoins,
        float antiFarmMultiplier,
        float vipMultiplier,
        float varyonClamped,
        float rawVaryonHp,
        boolean scalingEnabled,
        boolean varyonBridgeReady
    ) {
        String coinWord = coinWordFr(tier);
        String pvPhrase = formatPvPhrase(scalingEnabled, varyonBridgeReady, varyonClamped, rawVaryonHp);
        String roundingNote = Math.abs(finalCoins - exactCoins) > 0.001f
            ? String.format(Locale.FRANCE, " · valeur exacte intermédiaire: %.2f", exactCoins)
            : "";

        playerRef.sendMessage(Message.join(
            Message.raw("[Ecotale Jobs] ").color(Color.GRAY),
            Message.raw("+" + finalCoins).color(new Color(50, 205, 50)).bold(true),
            Message.raw(" " + coinWord).color(Color.WHITE),
            Message.raw(" · ").color(Color.DARK_GRAY),
            Message.raw(mobId).color(new Color(200, 200, 200)),
            Message.raw(" · tier ").color(Color.GRAY),
            Message.raw(tierName).color(new Color(100, 200, 255))
        ));

        String detail = String.format(Locale.FRANCE,
            "Détail: base %d × anti-farm %.0f %% × VIP %s × %s → %d %s%s",
            baseCoins,
            antiFarmMultiplier * 100.0,
            formatMul(vipMultiplier),
            pvPhrase,
            finalCoins,
            coinWord,
            roundingNote);
        playerRef.sendMessage(Message.raw(detail).color(Color.GRAY));
    }

    private static String coinWordFr(TierConfig tier) {
        String n = tier.getCoinTypeName();
        return "COPPER".equalsIgnoreCase(n) ? "cuivre" : n.toLowerCase(Locale.ROOT);
    }

    private static String formatMul(float f) {
        return String.format(Locale.FRANCE, "×%.2f", f);
    }

    private static String formatPvPhrase(boolean scalingEnabled, boolean bridgeReady, float clamped, float raw) {
        if (!scalingEnabled) {
            return "PV ×1,00 (échelle PV désactivée)";
        }
        if (!bridgeReady) {
            return "PV ×1,00 (composant Varyon indisponible)";
        }
        if (!(raw > 0) || !Float.isFinite(raw)) {
            return String.format(Locale.FRANCE, "PV %s (lecture invalide)", formatMul(clamped));
        }
        if (Math.abs(clamped - raw) <= 0.001f) {
            return String.format(Locale.FRANCE, "PV %s", formatMul(clamped));
        }
        return String.format(Locale.FRANCE, "PV %s (brut %s, plage %.2f–%.2f)",
            formatMul(clamped), formatMul(raw), VARYON_HP_CLAMP_MIN, VARYON_HP_CLAMP_MAX);
    }
}
