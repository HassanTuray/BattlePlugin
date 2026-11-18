package com.hfturay.battleplugin.items;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Keeps the three ability items with their owner.
 *
 *  - They cannot be dropped (Q, Ctrl+Q, or dragging out of the inventory).
 *  - They do not drop when the player dies. PlayerLogger gives them back on respawn.
 *
 * Moving them into chests and other containers is not blocked.
 */
public final class ItemProtection implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (AbilityItemFactory.isAbilityItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Component.text("Ability items cannot be dropped", NamedTextColor.GRAY));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(AbilityItemFactory::isAbilityItem);
    }
}
