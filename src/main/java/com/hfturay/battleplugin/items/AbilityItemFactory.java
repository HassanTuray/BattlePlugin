package com.hfturay.battleplugin.items;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

public class AbilityItemFactory {
    ItemStack item;
    String name;
    BattlePlayer owner; // be careful I seem to override some stuff here
    NamespacedKey key;

    public static boolean isAttackItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        NamespacedKey attackItemKey = new NamespacedKey(BattlePlugin.getInstance(), "attack-item");

        return meta.getPersistentDataContainer().has(attackItemKey, PersistentDataType.STRING);
    } 

    public static boolean isMobilityItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        NamespacedKey mobilityItemKey = new NamespacedKey(BattlePlugin.getInstance(), "mobility-item");

        return meta.getPersistentDataContainer().has(mobilityItemKey, PersistentDataType.STRING);
    }

    public static boolean isBuffItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        NamespacedKey buffItemKey = new NamespacedKey(BattlePlugin.getInstance(), "buff-item");

        return meta.getPersistentDataContainer().has(buffItemKey, PersistentDataType.STRING);
    }
}

