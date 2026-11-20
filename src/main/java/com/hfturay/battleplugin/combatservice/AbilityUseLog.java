package com.hfturay.battleplugin.combatservice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;

import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.AbilityRegistry;
import com.hfturay.battleplugin.ability.AbilitySlot;

/**
 * Remembers which attack or elemental ability each player used last, and when. Ability.activate() records into it.
 *
 * Damage does not say which ability dealt it, so whoever wants to know (Guy learning moves from the people hurting
 * his owner) asks: "what did this attacker use most recently?". That is a good guess for a hit that happened a
 * moment after a cast, and it is only ever used for damage that came from an ability, never from a plain punch.
 *
 * Only abilities on the magma cream (attack) and the blaze rod (elemental) are kept; those are the ones that can be
 * learned. Test dummies are skipped.
 */
public final class AbilityUseLog {

    private record Use(Class<? extends Ability> type, int tick) { }

    private final Map<UUID, Use> last = new HashMap<>();

    /** Whether Guy may learn this ability: it is on the attack item or the elemental item, and is not a test dummy. */
    public static boolean learnable(Class<? extends Ability> type) {
        if (type.getSimpleName().startsWith("Dummy")) {
            return false;
        }
        return AbilityRegistry.of(type)
            .map(entry -> entry.slot() == AbilitySlot.ATTACK || entry.slot() == AbilitySlot.ELEMENTAL)
            .orElse(false);
    }

    public void record(UUID player, Class<? extends Ability> type) {
        if (learnable(type)) {
            last.put(player, new Use(type, Bukkit.getCurrentTick()));
        }
    }

    /** The learnable ability this player used within the last maxAgeTicks, or null. */
    public Class<? extends Ability> recent(UUID player, int maxAgeTicks) {
        Use use = last.get(player);
        if (use == null || Bukkit.getCurrentTick() - use.tick() > maxAgeTicks) {
            return null;
        }
        return use.type();
    }

    public void forget(UUID player) {
        last.remove(player);
    }
}
