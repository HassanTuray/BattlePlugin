package com.hfturay.battleplugin.clickservice;

import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.cooldownservice.SilenceLogger;
import com.hfturay.battleplugin.items.AbilityItemFactory;
import com.hfturay.battleplugin.loggerservice.PlayerLogger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public class ClickLogger implements Listener {
    private SilenceLogger silenceLogger = BattlePlugin.getInstance().getSilenceLogger();

    @EventHandler
    public void onPlayerClick(PlayerInteractEvent e) {
        Action action = e.getAction();
        BattlePlayer player = PlayerLogger.getBattlePlayer(e.getPlayer().getUniqueId());

        // need to handle if an entity is right or left clicked

        ItemStack item = e.getItem();
        if(!(AbilityItemFactory.isAttackItem(item) || AbilityItemFactory.isMobilityItem(item) || AbilityItemFactory.isBuffItem(item))) {
            return;
        }

        // may need to send over the type of click that happened in the AbilityContext
        switch(action) {
            case Action.LEFT_CLICK_AIR:
                if (silenceLogger.isSilenced(player)) {
                    player.getPlayer().sendActionBar(Component.text("You are silenced", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
                }

                if (item.getType() == Material.MAGMA_CREAM) {
                    BattlePlugin.getInstance().getLogger().info("Invoking attack ability");

                    player.getCurrentAttackAbility().activate();
                }

                if (item.getType() == Material.FEATHER) {
                    BattlePlugin.getInstance().getLogger().info("Invoking mobility ability");

                    player.getCurrentMobilityAbility().activate();
                }

                if (item.getType() == Material.GHAST_TEAR) {
                    BattlePlugin.getInstance().getLogger().info("Invoking buff ability");

                    player.getCurrentBuffAbility().activate();
                }

                break;
            case Action.LEFT_CLICK_BLOCK:
                if (silenceLogger.isSilenced(player)) {
                    player.getPlayer().sendActionBar(Component.text("You are silenced", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
                }

               if (item.getType() == Material.MAGMA_CREAM) {
                    BattlePlugin.getInstance().getLogger().info("Invoking attack ability");

                    player.getCurrentAttackAbility().activate();
                }

                if (item.getType() == Material.FEATHER) {
                    BattlePlugin.getInstance().getLogger().info("Invoking mobility ability");

                    player.getCurrentMobilityAbility().activate();
                }

                if (item.getType() == Material.GHAST_TEAR) {
                    BattlePlugin.getInstance().getLogger().info("Invoking buff ability");

                    player.getCurrentBuffAbility().activate();
                }
                
                break;
            case Action.RIGHT_CLICK_AIR:
                if (item.getType() == Material.MAGMA_CREAM) {
                    player.nextAttackAbility();

                    BattlePlugin.getInstance().getLogger().info("Changing attack Ability");
                }

                if (item.getType() == Material.FEATHER) {
                    player.nextMobilityAbility();

                    BattlePlugin.getInstance().getLogger().info("Changing mobility ability");
                }

                if (item.getType() == Material.GHAST_TEAR) {
                    player.nextBuffAbility();

                    BattlePlugin.getInstance().getLogger().info("Changing buff ability");
                }
                
                break;
            case Action.RIGHT_CLICK_BLOCK:
                if (item.getType() == Material.MAGMA_CREAM) {
                    player.nextAttackAbility();

                    BattlePlugin.getInstance().getLogger().info("Changing Ability");
                }

                if (item.getType() == Material.FEATHER) {
                    player.nextMobilityAbility();

                    BattlePlugin.getInstance().getLogger().info("Changing mobility ability");
                }

                if (item.getType() == Material.GHAST_TEAR) {
                    player.nextBuffAbility();

                    BattlePlugin.getInstance().getLogger().info("Changing buff ability");
                }
                break;
            case Action.PHYSICAL:
                break;
            default:
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true) 
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (!(damager instanceof Player)) {
            return;
        }

        Player player = (Player) damager;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return;
        }

        BattlePlayer battlePlayer = PlayerLogger.getBattlePlayer(player.getUniqueId());
        if (battlePlayer == null) {
            return;
        }

        if (silenceLogger.isSilenced(battlePlayer)) {
            player.getPlayer().sendActionBar(Component.text("You are silenced", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        if (item.getType() == Material.MAGMA_CREAM) {
            BattlePlugin.getInstance().getLogger().info("Invoking attack ability");

            battlePlayer.getCurrentAttackAbility().activate();
        }

        if (item.getType() == Material.FEATHER) {
            BattlePlugin.getInstance().getLogger().info("Invoking mobility ability");

            battlePlayer.getCurrentMobilityAbility().activate();
        }

        if (item.getType() == Material.GHAST_TEAR) {
            BattlePlugin.getInstance().getLogger().info("Invoking buff ability");

            battlePlayer.getCurrentBuffAbility().activate();
        }
    }
}
