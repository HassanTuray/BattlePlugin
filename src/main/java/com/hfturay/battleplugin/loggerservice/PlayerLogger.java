package com.hfturay.battleplugin.loggerservice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.items.AbilityItemFactory;

/**
 * Keeps the ability data (BattlePlayer) for everyone online.
 *
 * register() is the one way a player gets set up: it is used when they join, and for everyone already online when
 * the plugin starts, so /reload no longer leaves online players without ability data.
 */
public class PlayerLogger implements Listener {
    private static final HashMap<UUID, BattlePlayer> players = new HashMap<>();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        register(e.getPlayer());
    }

    /**
     * Sets a player up: ability data (saved loadout, or their kit), their three items, health and food.
     * Safe to call for someone who is already set up; their loadout is left alone then.
     */
    public static BattlePlayer register(Player bukkitPlayer) {
        BattlePlugin plugin = BattlePlugin.getInstance();
        UUID id = bukkitPlayer.getUniqueId();

        BattlePlayer battlePlayer = players.get(id);
        if (battlePlayer == null) {
            battlePlayer = new BattlePlayer(bukkitPlayer);
            try {
                plugin.getKitService().setUp(battlePlayer, bukkitPlayer.getName(), plugin.getAbilityFactory()::create);
            } catch (RuntimeException ex) {
                // A bad loadout must never stop someone joining; they keep whatever loaded.
                plugin.getLogger().log(Level.WARNING, "Could not set up the abilities for " + bukkitPlayer.getName(), ex);
            }
            players.put(id, battlePlayer);
        }

        applyVitals(bukkitPlayer);
        giveAbilityItems(battlePlayer);
        return battlePlayer;
    }

    /** Sets up everyone already online. Used when the plugin starts or is reloaded. Returns how many were new. */
    public static int registerOnline() {
        int added = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean isNew = !players.containsKey(player.getUniqueId());
            register(player);
            if (isNew) {
                added++;
            }
        }
        return added;
    }

    private static void applyVitals(Player bukkitPlayer) {
        BattlePlugin plugin = BattlePlugin.getInstance();
        bukkitPlayer.setFoodLevel(plugin.getSettings().foodLevel());

        AttributeInstance maxHealth = bukkitPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(plugin.getSettings().baseHealth());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player bukkitPlayer = e.getPlayer();
        applyVitals(bukkitPlayer);

        // Ability items do not drop on death, so hand them back (a tick later, once the respawn has finished).
        BattlePlugin plugin = BattlePlugin.getInstance();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            BattlePlayer battlePlayer = players.get(bukkitPlayer.getUniqueId());
            if (battlePlayer != null && bukkitPlayer.isOnline()) {
                giveAbilityItems(battlePlayer);
            }
        });
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        BattlePlayer leaving = players.get(uuid);
        if (leaving != null) {
            BattlePlugin.getInstance().getLoadoutStore().save(uuid, leaving.getLoadout());
        }
        BattlePlugin.getInstance().getShikigamiService().killOwnedBy(uuid);
        BattlePlugin.getInstance().getInputService().forget(uuid);
        BattlePlugin.getInstance().getCooldownService().prune(uuid);
        BattlePlugin.getInstance().getAbilityScheduler().cancelAll(uuid);
        BattlePlugin.getInstance().getProjectileService().cancelAll(uuid);
        BattlePlugin.getInstance().getStunService().clear(uuid);
        BattlePlugin.getInstance().getImmunityService().clearAll(uuid);
        BattlePlugin.getInstance().getFallImmunityService().clearFallImmunity(uuid);
        BattlePlugin.getInstance().getParticleService().cancelAll(uuid);
        BattlePlugin.getInstance().getVisualService().cancelAll(uuid);
        players.remove(uuid);

    }

    private static void giveAbilityItems(BattlePlayer battlePlayer) {
        Player bukkitPlayer = battlePlayer.getPlayer();
        PlayerInventory inv = bukkitPlayer.getInventory();

        boolean hasAttackItem = false;
        boolean hasBuffItem = false;
        boolean hasMobilityItem = false;
        boolean hasElementalItem = false;
        boolean hasShikigamiItem = false;

        for (ItemStack stack : inv.getContents()) {
            if (AbilityItemFactory.isAttackItem(stack)) {
                hasAttackItem = true;
            } else if (AbilityItemFactory.isMobilityItem(stack)) {
                hasMobilityItem = true;
            } else if (AbilityItemFactory.isBuffItem(stack)) {
                hasBuffItem = true;
            } else if (AbilityItemFactory.isElementalItem(stack)) {
                hasElementalItem = true;
            } else if (AbilityItemFactory.isShikigamiItem(stack)) {
                hasShikigamiItem = true;
            }
        }

        if (!hasAttackItem) {
            inv.addItem(battlePlayer.getAttackItem().getItem());
        }
        if (!hasBuffItem) {
            inv.addItem(battlePlayer.getBuffItem().getItem());
        }
        if (!hasMobilityItem) {
            inv.addItem(battlePlayer.getMobilityItem().getItem());
        }
        if (!hasElementalItem) {
            inv.addItem(battlePlayer.getElementalItem().getItem());
        }
        if (!hasShikigamiItem) {
            inv.addItem(battlePlayer.getShikigamiItem().getItem());
        }
    }

    /** Everyone currently online with ability data. */
    public static Collection<BattlePlayer> getAll() {
        return new ArrayList<>(players.values());
    }

    /** Forgets everyone. Used when the plugin shuts down. */
    public static void clear() {
        players.clear();
    }

    public static BattlePlayer getBattlePlayer(UUID uuid) {
        return players.get(uuid);
    }
}
