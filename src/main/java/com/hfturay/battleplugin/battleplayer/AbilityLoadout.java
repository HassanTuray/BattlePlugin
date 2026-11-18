package com.hfturay.battleplugin.battleplayer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.BiFunction;

import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.AbilitySlot;

/**
 * The abilities a player has on each of their three items, plus which one each item currently has selected.
 *
 * Plain Java with no Bukkit in it, so the rules (any number of abilities per item, empty items are fine,
 * removing keeps the selection sensible) can be tested without a server.
 */
public final class AbilityLoadout {
    private final EnumMap<AbilitySlot, ArrayList<Ability>> abilities = new EnumMap<>(AbilitySlot.class);
    private final EnumMap<AbilitySlot, Integer> selected = new EnumMap<>(AbilitySlot.class);

    public AbilityLoadout() {
        for (AbilitySlot slot : AbilitySlot.values()) {
            abilities.put(slot, new ArrayList<>());
            selected.put(slot, 0);
        }
    }

    /** The live list for a slot. Adding to it directly is fine (the selection index is clamped when read). */
    public ArrayList<Ability> list(AbilitySlot slot) {
        return abilities.get(slot);
    }

    public int size(AbilitySlot slot) {
        return abilities.get(slot).size();
    }

    public int total() {
        int total = 0;
        for (AbilitySlot slot : AbilitySlot.values()) {
            total += size(slot);
        }
        return total;
    }

    /** The ability currently selected on this item, or null if the item has none. */
    public Ability current(AbilitySlot slot) {
        List<Ability> list = abilities.get(slot);
        if (list.isEmpty()) {
            return null;
        }
        int index = selected.get(slot);
        if (index < 0 || index >= list.size()) {
            index = 0;
            selected.put(slot, 0);
        }
        return list.get(index);
    }

    /** Moves the selection on and returns the newly selected ability, or null if the item has none. */
    public Ability next(AbilitySlot slot) {
        List<Ability> list = abilities.get(slot);
        if (list.isEmpty()) {
            return null;
        }
        int index = (selected.get(slot) + 1) % list.size();
        selected.put(slot, index);
        return list.get(index);
    }

    /** True if this exact ability object is loaded (not just one of the same type). */
    public boolean contains(Ability ability) {
        for (AbilitySlot slot : AbilitySlot.values()) {
            for (Ability loaded : list(slot)) {
                if (loaded == ability) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean has(Class<? extends Ability> type) {
        return find(type) != null;
    }

    /** The first ability that is an instance of the type, across all three items, or null. */
    public <T extends Ability> T find(Class<T> type) {
        for (AbilitySlot slot : AbilitySlot.values()) {
            for (Ability ability : abilities.get(slot)) {
                if (type.isInstance(ability)) {
                    return type.cast(ability);
                }
            }
        }
        return null;
    }

    /** Adds the ability to the item. Returns false (and does nothing) if the player already has that ability. */
    public boolean add(AbilitySlot slot, Ability ability) {
        if (has(ability.getClass())) {
            return false;
        }
        abilities.get(slot).add(ability);
        return true;
    }

    /**
     * Removes every ability of exactly this class. The selection stays on the same ability where possible.
     * Returns what was removed (empty if the player did not have it).
     */
    public List<Ability> remove(Class<? extends Ability> type) {
        List<Ability> removed = new ArrayList<>();

        for (AbilitySlot slot : AbilitySlot.values()) {
            ArrayList<Ability> list = abilities.get(slot);

            for (int i = list.size() - 1; i >= 0; i--) {
                if (list.get(i).getClass() != type) {
                    continue;
                }

                removed.add(list.remove(i));

                int current = selected.get(slot);
                if (i < current) {
                    selected.put(slot, current - 1);
                }
            }

            if (selected.get(slot) >= list.size()) {
                selected.put(slot, 0);
            }
        }

        removed.forEach(Ability::cancel);
        return removed;
    }

    /** Removes everything and resets the selections. Returns how many abilities were removed. */
    public int clear() {
        int count = 0;
        for (AbilitySlot slot : AbilitySlot.values()) {
            ArrayList<Ability> list = abilities.get(slot);
            count += list.size();
            list.forEach(Ability::cancel);
            list.clear();
            selected.put(slot, 0);
        }
        return count;
    }

    /**
     * Gives this loadout every ability the other one has that it does not, on the same items.
     * `create` builds a fresh instance owned by this loadout's player. Returns how many were added.
     */
    public int addMissingFrom(AbilityLoadout other, BiFunction<AbilitySlot, Class<? extends Ability>, Ability> create) {
        int added = 0;
        for (AbilitySlot slot : AbilitySlot.values()) {
            for (Ability theirs : new ArrayList<>(other.list(slot))) {
                if (has(theirs.getClass())) {
                    continue;
                }
                if (add(slot, create.apply(slot, theirs.getClass()))) {
                    added++;
                }
            }
        }
        return added;
    }

    /** Removes every ability of this loadout that the other one also has. Returns how many were removed. */
    public int removeCommonWith(AbilityLoadout other) {
        int removed = 0;
        for (AbilitySlot slot : AbilitySlot.values()) {
            for (Ability theirs : new ArrayList<>(other.list(slot))) {
                removed += remove(theirs.getClass()).size();
            }
        }
        return removed;
    }
}
