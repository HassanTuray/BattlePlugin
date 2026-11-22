package com.hfturay.battleplugin.configservice;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import com.hfturay.battleplugin.ability.AbilityRegistry;
import com.hfturay.battleplugin.ability.RequiresSettings;

/**
 * The numbers in abilities.yml (speed, damage, cooldown ...), so they can be changed without recompiling.
 *
 * Abilities ask for a number when they are used, with the value in the code as the fallback. Anything not in
 * the file, or not a sensible number, falls back to the code value. So an empty or broken file changes nothing,
 * and a reload takes effect the next time an ability is used (even for abilities players already hold).
 */
public final class AbilityTuning {

    /** The settings an ability may have. A name outside this list is reported as a probable typo. */
    public static final Set<String> KNOWN_KEYS = Set.of(
        "cooldown", "speed", "max-ticks", "hitbox", "radius", "damage", "horizontal-force", "lift",
        "range", "duration-ticks", "amplifier",
        // projectiles and blasts
        "gravity", "knockback", "interval-ticks", "effect-ticks", "x-bound", "y-bound", "z-bound",
        // potion levels
        "slow-amplifier", "poison-amplifier", "wither-amplifier", "blindness-amplifier", "night-vision-amplifier",
        "strength-amplifier", "resistance-amplifier", "regeneration-amplifier",
        // Anthrax zombies
        "zombie-count", "zombie-health", "zombie-lifetime-ticks", "zombie-resistance",
        // Revolutionary Spirit
        "health-threshold",
        // Earth Dome
        "thickness",
        // Blast Off
        "launch-lift",
        // Generic, Damon and Nathan kits
        "pitch-lift", "min-horizontal-multiplier", "min-vertical-multiplier", "regeneration-ticks",
        "damage-multiplier", "combo-ticks", "slam-force", "stun-ticks", "homing-distance", "turn-strength",
        "homing-damage-multiplier", "catch-radius", "damage-bonus", "jump-amplifier", "walk-speed", "end-health",
        // Shikigami
        "health", "attack-range", "attack-interval-ticks", "target-range", "follow-distance", "leash-distance",
        "lifetime-ticks", "idle-ticks", "max-active", "hit-memory-ticks", "flame-interval-ticks", "attack-hit-delay-ticks",
        "bomb-interval-ticks", "bomb-speed", "bomb-gravity", "bomb-hitbox", "bomb-max-ticks", "bomb-radius", "bomb-damage", "bomb-knockback",
        "grow-ticks",
        // Domain expansions
        "dash-speed", "dash-delay-ticks", "hover-height", "hover-behind", "learn-damage", "learn-window-ticks", "scan-ticks", "scan-distance", "chaos-cost", "form-ticks", "tower-height", "flame-height", "endermen-count", "phantom-count", "warden-count",
        "mobs-target-caster", "charge-ticks",
        // Explosive Gravity Bomb
        "pull-radius", "pull-force", "start-radius", "end-radius", "blast-radius",
        // Shilajit
        "heal",
        // Joga Bonito
        "return-speed",
        // Air Dome
        "speed-amplifier", "push-strength", "push-lift", "damage-interval-ticks",
        // Majestic Destroyer Flame
        "width", "height", "fire-ticks", "smoke-count",
        // Water Barrage
        "count", "turn-rate",
        // Water Prison
        "cube-size", "pop-ticks", "pop-lift", "trail-ticks",
        // Meltdown of Waves
        "silence-ticks", "front-offset", "back-reach", "pull-gain", "max-push-speed");

    /** Result of reading the file. A failed read keeps the numbers that were already loaded. */
    public record LoadResult(boolean ok, int abilities, int values, List<String> warnings, String error) { }

    private final File file;
    private final Logger logger;
    // ability (normalized name) -> setting -> value. Replaced as a whole on reload, never edited in place.
    private volatile Map<String, Map<String, Double>> values = Map.of();

    public AbilityTuning(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    /** True if the file gives this ability that setting (as a valid number). */
    public boolean has(String abilityName, String key) {
        Map<String, Double> forAbility = values.get(AbilityRegistry.normalize(abilityName));
        return forAbility != null && forAbility.containsKey(key);
    }

    /** Settings that abilities with @RequiresSettings need but the file does not give them. */
    public static List<String> missingRequired(Map<String, Map<String, Double>> parsed) {
        List<String> problems = new ArrayList<>();
        for (AbilityRegistry.Entry entry : AbilityRegistry.all()) {
            RequiresSettings required = entry.type().getAnnotation(RequiresSettings.class);
            if (required == null) {
                continue;
            }
            Map<String, Double> given = parsed.getOrDefault(entry.key(), Map.of());
            for (String key : required.value()) {
                if (!given.containsKey(key)) {
                    problems.add(entry.type().getSimpleName() + " needs '" + key + "' in abilities.yml, so it will not run until it is added.");
                }
            }
        }
        return problems;
    }

    /** The tuned value for an ability's setting, or the fallback if the file does not set it. */
    public double number(String abilityName, String key, double fallback) {
        Map<String, Double> forAbility = values.get(AbilityRegistry.normalize(abilityName));
        if (forAbility == null) {
            return fallback;
        }
        Double value = forAbility.get(key);
        return value == null ? fallback : value;
    }

    /** (Re)reads abilities.yml. If the file cannot be read, the previous numbers stay in use. */
    public LoadResult reload() {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException | RuntimeException e) {
            logger.log(Level.WARNING, "Could not read " + file.getName() + ". The previous numbers stay in use.", e);
            return new LoadResult(false, values.size(), count(values), List.of(), String.valueOf(e.getMessage()));
        }

        List<String> warnings = new ArrayList<>();
        Map<String, Map<String, Double>> parsed = parse(yaml, warnings);
        warnings.addAll(missingRequired(parsed));
        for (String warning : warnings) {
            logger.warning(file.getName() + ": " + warning);
        }
        values = parsed;
        return new LoadResult(true, parsed.size(), count(parsed), warnings, null);
    }

    /** Turns the file contents into numbers. Public so it can be checked without a server. */
    public static Map<String, Map<String, Double>> parse(ConfigurationSection root, List<String> warnings) {
        Map<String, Map<String, Double>> result = new HashMap<>();

        for (String abilityName : root.getKeys(false)) {
            if (AbilityRegistry.find(abilityName).isEmpty()) {
                warnings.add("'" + abilityName + "' is not an ability. Skipped.");
                continue;
            }
            ConfigurationSection section = root.getConfigurationSection(abilityName);
            if (section == null) {
                warnings.add("'" + abilityName + "' should be a list of settings. Skipped.");
                continue;
            }

            Map<String, Double> settings = new HashMap<>();
            for (String key : section.getKeys(false)) {
                if (!KNOWN_KEYS.contains(key)) {
                    warnings.add(abilityName + ": unknown setting '" + key + "' (known: " + String.join(", ", new java.util.TreeSet<>(KNOWN_KEYS)) + "). Skipped.");
                    continue;
                }
                if (!section.isInt(key) && !section.isDouble(key)) {
                    warnings.add(abilityName + "." + key + " is not a number. Using the built-in value.");
                    continue;
                }
                double value = section.getDouble(key);
                if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
                    warnings.add(abilityName + "." + key + " must be a number that is not negative. Using the built-in value.");
                    continue;
                }
                settings.put(key, value);
            }

            // Listing an ability twice in different spellings: the later one wins, but say so.
            String id = AbilityRegistry.find(abilityName).get().key();
            if (result.containsKey(id)) {
                warnings.add("'" + abilityName + "' is listed twice. The later entry wins.");
            }
            result.put(id, settings);
        }
        return result;
    }

    private static int count(Map<String, Map<String, Double>> map) {
        int total = 0;
        for (Map<String, Double> settings : map.values()) {
            total += settings.size();
        }
        return total;
    }
}
