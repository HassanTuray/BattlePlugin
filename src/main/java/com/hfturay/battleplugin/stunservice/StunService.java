package com.hfturay.battleplugin.stunservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.combatservice.ImmunityService;

/**
 * Keeps track of stunned entities and ticks their stuns.
 *
 * One stun per entity; adding a new one replaces (and properly ends) the old one. A stun is dropped from the
 * map as soon as it is over, so the map only ever holds stuns that are running. Safe to ask about, or remove,
 * an entity that has no stun.
 */
public final class StunService implements Listener {
    private final Map<UUID, Stun> stuns = new HashMap<>();
    private final ImmunityService immunity;
    private final Logger logger;
    private BukkitTask loop;

    public StunService(ImmunityService immunity, Logger logger) {
        this.immunity = immunity;
        this.logger = logger;
    }

    public void start(Plugin plugin) {
        loop = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    /** Ends every stun and stops the loop. */
    public void shutdown() {
        if (loop != null) {
            loop.cancel();
            loop = null;
        }
        for (Stun stun : new ArrayList<>(stuns.values())) {
            safeCancel(stun);
        }
        stuns.clear();
    }

    /** Advances every stun by one tick. Public so it can be driven without a server. */
    public void tick() {
        for (Map.Entry<UUID, Stun> entry : new ArrayList<>(stuns.entrySet())) {
            Stun stun = entry.getValue();
            boolean over;
            try {
                over = stun.tick(immunity);
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "Stun failed and was ended", e);
                safeCancel(stun);
                over = true;
            }
            if (over && stuns.get(entry.getKey()) == stun) {
                stuns.remove(entry.getKey());
            }
        }
    }

    // ---- adding / removing ----

    public void add(BattlePlayer battlePlayer, Stun stun) {
        add(battlePlayer.getPlayer(), stun);
    }

    public void add(Entity entity, Stun stun) {
        Stun old = stuns.put(entity.getUniqueId(), stun);
        if (old != null && old != stun) {
            old.cancel(immunity);
        }
    }

    public void removeStun(BattlePlayer battlePlayer) {
        removeStun(battlePlayer.getPlayer());
    }

    public void removeStun(Entity entity) {
        removeStun(entity.getUniqueId());
    }

    /** Ends the entity's stun, if it has one. */
    public void removeStun(UUID id) {
        Stun stun = stuns.remove(id);
        if (stun != null) {
            safeCancel(stun);
        }
    }

    /** Quit or death: the stun, and any immunity it gave, ends. */
    public void clear(UUID id) {
        removeStun(id);
    }

    // ---- asking ----

    public boolean isStunned(BattlePlayer battlePlayer) {
        return isStunned(battlePlayer.getPlayer());
    }

    public boolean isStunned(Entity entity) {
        Stun stun = stuns.get(entity.getUniqueId());
        return stun != null && stun.isActive();
    }

    public boolean velocityIsForced(BattlePlayer battlePlayer) {
        return velocityIsForced(battlePlayer.getPlayer());
    }

    public boolean velocityIsForced(Entity entity) {
        Stun stun = stuns.get(entity.getUniqueId());
        return stun != null && stun.isActive() && stun.forcedVelocity != null;
    }

    public Stun getStun(BattlePlayer battlePlayer) {
        return getStun(battlePlayer.getPlayer());
    }

    public Stun getStun(Entity entity) {
        return stuns.get(entity.getUniqueId());
    }

    public int activeCount() {
        return stuns.size();
    }

    // ---- movement lock for players ----

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Stun stun = stuns.get(player.getUniqueId());

        if (stun == null || !stun.isActive() || stun.forcedVelocity != null) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        boolean changed = false;
        if (stun.lockX) {
            to.setX(from.getX());
            changed = true;
        }
        if (stun.lockY) {
            to.setY(from.getY());
            changed = true;
        }
        if (stun.lockZ) {
            to.setZ(from.getZ());
            changed = true;
        }
        if (changed) {
            event.setTo(to);
        }
    }

    private void safeCancel(Stun stun) {
        try {
            stun.cancel(immunity);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Cleanup of a stun failed", e);
        }
    }
}
