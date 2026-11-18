package com.hfturay.battleplugin.chaosservice;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

/**
 * Chaos: the energy bar filled by fighting. It is shown on the experience bar (level number = chaos, bar fill =
 * chaos out of the full amount, with a resource pack recolouring the bar red).
 *
 * Gained by dealing damage (per heart) and by killing a player; lost by dying. It may go above the full amount;
 * the bar then just stays full. Later, domain expansions will spend it through spend()/has().
 *
 * Because the experience bar is taken over, normal experience is switched off for everyone.
 */
public final class ChaosService implements Listener {
    private final Plugin plugin;
    private final File file;
    private final Logger logger;
    private final Map<UUID, Double> chaos = new HashMap<>();

    private double full = 100;
    private double perHeart = 1;
    private double killGain = 10;
    private double deathLoss = 15;
    private boolean countNonPlayerDamage = false;

    public ChaosService(Plugin plugin, File file) {
        this.plugin = plugin;
        this.file = file;
        this.logger = plugin.getLogger();
    }

    /** Reads the chaos section of config.yml. Silly values fall back to the defaults. */
    public void reload(ConfigurationSection config) {
        full = number(config, "chaos.full", 100, 1);
        perHeart = number(config, "chaos.per-heart", 1, 0);
        killGain = number(config, "chaos.kill-gain", 10, 0);
        deathLoss = number(config, "chaos.death-loss", 15, 0);
        countNonPlayerDamage = config.getBoolean("chaos.count-non-player-damage", false);
        for (Player player : Bukkit.getOnlinePlayers()) {
            show(player);
        }
    }

    private double number(ConfigurationSection config, String path, double fallback, double min) {
        if (!config.isSet(path)) {
            return fallback;
        }
        double value = config.getDouble(path, fallback);
        if ((!config.isInt(path) && !config.isDouble(path)) || Double.isNaN(value) || Double.isInfinite(value) || value < min) {
            logger.warning("config.yml: '" + path + "' must be a number of at least " + min + ". Using " + fallback + ".");
            return fallback;
        }
        return value;
    }

    // ---- the numbers ----

    public double get(UUID id) {
        return chaos.getOrDefault(id, 0.0);
    }

    public double full() {
        return full;
    }

    /** True if the player has at least this much chaos (for domain expansions later). */
    public boolean has(UUID id, double amount) {
        return get(id) >= amount;
    }

    /** Sets the amount (never below 0) and updates the bar. */
    public void set(UUID id, double amount) {
        double value = Double.isNaN(amount) ? 0 : Math.max(0, Math.min(amount, 1_000_000));
        chaos.put(id, value);
        Player player = Bukkit.getPlayer(id);
        if (player != null) {
            show(player);
        }
    }

    public void add(UUID id, double amount) {
        set(id, get(id) + amount);
    }

    /** Takes chaos if there is enough. Returns false (and takes nothing) if not. */
    public boolean spend(UUID id, double amount) {
        if (!has(id, amount)) {
            return false;
        }
        set(id, get(id) - amount);
        return true;
    }

    // ---- the bar ----

    /** Puts the chaos on the experience bar. */
    public void show(Player player) {
        double value = get(player.getUniqueId());
        player.setLevel((int) Math.floor(value));
        player.setExp((float) Math.min(1.0, value / full));
    }

    // ---- saving ----

    public void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            try {
                chaos.put(UUID.fromString(key), Math.max(0, yaml.getDouble(key)));
            } catch (IllegalArgumentException ignored) {
                logger.warning(file.getName() + ": '" + key + "' is not a player id. Skipped.");
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        chaos.forEach((id, value) -> {
            if (value > 0) {
                yaml.set(id.toString(), value);
            }
        });
        try {
            yaml.save(file);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not save " + file.getName(), e);
        }
    }

    // ---- events ----

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = attackerOf(event.getDamager());
        if (attacker == null || attacker.equals(event.getEntity())) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        if (!(victim instanceof Player) && !countNonPlayerDamage) {
            return;
        }
        // No credit for overkill: at most the health the victim had left.
        double dealt = Math.min(event.getFinalDamage(), victim.getHealth());
        if (dealt > 0) {
            add(attacker.getUniqueId(), dealt / 2.0 * perHeart);     // 2 health = 1 heart
        }
    }

    private static Player attackerOf(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Player killer = dead.getKiller();
        if (killer != null && !killer.equals(dead)) {
            add(killer.getUniqueId(), killGain);
        }
        add(dead.getUniqueId(), -deathLoss);
        event.setKeepLevel(false);
        event.setDroppedExp(0);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        // Respawning resets experience, so put the chaos back a tick later.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                show(player);
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        show(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        save();
    }

    /** Experience orbs and the like must not disturb the bar. */
    @EventHandler
    public void onExp(PlayerExpChangeEvent event) {
        event.setAmount(0);
    }
}
