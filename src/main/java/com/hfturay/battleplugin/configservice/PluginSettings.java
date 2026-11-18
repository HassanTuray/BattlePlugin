package com.hfturay.battleplugin.configservice;

import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;

/**
 * The values in config.yml, checked. A missing or silly value falls back to a sensible one with a warning,
 * so a typo in the file cannot leave players with zero health.
 */
public final class PluginSettings {
    public static final double DEFAULT_BASE_HEALTH = 40;
    public static final int DEFAULT_FOOD_LEVEL = 16;

    private volatile double baseHealth = DEFAULT_BASE_HEALTH;
    private volatile int foodLevel = DEFAULT_FOOD_LEVEL;

    public double baseHealth() {
        return baseHealth;
    }

    public int foodLevel() {
        return foodLevel;
    }

    public void load(ConfigurationSection config, Logger logger) {
        baseHealth = readNumber(config, "player.base-health", DEFAULT_BASE_HEALTH, 2, 1024, logger);
        foodLevel = (int) readNumber(config, "player.food-level", DEFAULT_FOOD_LEVEL, 0, 20, logger);
    }

    private static double readNumber(ConfigurationSection config, String path, double fallback, double min, double max, Logger logger) {
        if (!config.isSet(path)) {
            return fallback;
        }
        if (!config.isInt(path) && !config.isDouble(path)) {
            logger.warning("config.yml: '" + path + "' is not a number. Using " + fallback + ".");
            return fallback;
        }
        double value = config.getDouble(path);
        if (Double.isNaN(value) || value < min || value > max) {
            logger.warning("config.yml: '" + path + "' must be between " + min + " and " + max + ". Using " + fallback + ".");
            return fallback;
        }
        return value;
    }
}
