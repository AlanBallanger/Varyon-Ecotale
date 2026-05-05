package fr.varyon.ecotale.jobs.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MobLastAttackerSystem extends DamageEventSystem {

    public record AttackerEntry(UUID playerUuid, Ref<EntityStore> attackerRef) {}

    private final ConcurrentHashMap<Integer, AttackerEntry> lastAttackers = new ConcurrentHashMap<>();

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return NPCEntity.getComponentType();
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {
        if (damage.isCancelled() || damage.getAmount() <= 0f) {
            return;
        }
        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return;
        }
        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef == null || !attackerRef.isValid()) {
            return;
        }
        Player player = store.getComponent(attackerRef, Player.getComponentType());
        if (player == null) {
            player = commandBuffer.getComponent(attackerRef, Player.getComponentType());
        }
        if (player == null) {
            return;
        }
        PlayerRef playerRef = store.getComponent(attackerRef, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        Ref<EntityStore> victimRef = archetypeChunk.getReferenceTo(index);
        lastAttackers.put(System.identityHashCode(victimRef), new AttackerEntry(playerRef.getUuid(), attackerRef));
    }

    @Nullable
    public AttackerEntry removeAndGet(@Nonnull Ref<EntityStore> victimRef) {
        return lastAttackers.remove(System.identityHashCode(victimRef));
    }
}
