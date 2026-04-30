package fr.varyon.ecotale.jobs.integration;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * Reads mob HP scaling from the Varyon mod ({@code MobScalingComponent}) via reflection only.
 */
public final class VaryonMobScalingBridge {

    private final Class<?> scalingClass;
    private final Method getHealthMultiplierMethod;
    private final Method getComponentTypeMethod;
    private final boolean reflectionReady;

    @Nullable
    private volatile ComponentType<EntityStore, ?> cachedComponentType;

    public VaryonMobScalingBridge() {
        Class<?> scl = null;
        Method gm = null;
        Method gct = null;
        try {
            scl = Class.forName("com.varyon.component.MobScalingComponent");
            gm = scl.getMethod("getHealthMultiplier");
            gct = scl.getMethod("getComponentType");
        } catch (Throwable ignored) {
        }
        this.scalingClass = scl;
        this.getHealthMultiplierMethod = gm;
        this.getComponentTypeMethod = gct;
        this.reflectionReady = scl != null && gm != null && gct != null;
    }

    public boolean isReflectionReady() {
        return reflectionReady;
    }

    @Nullable
    private ComponentType<EntityStore, ?> resolveComponentType() {
        ComponentType<EntityStore, ?> ct = cachedComponentType;
        if (ct != null || !reflectionReady) {
            return ct;
        }
        synchronized (this) {
            ct = cachedComponentType;
            if (ct != null) {
                return ct;
            }
            try {
                Object o = getComponentTypeMethod.invoke(null);
                if (o instanceof ComponentType<?, ?> raw) {
                    @SuppressWarnings("unchecked")
                    ComponentType<EntityStore, ?> typed =
                        (ComponentType<EntityStore, ?>) raw;
                    cachedComponentType = typed;
                    return typed;
                }
            } catch (Throwable ignored) {
            }
            return null;
        }
    }

    /**
     * @return {@code MobScalingComponent#getHealthMultiplier()} or 1.0 when absent / no Varyon / no scaling on entity
     */
    public float readVictimHealthMultiplier(Store<EntityStore> store, Ref<EntityStore> mobRef) {
        if (!reflectionReady) {
            return 1.0f;
        }
        ComponentType<EntityStore, ?> ct = resolveComponentType();
        if (ct == null) {
            return 1.0f;
        }
        try {
            Object comp = store.getComponent(mobRef, ct);
            if (comp == null || scalingClass == null || !scalingClass.isInstance(comp)) {
                return 1.0f;
            }
            Object h = getHealthMultiplierMethod.invoke(comp);
            return h instanceof Float f ? f : 1.0f;
        } catch (Throwable ignored) {
            return 1.0f;
        }
    }
}
