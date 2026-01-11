package com.hfturay.battleplugin.items;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.BattlePlugin;
import net.kyori.adventure.text.Component;

public class BuffItem {
    private ItemStack item;
    private BattlePlayer owner;
    private String name;
    private NamespacedKey key;

    public BuffItem(BattlePlayer owner) {
        this.item = ItemStack.of(Material.GHAST_TEAR);
        this.owner = owner;

        Player bukkitPlayer = this.owner.getPlayer();
        this.name = bukkitPlayer.getName() + "'s Buff";

        this.key = new NamespacedKey(BattlePlugin.getInstance(), "buff-item");

        this.item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, this.name);
            meta.displayName(Component.text(this.name));
        });

        this.item.getItemMeta().displayName(Component.text(this.name));
    }

    public ItemStack getItem() {
        return this.item;
    }
}
