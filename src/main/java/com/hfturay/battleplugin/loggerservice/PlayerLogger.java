package com.hfturay.battleplugin.loggerservice;

import org.bukkit.event.Listener;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.items.AbilityItemFactory;

public class PlayerLogger implements Listener {
    private static HashMap<UUID, BattlePlayer> players = new HashMap<>();
    private final double BASE_HEALTH = 40;

    /*
     * Everytime a player joins a new battleplayer object is created
     * If they are a dup then they are not added to the hashmap
     * However, what happens to this dupe after has been created
     * Consider checking if the player is a dupe via uuid before wrapping then in a battleplayer
     */

     /*
      * Consider saving the battle player history in some file 
      */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player bukkitPlayer = e.getPlayer();
        bukkitPlayer.setFoodLevel(16);

        BattlePlugin.getInstance().getLogger().info("Someone joined");
        BattlePlugin.getInstance().getLogger().info(bukkitPlayer.getName() + ": " +  bukkitPlayer.getUniqueId().toString());

        if (!players.containsKey(bukkitPlayer.getUniqueId())) {
            BattlePlayer battlePlayer = new BattlePlayer(bukkitPlayer);
            players.put(bukkitPlayer.getUniqueId(), battlePlayer);
            giveAbilityItems(battlePlayer);

            BattlePlugin.getInstance().getLogger().info("Newbie");
        } else {
            giveAbilityItems(players.get(bukkitPlayer.getUniqueId()));
            BattlePlugin.getInstance().getLogger().info("Not their first rodeo");
        }

        AttributeInstance maxHealth = bukkitPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH);

        if (maxHealth != null) {
            maxHealth.setBaseValue(BASE_HEALTH);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player bukkitPlayer = e.getPlayer();
        bukkitPlayer.setFoodLevel(16);

        AttributeInstance maxHealth = bukkitPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH);

        if (maxHealth != null) {
            maxHealth.setBaseValue(BASE_HEALTH);
        }
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        DeathLogger.killShikigami(uuid);
        players.remove(uuid);

        BattlePlugin.getInstance().getLogger().info("removing you!");
    }

    private void giveAbilityItems(BattlePlayer battlePlayer) {
        Player bukkitPlayer = battlePlayer.getPlayer();
        PlayerInventory inv = bukkitPlayer.getInventory();

        boolean hasAttackItem = false;
        boolean hasBuffItem = false;
        boolean hasMobilityItem = false;

        for (ItemStack stack : inv.getContents()) {
            // should check which item they are individually 
            if (AbilityItemFactory.isAttackItem(stack)) {
                BattlePlugin.getInstance().getLogger().info("This player has an attack item");
                hasAttackItem = true;
            } else if (AbilityItemFactory.isMobilityItem(stack)) {
                BattlePlugin.getInstance().getLogger().info("This player has a mobility item");
                hasMobilityItem = true;
            } else if (AbilityItemFactory.isBuffItem(stack)) {
                BattlePlugin.getInstance().getLogger().info("This player has a buff item");
                hasBuffItem = true;
            }
        }
        
        if (!hasAttackItem) {
            ItemStack bukkitItem = battlePlayer.getAttackItem().getItem();
            inv.addItem(bukkitItem);
            BattlePlugin.getInstance().getLogger().info("Giving attack");
        }
        if (!hasBuffItem) {
            ItemStack bukkitItem = battlePlayer.getBuffItem().getItem();
            inv.addItem(bukkitItem);
            BattlePlugin.getInstance().getLogger().info("giving buff");
        }
        if (!hasMobilityItem) {
            ItemStack bukkitItem = battlePlayer.getMobilityItem().getItem();
            inv.addItem(bukkitItem);
            BattlePlugin.getInstance().getLogger().info("giving mobility");
        }
    }

    public static BattlePlayer getBattlePlayer(UUID uuid) {
        return players.get(uuid);
    }
}
