package com.hfturay.battleplugin.combatservice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntSupplier;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * "The next fall does not hurt", for leaps and similar moves.
 *
 * It ends in whichever way comes first:
 *   - the fall damage is cancelled (the normal case)
 *   - the player lands without taking damage (water, a short drop): a few ticks after touching down
 *   - a time limit, as a backstop
 *
 * The old version only ended when fall damage was cancelled, so a landing in water left the immunity
 * armed and it silently ate a completely unrelated fall later.
 */
public final class FallImmunityService implements Listener {
    /** Longest a fall immunity can last. */
    public static final int MAX_TICKS = 20 * 30;

    /** Ticks after touching down before the immunity is dropped. Gives the fall damage event time to arrive first. */
    public static final int LANDING_GRACE_TICKS = 5;

    private static final class Entry {
        final int expiry;
        boolean sawAir = false;     // has left the ground since it was granted
        int landedAt = -1;          // tick they touched down after being in the air
        Entry(int expiry) { this.expiry = expiry; }
    }

    private final Map<UUID, Entry> entries = new HashMap<>();
    private final IntSupplier clock;

    public FallImmunityService() {
        this(Bukkit::getCurrentTick);
    }

    public FallImmunityService(IntSupplier clock) {
        this.clock = clock;
    }

    public void grantFallImmunity(UUID id) {
        entries.put(id, new Entry(clock.getAsInt() + MAX_TICKS));
    }

    public void grantFallImmunity(Entity entity) {
        grantFallImmunity(entity.getUniqueId());
    }

    public boolean hasFallImmunity(UUID id) {
        Entry entry = entries.get(id);
        if (entry == null) {
            return false;
        }
        int now = clock.getAsInt();
        boolean timedOut = now >= entry.expiry;
        boolean landed = entry.landedAt >= 0 && now - entry.landedAt >= LANDING_GRACE_TICKS;
        if (timedOut || landed) {
            entries.remove(id);
            return false;
        }
        return true;
    }

    public boolean hasFallImmunity(Entity entity) {
        return hasFallImmunity(entity.getUniqueId());
    }

    public void clearFallImmunity(UUID id) {
        entries.remove(id);
    }

    public void clearFallImmunity(Entity entity) {
        clearFallImmunity(entity.getUniqueId());
    }

    /** Feed this the player's ground state as they move. Public so it can be tested without a server. */
    public void trackMovement(UUID id, boolean onGround) {
        Entry entry = entries.get(id);
        if (entry == null) {
            return;
        }
        if (!onGround) {
            entry.sawAir = true;
            entry.landedAt = -1;
        } else if (entry.sawAir && entry.landedAt < 0) {
            entry.landedAt = clock.getAsInt();
        }
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) {
            return;
        }
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL && hasFallImmunity(player)) {
            e.setCancelled(true);
            clearFallImmunity(player);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        trackMovement(player.getUniqueId(), player.isOnGround());
    }

    public static boolean isGrounded(Entity entity) {
        if (entity instanceof Player) {
            return entity.getVelocity().getY() == 0 && entity.isOnGround();
        }

        return entity.isOnGround();
    }
}
