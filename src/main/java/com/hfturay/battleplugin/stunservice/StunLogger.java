package com.hfturay.battleplugin.stunservice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.loggerservice.PlayerLogger;

public class StunLogger implements Listener {
    private Map<UUID, Stun> stuns = new HashMap<>();

    public void start() {
        new BukkitRunnable() {
            public void run() {
                for(UUID id : stuns.keySet()) {
                    if (stuns.get(id) == null) {
                        continue;
                    }

                    boolean res = stuns.get(id).apply();

                    if (res) {
                        stuns.put(id, null);
                    }    
                }
            }
        }.runTaskTimer(BattlePlugin.getInstance(), 1L, 1L);
    }

    public void add(BattlePlayer battlePlayer, Stun stun) {
        add(battlePlayer.getPlayer(), stun);
    }

    public void add(Entity entity, Stun stun) {
        UUID id = entity.getUniqueId();

        if (stuns.containsKey(id) && stuns.get(id) != null) {
            stuns.get(id).duration = 0;
            stuns.put(id, stun);
            return;
        }

        stuns.put(id, stun);
    }

    public boolean isStunned(BattlePlayer battlePlayer) {
        return isStunned(battlePlayer.getPlayer());
    }

    public boolean isStunned(Entity entity) {
        UUID id = entity.getUniqueId();

        if (!stuns.containsKey(id)) {
            return false;
        }

        Stun stun = stuns.get(id);

        return stun != null && stun.duration > 0;
    }

    public boolean velocityIsForced(BattlePlayer battlePlayer) {
        return velocityIsForced(battlePlayer.getPlayer());
    }

    public boolean velocityIsForced(Entity entity) {
        UUID id = entity.getUniqueId();

        if(!stuns.containsKey(id)) {
            return false;
        }

        Stun stun = stuns.get(id);

        return stun != null && stun.duration > 0 && stun.forcedVelocity != null;
    }

    public void removeStun(BattlePlayer battlePlayer) {
        removeStun(battlePlayer.getPlayer());
    }

    public void removeStun(Entity entity) {
        UUID id = entity.getUniqueId();

        if (!stuns.containsKey(id)) {
            return;
        }

        Stun stun = stuns.get(id);
        stun.cancel();
        stuns.put(id, null);

    }

    public Stun getStun(BattlePlayer battlePlayer) {
        return getStun(battlePlayer.getPlayer());
    }

    public Stun getStun(Entity entity) {
        UUID id = entity.getUniqueId();

        if (stuns.containsKey(id)) {
            return stuns.get(id);    
        }

        return null;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        if (isStunned(player) && !velocityIsForced(player)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            Stun stun = stuns.get(player.getUniqueId());

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
    }
}
