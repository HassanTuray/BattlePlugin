package com.hfturay.battleplugin.glideservice;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Puts a player in the elytra flying pose without an elytra, and keeps them there until told otherwise.
 *
 * The server normally ends gliding on its own when nothing glide-capable is worn. Before it does, it asks the plugins
 * with an EntityToggleGlideEvent, so this service cancels that event for the players it holds. The pose is only
 * the posture: whatever moves the player (a push every tick, for instance) is up to the ability using it.
 */
public final class GlideService implements Listener {

    private final Set<UUID> held = new HashSet<>();

    /** Starts the flying pose. Safe to call again. */
    public void begin(Player player) {
        held.add(player.getUniqueId());
        player.setGliding(true);
    }

    /** Re-asserts the pose; call every tick while an ability holds the player in it. */
    public void keep(Player player) {
        if (held.contains(player.getUniqueId()) && !player.isGliding()) {
            player.setGliding(true);
        }
    }

    /** Releases the player: they fall out of the pose normally. Safe to call when they were never held. */
    public void end(Player player) {
        if (held.remove(player.getUniqueId())) {
            player.setGliding(false);
        }
    }

    public boolean isHeld(UUID id) {
        return held.contains(id);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onToggleGlide(EntityToggleGlideEvent event) {
        if (!event.isGliding() && held.contains(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        held.remove(event.getPlayer().getUniqueId());
    }
}
