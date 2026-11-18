package com.hfturay.battleplugin.combatservice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntSupplier;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

/**
 * Who cannot be damaged right now, and who cannot be targeted right now.
 *
 * Two separate timers per entity. Damage immunity is enforced by DamageService (it blocks the damage only).
 * Untargetable is enforced by Targets (the one place every area attack, hit scan and projectile asks "can I hit
 * this?"), so an untargetable entity gets none of an ability's effects (knockback, potions, stuns, pulls) and
 * projectiles and beams pass through it. DamageService also stops mobs from picking it as an AI target.
 *
 * Like the CooldownService, this stores an expiry tick instead of counting down every tick, so there is no
 * per-tick loop and no entries that tick forever. Granting while already immune keeps whichever ends later
 * (it does not add the durations together). Expired entries are dropped when they are looked at.
 *
 * The actual blocking is done by DamageService.
 */
public final class ImmunityService {
    private final Map<UUID, Integer> expiry = new HashMap<>();
    private final Map<UUID, Integer> untargetableExpiry = new HashMap<>();
    private final IntSupplier clock;

    public ImmunityService() {
        this(Bukkit::getCurrentTick);
    }

    public ImmunityService(IntSupplier clock) {
        this.clock = clock;
    }

    /** Immune for this many ticks from now, or until the current immunity ends if that is later. */
    public void grant(UUID id, int ticks) {
        if (ticks <= 0) {
            return;
        }
        expiry.merge(id, clock.getAsInt() + ticks, Math::max);
    }

    public void grant(Entity entity, int ticks) {
        grant(entity.getUniqueId(), ticks);
    }

    public int remainingTicks(UUID id) {
        Integer end = expiry.get(id);
        if (end == null) {
            return 0;
        }
        int left = end - clock.getAsInt();
        if (left <= 0) {
            expiry.remove(id);
            return 0;
        }
        return left;
    }

    public int remainingTicks(Entity entity) {
        return remainingTicks(entity.getUniqueId());
    }

    public boolean isImmune(UUID id) {
        return remainingTicks(id) > 0;
    }

    public boolean isImmune(Entity entity) {
        return isImmune(entity.getUniqueId());
    }

    /** Untargetable for this many ticks from now, or until the current untargetable state ends if that is later. */
    public void grantUntargetable(UUID id, int ticks) {
        if (ticks <= 0) {
            return;
        }
        untargetableExpiry.merge(id, clock.getAsInt() + ticks, Math::max);
    }

    public void grantUntargetable(Entity entity, int ticks) {
        grantUntargetable(entity.getUniqueId(), ticks);
    }

    public int untargetableRemainingTicks(UUID id) {
        Integer end = untargetableExpiry.get(id);
        if (end == null) {
            return 0;
        }
        int left = end - clock.getAsInt();
        if (left <= 0) {
            untargetableExpiry.remove(id);
            return 0;
        }
        return left;
    }

    public boolean isUntargetable(UUID id) {
        return untargetableRemainingTicks(id) > 0;
    }

    public boolean isUntargetable(Entity entity) {
        return isUntargetable(entity.getUniqueId());
    }

    /** Ends the untargetable state now, leaving damage immunity alone. */
    public void clearUntargetable(UUID id) {
        untargetableExpiry.remove(id);
    }

    public void clearUntargetable(Entity entity) {
        clearUntargetable(entity.getUniqueId());
    }

    /** Ends the damage immunity now (not the untargetable state; see clearAll). */
    public void clear(UUID id) {
        expiry.remove(id);
    }

    /** Ends both the damage immunity and the untargetable state. Use on quit and death. */
    public void clearAll(UUID id) {
        expiry.remove(id);
        untargetableExpiry.remove(id);
    }

    public void clear(Entity entity) {
        clear(entity.getUniqueId());
    }
}
