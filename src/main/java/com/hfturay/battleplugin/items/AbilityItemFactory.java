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

    public static boolean isElementalItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        NamespacedKey elementalItemKey = new NamespacedKey(BattlePlugin.getInstance(), "elemental-item");

        return meta.getPersistentDataContainer().has(elementalItemKey, PersistentDataType.STRING);
    }

    public static boolean isShikigamiItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        NamespacedKey shikigamiItemKey = new NamespacedKey(BattlePlugin.getInstance(), "shikigami-item");

        return meta.getPersistentDataContainer().has(shikigamiItemKey, PersistentDataType.STRING);
    }

    public static boolean isDomainItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        NamespacedKey domainItemKey = new NamespacedKey(BattlePlugin.getInstance(), "domain-item");

        return meta.getPersistentDataContainer().has(domainItemKey, PersistentDataType.STRING);
    }

    /** True for any of the six ability items. */
    public static boolean isAbilityItem(ItemStack item) {
        return isAttackItem(item) || isMobilityItem(item) || isBuffItem(item) || isElementalItem(item)
            || isShikigamiItem(item) || isDomainItem(item);
    }
}
