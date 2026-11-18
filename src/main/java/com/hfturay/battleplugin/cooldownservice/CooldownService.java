package com.hfturay.battleplugin.cooldownservice;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntSupplier;

import org.bukkit.Bukkit;

import com.hfturay.battleplugin.ability.Ability;

/**
 * Who can use which ability again, and when.
 *
 * - Time is the server tick, the same clock abilities, projectiles and effects run on. If the server lags, cooldowns
 *   slow down with everything else instead of expiring early.
 * - Stored as an expiry tick, so there is no per-tick loop. Expired entries are dropped when they are looked at.
 * - Keyed by player and ability CLASS, so loading and removing an ability does not reset its cooldown.
 * - Cooldowns survive death and rejoining (they only go when they expire or are reset).
 */
public final class CooldownService {
    private final IntSupplier clock;
    private final Map<UUID, Map<Class<? extends Ability>, Integer>> expiries = new HashMap<>();

    public CooldownService() {
        this(Bukkit::getCurrentTick);
    }

    /** @param clock the current tick. Passing your own is for testing without a server. */
    public CooldownService(IntSupplier clock) {
        this.clock = clock;
    }

    /** Starts (or restarts) a cooldown. Zero or less means no cooldown. */
    public void start(UUID player, Class<? extends Ability> type, int ticks) {
        if (ticks <= 0) {
            reset(player, type);
            return;
        }
        expiries.computeIfAbsent(player, id -> new HashMap<>()).put(type, clock.getAsInt() + ticks);
    }

    /** Ticks until the ability is usable again. 0 if it is ready. */
    public int remainingTicks(UUID player, Class<? extends Ability> type) {
        Map<Class<? extends Ability>, Integer> mine = expiries.get(player);
        if (mine == null) {
            return 0;
        }

        Integer expiry = mine.get(type);
        if (expiry == null) {
            return 0;
        }

        int remaining = expiry - clock.getAsInt();
        if (remaining <= 0) {
            mine.remove(type);
            return 0;
        }
        return remaining;
    }

    public boolean isReady(UUID player, Class<? extends Ability> type) {
        return remainingTicks(player, type) == 0;
    }

    /** Takes time off a running cooldown (cooldown reduction). Does nothing if it is not running. */
    public void reduce(UUID player, Class<? extends Ability> type, int ticks) {
        int remaining = remainingTicks(player, type);
        if (remaining == 0 || ticks <= 0) {
            return;
        }

        if (ticks >= remaining) {
            reset(player, type);
        } else {
            expiries.get(player).put(type, clock.getAsInt() + remaining - ticks);
        }
    }

    /** Makes one ability usable right now. */
    public void reset(UUID player, Class<? extends Ability> type) {
        Map<Class<? extends Ability>, Integer> mine = expiries.get(player);
        if (mine != null) {
            mine.remove(type);
        }
    }

    /** Makes every ability usable right now. Returns how many cooldowns were actually running. */
    public int resetAll(UUID player) {
        Map<Class<? extends Ability>, Integer> mine = expiries.remove(player);
        if (mine == null) {
            return 0;
        }

        int now = clock.getAsInt();
        int running = 0;
        for (int expiry : mine.values()) {
            if (expiry > now) {
                running++;
            }
        }
        return running;
    }

    /** Forgets expired entries for a player (called when they leave). Running cooldowns are kept. */
    public void prune(UUID player) {
        Map<Class<? extends Ability>, Integer> mine = expiries.get(player);
        if (mine == null) {
            return;
        }

        int now = clock.getAsInt();
        Iterator<Integer> it = mine.values().iterator();
        while (it.hasNext()) {
            if (it.next() <= now) {
                it.remove();
            }
        }
        if (mine.isEmpty()) {
            expiries.remove(player);
        }
    }

    /** Ticks as seconds with one decimal, rounded UP so it never says "0.0" while time is left: 25 ticks -> "1.3". */
    public static String formatSeconds(int ticks) {
        int tenths = (Math.max(0, ticks) + 1) / 2;
        return (tenths / 10) + "." + (tenths % 10);
    }
}
