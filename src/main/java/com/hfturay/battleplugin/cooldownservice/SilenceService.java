package com.hfturay.battleplugin.cooldownservice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntSupplier;

import org.bukkit.Bukkit;

/**
 * Who is silenced (cannot use abilities) and until when. Same rules as the CooldownService: server ticks, stored as an
 * expiry, no per-tick loop. Asking never changes anything.
 */
public final class SilenceService {
    private final IntSupplier clock;
    private final Map<UUID, Integer> expiries = new HashMap<>();

    public SilenceService() {
        this(Bukkit::getCurrentTick);
    }

    /** @param clock the current tick. Passing your own is for testing without a server. */
    public SilenceService(IntSupplier clock) {
        this.clock = clock;
    }

    /** Silences for the given ticks. If already silenced for longer, the longer one is kept. */
    public void silence(UUID player, int ticks) {
        if (ticks <= 0) {
            return;
        }
        int until = clock.getAsInt() + ticks;
        expiries.merge(player, until, Math::max);
    }

    /** Ticks of silence left. 0 if not silenced. */
    public int remainingTicks(UUID player) {
        Integer expiry = expiries.get(player);
        if (expiry == null) {
            return 0;
        }

        int remaining = expiry - clock.getAsInt();
        if (remaining <= 0) {
            expiries.remove(player);
            return 0;
        }
        return remaining;
    }

    public boolean isSilenced(UUID player) {
        return remainingTicks(player) > 0;
    }

    /** Ends a silence now. */
    public void clear(UUID player) {
        expiries.remove(player);
    }
}
