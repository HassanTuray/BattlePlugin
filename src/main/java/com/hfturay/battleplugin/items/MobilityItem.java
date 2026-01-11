package com.hfturay.battleplugin.items;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.BattlePlugin;
import net.kyori.adventure.text.Component;

public class MobilityItem {
    private ItemStack item;
    private BattlePlayer owner;
    private String name;
    private NamespacedKey key;

    public MobilityItem(BattlePlayer owner) {
        this.item = ItemStack.of(Material.FEATHER);
        this.owner = owner;

        Player bukkitPlayer = this.owner.getPlayer();
        this.name = bukkitPlayer.getName() + "'s Mobility";

        this.key = new NamespacedKey(BattlePlugin.getInstance(), "mobility-item");

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
