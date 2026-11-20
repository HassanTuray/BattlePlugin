package com.hfturay.battleplugin.loadoutservice;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

/**
 * Remembers what each player has on their items between joins, so a /load or /remove sticks.
 *
 * One small file per player, plugins/BattlePlugin/playerdata/<uuid>.yml:
 *
 *   attack: [brick, homebrew]
 *   mobility: [leap]
 *   buff: []
 *
 * Abilities are stored by their registry name, in item order. A player with no file yet gets their default kit
 * (nothing is saved until they leave, or until a command changes their loadout). An empty list is saved as is,
 * so clearing a player's abilities also sticks. Names that no longer exist are skipped with a warning.
 */
public final class LoadoutStore {
    private final File folder;
    private final Logger logger;

    public LoadoutStore(File folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    public File fileFor(UUID id) {
        return new File(folder, id + ".yml");
    }

    public boolean hasSaved(UUID id) {
        return fileFor(id).isFile();
    }

    /** Writes the loadout to disk. Failures are logged, never thrown: losing a save must not break a quit or a command. */
    public boolean save(UUID id, AbilityLoadout loadout) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("version", 1);

        for (AbilitySlot slot : AbilitySlot.values()) {
            List<String> keys = new ArrayList<>();
            for (Ability ability : loadout.list(slot)) {
                if (ability.isTemporary()) {
                    continue;       // lent for a moment, never remembered
                }
                Optional<AbilityRegistry.Entry> entry = AbilityRegistry.of(ability.getClass());
                if (entry.isPresent()) {
                    keys.add(entry.get().key());
                } else {
                    logger.warning("Not saving " + ability.getClass().getSimpleName() + ": it is not in the AbilityRegistry");
                }
            }
            config.set(slot.name().toLowerCase(), keys);
        }

        File target = fileFor(id);
        File temp = new File(folder, id + ".yml.tmp");
        try {
            Files.createDirectories(folder.toPath());
            config.save(temp);                                  // write fully first, so a crash never leaves half a file
            try {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | RuntimeException e) {
            logger.log(Level.WARNING, "Could not save the loadout for " + id, e);
            return false;
        }
    }

    /** The saved ability names per item, or null if there is no usable file. */
    public Map<AbilitySlot, List<String>> read(UUID id) {
        File file = fileFor(id);
        if (!file.isFile()) {
            return null;
        }

        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (IOException | InvalidConfigurationException | RuntimeException e) {
            logger.log(Level.WARNING, "Could not read the saved loadout for " + id + ". The default kit is used instead.", e);
            return null;
        }

        Map<AbilitySlot, List<String>> saved = new EnumMap<>(AbilitySlot.class);
        for (AbilitySlot slot : AbilitySlot.values()) {
            saved.put(slot, config.getStringList(slot.name().toLowerCase()));
        }
        return saved;
    }

    /**
     * Replaces the player's loadout with the saved one. Returns how many abilities were loaded, or -1 if there
     * was nothing saved (the player keeps whatever they have, normally the default kit).
     */
    public int restore(BattlePlayer owner, BiFunction<Class<? extends Ability>, BattlePlayer, Ability> creator) {
        UUID id = owner.getPlayer().getUniqueId();
        Map<AbilitySlot, List<String>> saved = read(id);
        if (saved == null) {
            return -1;
        }

        AbilityLoadout loadout = owner.getLoadout();
        loadout.clear();

        int loaded = 0;
        for (AbilitySlot slot : AbilitySlot.values()) {
            for (String name : saved.get(slot)) {
                Optional<AbilityRegistry.Entry> entry = AbilityRegistry.find(name);
                if (entry.isEmpty()) {
                    logger.warning("Saved loadout for " + id + " mentions '" + name + "', which no longer exists. Skipped.");
                    continue;
                }
                try {
                    if (loadout.add(slot, creator.apply(entry.get().type(), owner))) {
                        loaded++;
                    }
                } catch (RuntimeException e) {
                    logger.log(Level.WARNING, "Could not load '" + name + "' for " + id, e);
                }
            }
        }
        return loaded;
    }
}
