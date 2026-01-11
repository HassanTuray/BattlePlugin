package com.hfturay.battleplugin.falldamageservice;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import com.hfturay.battleplugin.BattlePlugin;

public class FallDamageLogger implements Listener {

    private final Set<UUID> fallImmunity = new HashSet<>();

    public void grantFallImmunity(Entity entity) {
        fallImmunity.add(entity.getUniqueId());
    }

    public boolean hasFallImmunity(Entity entity) {
        return fallImmunity.contains(entity.getUniqueId());
    }

    public void clearFallImmunity(Entity entity) {
        fallImmunity.remove(entity.getUniqueId());
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) {
            return;
        }

        if (e.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (hasFallImmunity(player)) {
                e.setCancelled(true);
                clearFallImmunity(player); 
            }
        }
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();

        if (hasFallImmunity(player)) {
            return;
        }

        if (isGrounded(player)) {
            clearFallImmunity(player);
        }
    }

    public boolean isGrounded(Entity entity) {
        if (entity instanceof Player) {
            return entity.getVelocity().getY() == 0 && entity.isOnGround();
        }
        
        return entity.isOnGround();
    }
}
