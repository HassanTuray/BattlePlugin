package com.hfturay.battleplugin.items;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

import net.kyori.adventure.text.Component;

/** The breeze rod. It holds the shikigami a player can summon: right-click cycles them, left-click summons. */
public class ShikigamiItem {
    private final ItemStack item;

    public ShikigamiItem(BattlePlayer owner) {
        this.item = ItemStack.of(Material.BREEZE_ROD);

        Player bukkitPlayer = owner.getPlayer();
        String name = bukkitPlayer.getName() + "'s Shikigami";
        NamespacedKey key = new NamespacedKey(BattlePlugin.getInstance(), "shikigami-item");

        this.item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, name);
            meta.displayName(Component.text(name));
        });
    }

    public ItemStack getItem() {
        return this.item;
    }
}
