package com.hfturay.battleplugin.items;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

import net.kyori.adventure.text.Component;

/** The amethyst shard. It holds the player's domain expansion: right-click cycles, left-click casts (needs full chaos). */
public class DomainItem {
    private final ItemStack item;

    public DomainItem(BattlePlayer owner) {
        this.item = ItemStack.of(Material.AMETHYST_SHARD);

        Player bukkitPlayer = owner.getPlayer();
        String name = bukkitPlayer.getName() + "'s Domain";
        NamespacedKey key = new NamespacedKey(BattlePlugin.getInstance(), "domain-item");

        this.item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, name);
            meta.displayName(Component.text(name));
        });
    }

    public ItemStack getItem() {
        return this.item;
    }
}
