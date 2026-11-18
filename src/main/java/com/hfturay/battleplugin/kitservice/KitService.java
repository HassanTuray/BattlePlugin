package com.hfturay.battleplugin.kitservice;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.AbilityRegistry;
import com.hfturay.battleplugin.ability.AbilitySlot;
import com.hfturay.battleplugin.battleplayer.AbilityLoadout;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.kitservice.KitConfig.Kit;
import com.hfturay.battleplugin.kitservice.KitConfig.Match;
import com.hfturay.battleplugin.loadoutservice.LoadoutStore;

/**
 * Decides what a player starts with, from kits.yml.
 *
 *   saved loadout  ->  kit for their UUID  ->  kit for their username  ->  default kit  ->  nothing
 *
 * A player who matched a kit by UUID or username is saved straight away, so they are registered under their real
 * UUID from then on. The default kit is not saved at that point, so adding someone to kits.yml after they first
 * joined still works once you run "/kits apply" for them.
 */
public final class KitService {

    public enum Source { SAVED, UUID, NAME, DEFAULT, NONE }

    /** Result of reading the file. A failed read keeps the kits that were already loaded. */
    public record LoadResult(boolean ok, int kits, int players, List<String> warnings, String error) { }

    private final File file;
    private final LoadoutStore store;
    private final Logger logger;
    private volatile KitConfig config = KitConfig.empty();

    public KitService(File file, LoadoutStore store, Logger logger) {
        this.file = file;
        this.store = store;
        this.logger = logger;
    }

    public KitConfig config() {
        return config;
    }

    /** (Re)reads kits.yml. If the file cannot be read, the previous kits stay in use. */
    public LoadResult reload() {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException | RuntimeException e) {
            logger.log(Level.WARNING, "Could not read " + file.getName() + ". The previous kits stay in use.", e);
            return new LoadResult(false, config.kits().size(), config.listedPlayers(), List.of(), String.valueOf(e.getMessage()));
        }

        List<String> warnings = new ArrayList<>();
        KitConfig parsed = KitConfig.parse(yaml, warnings::add);
        for (String warning : warnings) {
            logger.warning(file.getName() + ": " + warning);
        }
        config = parsed;
        return new LoadResult(true, parsed.kits().size(), parsed.listedPlayers(), warnings, null);
    }

    /**
     * Gives a player who just joined their abilities. Returns where they came from.
     *
     * @param username the name they joined with, used to match kits.yml when their UUID is not listed
     */
    public Source setUp(BattlePlayer player, String username, BiFunction<Class<? extends Ability>, BattlePlayer, Ability> creator) {
        UUID id = player.getPlayer().getUniqueId();

        if (store.restore(player, creator) >= 0) {
            return Source.SAVED;
        }

        Match match = config.forPlayer(id, username);
        if (match == null) {
            return Source.NONE;
        }

        apply(match.kit(), player, creator);

        if (match.source() == KitConfig.Source.DEFAULT) {
            return Source.DEFAULT;
        }
        store.save(id, player.getLoadout());
        return match.source() == KitConfig.Source.UUID ? Source.UUID : Source.NAME;
    }

    /** The kit this player is supposed to have right now, ignoring anything saved. Null if there is none. */
    public Match resolve(UUID id, String username) {
        return config.forPlayer(id, username);
    }

    /** Replaces everything the player has with the kit. Returns how many abilities were loaded. */
    public int apply(Kit kit, BattlePlayer player, BiFunction<Class<? extends Ability>, BattlePlayer, Ability> creator) {
        AbilityLoadout loadout = player.getLoadout();
        loadout.clear();

        int loaded = 0;
        for (AbilitySlot slot : AbilitySlot.values()) {
            for (String key : kit.abilities().get(slot)) {
                var entry = AbilityRegistry.find(key);
                if (entry.isEmpty()) {
                    continue;
                }
                try {
                    if (loadout.add(slot, creator.apply(entry.get().type(), player))) {
                        loaded++;
                    }
                } catch (RuntimeException e) {
                    logger.log(Level.WARNING, "Could not give '" + key + "' from kit '" + kit.name() + "'", e);
                }
            }
        }
        return loaded;
    }
}
