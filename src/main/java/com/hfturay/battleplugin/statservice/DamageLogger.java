package com.hfturay.battleplugin.statservice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

public class DamageLogger implements Listener {
    private Map<UUID, Integer> immunities = new HashMap<>();

    public void start() {
        new BukkitRunnable() {
            public void run() {
                for (UUID id : immunities.keySet()) {
                    if (immunities.get(id) > 0) {
                        immunities.put(id, immunities.get(id) - 1);
                    }
                }
            }
        }.runTaskTimer(BattlePlugin.getInstance(), 1L, 1L);
    }

    public void grantImmunity(BattlePlayer battlePlayer, int duration) {
        grantImmunity(battlePlayer.getPlayer(), duration);
    }

    public void grantImmunity(Entity entity, int duration) {
        UUID uuid = entity.getUniqueId();

        if (!immunities.containsKey(uuid)) {
            immunities.put(uuid, duration);
        } else {
            int currentDuration = immunities.get(uuid);
            immunities.put(uuid, currentDuration + duration);
        }
    }

    public boolean isImmune(BattlePlayer battlePlayer) {
        return isImmune(battlePlayer.getPlayer());
    }

    public boolean isImmune(Entity entity) {
        UUID uuid = entity.getUniqueId();

        if (!immunities.containsKey(uuid)) {
            return false;
        } else {
            return immunities.get(uuid) > 0;
        }
    }

    public int getRemainingImmuneTicks(BattlePlayer battlePlayer) {
        return getRemainingImmuneTicks(battlePlayer.getPlayer());
    }

    public int getRemainingImmuneTicks(Entity entity) {
        UUID uuid = entity.getUniqueId();

        if (!immunities.containsKey(uuid)) {
            return 0;
        } else {
            return immunities.get(uuid);
        }
    }

    public void removeImmunity(BattlePlayer battlePlayer) {
       removeImmunity(battlePlayer.getPlayer());
    }

    public void removeImmunity(Entity entity) {
        if (isImmune(entity)) {
            UUID id = entity.getUniqueId();
            immunities.put(id, 0);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();
        UUID id = damaged.getUniqueId();

        if (immunities.containsKey(id) && immunities.get(id) > 0) {
            event.setDamage(0);
        }
    }
}


