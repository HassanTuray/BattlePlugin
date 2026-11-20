package com.hfturay.battleplugin.kitservice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.configuration.ConfigurationSection;

import com.hfturay.battleplugin.ability.AbilityRegistry;
import com.hfturay.battleplugin.ability.AbilitySlot;

/**
 * The contents of kits.yml, checked and ready to use. Plain data with no server in it.
 *
 * Nothing in the file can break joining: an unknown ability, a kit that does not exist, a bad value are
 * reported through the warning callback and skipped, and the rest still loads.
 */
public final class KitConfig {

    /** A named set of abilities per item, stored as registry keys. */
    public record Kit(String name, Map<AbilitySlot, List<String>> abilities) {
        public int size() {
            int total = 0;
            for (List<String> list : abilities.values()) {
                total += list.size();
            }
            return total;
        }
    }

    public enum Source { UUID, NAME, DEFAULT }

    public record Match(Kit kit, Source source) { }

    private final Map<String, Kit> kits;
    private final Map<UUID, String> byUuid;
    private final Map<String, String> byName;
    private final String defaultKit;

    private KitConfig(Map<String, Kit> kits, Map<UUID, String> byUuid, Map<String, String> byName, String defaultKit) {
        this.kits = kits;
        this.byUuid = byUuid;
        this.byName = byName;
        this.defaultKit = defaultKit;
    }

    public static KitConfig empty() {
        return new KitConfig(Map.of(), Map.of(), Map.of(), null);
    }

    public static KitConfig parse(ConfigurationSection root, Consumer<String> warn) {
        Map<String, Kit> kits = new LinkedHashMap<>();
        ConfigurationSection kitSection = root.getConfigurationSection("kits");
        if (kitSection != null) {
            for (String name : kitSection.getKeys(false)) {
                ConfigurationSection section = kitSection.getConfigurationSection(name);
                if (section == null) {
                    warn.accept("Kit '" + name + "' is not a list of items. Skipped.");
                    continue;
                }
                String key = name.toLowerCase(Locale.ROOT);
                if (kits.containsKey(key)) {
                    warn.accept("Kit '" + name + "' is listed twice. The first one is used.");
                    continue;
                }
                kits.put(key, parseKit(name, section, warn));
            }
        }

        Map<UUID, String> byUuid = new HashMap<>();
        Map<String, String> byName = new HashMap<>();
        ConfigurationSection players = root.getConfigurationSection("players");
        if (players != null) {
            for (String who : players.getKeys(false)) {
                String kitName = players.getString(who);
                if (kitName == null || !kits.containsKey(kitName.toLowerCase(Locale.ROOT))) {
                    warn.accept("Player '" + who + "' has kit '" + kitName + "', which does not exist. Skipped.");
                    continue;
                }
                String kitKey = kitName.toLowerCase(Locale.ROOT);

                UUID id = asUuid(who);
                if (id != null) {
                    if (byUuid.putIfAbsent(id, kitKey) != null) {
                        warn.accept("Player '" + who + "' is listed twice. The first one is used.");
                    }
                } else if (byName.putIfAbsent(who.toLowerCase(Locale.ROOT), kitKey) != null) {
                    warn.accept("Player '" + who + "' is listed twice. The first one is used.");
                }
            }
        }

        String defaultKit = null;
        String requested = root.getString("default");
        if (requested != null && !requested.isBlank() && !requested.equalsIgnoreCase("none")) {
            String key = requested.toLowerCase(Locale.ROOT);
            if (kits.containsKey(key)) {
                defaultKit = key;
            } else {
                warn.accept("The default kit '" + requested + "' does not exist. Players not listed get nothing.");
            }
        }

        return new KitConfig(Collections.unmodifiableMap(kits), Collections.unmodifiableMap(byUuid),
            Collections.unmodifiableMap(byName), defaultKit);
    }

    private static Kit parseKit(String name, ConfigurationSection section, Consumer<String> warn) {
        Map<AbilitySlot, List<String>> abilities = new EnumMap<>(AbilitySlot.class);
        List<String> seen = new ArrayList<>();

        for (AbilitySlot slot : AbilitySlot.values()) {
            List<String> keys = new ArrayList<>();
            for (String listed : section.getStringList(slot.name().toLowerCase(Locale.ROOT))) {
                // a bundle name (flyingraijin) stands for all of its parts
                List<AbilityRegistry.Entry> found = AbilityRegistry.findBundle(listed)
                    .orElseGet(() -> AbilityRegistry.find(listed).map(List::of).orElse(List.of()));
                if (found.isEmpty()) {
                    warn.accept("Kit '" + name + "': there is no ability called '" + listed + "'. Skipped.");
                    continue;
                }
                for (AbilityRegistry.Entry entry : found) {
                    if (entry.slot() != slot) {
                        warn.accept("Kit '" + name + "': '" + listed + "' belongs on the "
                            + entry.slot().name().toLowerCase(Locale.ROOT) + " item, not " + slot.name().toLowerCase(Locale.ROOT) + ". Skipped.");
                        continue;
                    }
                    if (seen.contains(entry.key())) {
                        continue;
                    }
                    seen.add(entry.key());
                    keys.add(entry.key());
                }
            }
            abilities.put(slot, Collections.unmodifiableList(keys));
        }
        return new Kit(name, Collections.unmodifiableMap(abilities));
    }

    private static UUID asUuid(String text) {
        if (text.length() != 36) {
            return null;
        }
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ---- lookups ----

    public Kit kit(String name) {
        return name == null ? null : kits.get(name.toLowerCase(Locale.ROOT));
    }

    public Map<String, Kit> kits() {
        return kits;
    }

    public Kit defaultKit() {
        return defaultKit == null ? null : kits.get(defaultKit);
    }

    public int listedPlayers() {
        return byUuid.size() + byName.size();
    }

    /** The kit a player should start with: by UUID, then by username, then the default. Null if there is none. */
    public Match forPlayer(UUID id, String username) {
        String viaUuid = id == null ? null : byUuid.get(id);
        if (viaUuid != null) {
            return new Match(kits.get(viaUuid), Source.UUID);
        }
        String viaName = username == null ? null : byName.get(username.toLowerCase(Locale.ROOT));
        if (viaName != null) {
            return new Match(kits.get(viaName), Source.NAME);
        }
        Kit fallback = defaultKit();
        return fallback == null ? null : new Match(fallback, Source.DEFAULT);
    }
}
