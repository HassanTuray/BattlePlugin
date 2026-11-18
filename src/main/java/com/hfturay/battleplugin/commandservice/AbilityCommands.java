package com.hfturay.battleplugin.commandservice;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.AbilityRegistry;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.cooldownservice.CooldownService;
import com.hfturay.battleplugin.loggerservice.PlayerLogger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Testing commands for loading and removing abilities.
 *
 *   /load [skill] [player]          put a skill on the item it belongs to (any number per item)
 *   /load [player1] [player2]       give player2 every ability player1 has
 *   /remove [skill] [player]        take a skill off a player
 *   /remove [player1] [player2]     take off player2 every ability that player1 has
 *   /clearabilities [player]        take off everything
 *   /clearcooldowns [player]        make every ability usable right now (no player = yourself)
 *
 * For /load and /remove the first word is read as a skill if it is one, otherwise as a player.
 * "me" stands for yourself in any player slot. Skill names ignore case, spaces and punctuation.
 */
public final class AbilityCommands implements CommandExecutor, TabCompleter {

    /** Finds an online player's battle data. Returns null if there is no such player. */
    @FunctionalInterface
    public interface PlayerResolver {
        /** @throws IllegalArgumentException with a message for the sender if the player exists but cannot be used */
        BattlePlayer resolve(String name, CommandSender sender);
    }

    @FunctionalInterface
    public interface AbilityCreator {
        Ability create(Class<? extends Ability> type, BattlePlayer owner);
    }

    private static final String LOAD_USAGE = "/load <skill> <player>   or   /load <fromPlayer> <toPlayer>";
    private static final String REMOVE_USAGE = "/remove <skill> <player>   or   /remove <player1> <player2>";
    private static final String CLEAR_USAGE = "/clearabilities <player>";

    private final PlayerResolver players;
    private final AbilityCreator creator;
    private final CooldownService cooldowns;
    private final Supplier<List<String>> onlineNames;
    private final Consumer<BattlePlayer> afterChange;
    private final Logger logger;

    public AbilityCommands(PlayerResolver players, AbilityCreator creator, CooldownService cooldowns,
                           Supplier<List<String>> onlineNames, Consumer<BattlePlayer> afterChange, Logger logger) {
        this.players = players;
        this.creator = creator;
        this.cooldowns = cooldowns;
        this.onlineNames = onlineNames;
        this.afterChange = afterChange;
        this.logger = logger;
    }

    /** The real thing, wired to the running server. */
    public static AbilityCommands forServer(BattlePlugin plugin) {
        PlayerResolver resolver = (name, sender) -> {
            Player player = name.equalsIgnoreCase("me") && sender instanceof Player self
                ? self
                : Bukkit.getPlayerExact(name);

            if (player == null) {
                return null;
            }

            BattlePlayer battlePlayer = PlayerLogger.getBattlePlayer(player.getUniqueId());
            if (battlePlayer == null) {
                throw new IllegalArgumentException(player.getName() + " is online but has no ability data yet. Have them rejoin.");
            }
            return battlePlayer;
        };

        return new AbilityCommands(
            resolver,
            plugin.getAbilityFactory()::create,
            plugin.getCooldownService(),
            () -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),
            plugin::onLoadoutChanged,
            plugin.getLogger());
    }

    // ---- entry point ----

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            switch (command.getName().toLowerCase(Locale.ROOT)) {
                case "load" -> load(sender, args);
                case "remove" -> remove(sender, args);
                case "clearabilities" -> clear(sender, args);
                case "clearcooldowns" -> clearCooldowns(sender, args);
                default -> {
                    return false;
                }
            }
        } catch (IllegalArgumentException e) {
            error(sender, e.getMessage());
        } catch (RuntimeException e) {
            error(sender, "Something went wrong (" + e + "). See the console.");
            logger.log(Level.WARNING, "/" + command.getName() + " failed", e);
        }
        return true;
    }

    // ---- /load ----

    private void load(CommandSender sender, String[] args) {
        if (args.length != 2) {
            usage(sender, LOAD_USAGE);
            return;
        }

        Optional<AbilityRegistry.Entry> skill = AbilityRegistry.find(args[0]);
        if (skill.isPresent()) {
            loadSkill(sender, skill.get(), requirePlayer(sender, args[1]));
            return;
        }

        BattlePlayer from = players.resolve(args[0], sender);
        if (from == null) {
            error(sender, "'" + args[0] + "' is not a skill or an online player. Press Tab to see the skills.");
            return;
        }

        BattlePlayer to = requirePlayer(sender, args[1]);
        if (from == to) {
            error(sender, "That is the same player.");
            return;
        }

        int total = from.getLoadout().total();
        int added = to.getLoadout().addMissingFrom(from.getLoadout(), (slot, type) -> creator.create(type, to));
        afterChange.accept(to);

        ok(sender, "Copied " + added + " abilities from " + name(from) + " to " + name(to)
            + (total - added > 0 ? " (" + (total - added) + " they already had)" : "") + ".");
        if (added > 0) {
            tell(sender, to, "You were given " + added + " abilities from " + name(from) + "'s kit.");
        }
    }

    private void loadSkill(CommandSender sender, AbilityRegistry.Entry skill, BattlePlayer target) {
        Ability existing = target.getLoadout().find(skill.type());
        if (existing != null) {
            error(sender, name(target) + " already has " + existing.getDisplayName() + ".");
            return;
        }

        Ability ability = creator.create(skill.type(), target);
        target.getLoadout().add(skill.slot(), ability);
        afterChange.accept(target);

        ok(sender, "Loaded " + ability.getDisplayName() + " onto " + name(target) + "'s " + skill.slot().label()
            + " item (" + target.getLoadout().size(skill.slot()) + " " + skill.slot().label() + " abilities now).");
        tell(sender, target, "You were given " + ability.getDisplayName() + " on your " + skill.slot().label() + " item.");
    }

    // ---- /remove ----

    private void remove(CommandSender sender, String[] args) {
        if (args.length != 2) {
            usage(sender, REMOVE_USAGE);
            return;
        }

        Optional<AbilityRegistry.Entry> skill = AbilityRegistry.find(args[0]);
        if (skill.isPresent()) {
            BattlePlayer target = requirePlayer(sender, args[1]);
            List<Ability> removed = target.getLoadout().remove(skill.get().type());

            if (removed.isEmpty()) {
                error(sender, name(target) + " does not have " + skill.get().key() + ".");
                return;
            }

            afterChange.accept(target);
            String display = removed.get(0).getDisplayName();
            ok(sender, "Removed " + display + " from " + name(target) + ".");
            tell(sender, target, display + " was removed from your abilities.");
            return;
        }

        BattlePlayer from = players.resolve(args[0], sender);
        if (from == null) {
            error(sender, "'" + args[0] + "' is not a skill or an online player. Press Tab to see the skills.");
            return;
        }

        BattlePlayer target = requirePlayer(sender, args[1]);
        int removed = target.getLoadout().removeCommonWith(from.getLoadout());
        afterChange.accept(target);

        ok(sender, "Removed " + removed + " abilities from " + name(target) + " that " + name(from) + " has.");
        if (removed > 0) {
            tell(sender, target, removed + " abilities were removed from your kit.");
        }
    }

    // ---- /clearabilities ----

    private void clear(CommandSender sender, String[] args) {
        if (args.length != 1) {
            usage(sender, CLEAR_USAGE);
            return;
        }

        BattlePlayer target = requirePlayer(sender, args[0]);
        int removed = target.getLoadout().clear();
        afterChange.accept(target);

        ok(sender, "Cleared " + removed + " abilities from " + name(target) + ".");
        if (removed > 0) {
            tell(sender, target, "All of your abilities were cleared.");
        }
    }

    // ---- /clearcooldowns ----

    private void clearCooldowns(CommandSender sender, String[] args) {
        if (args.length > 1) {
            usage(sender, "/clearcooldowns [player]   (no player = yourself)");
            return;
        }

        BattlePlayer target = requirePlayer(sender, args.length == 0 ? "me" : args[0]);
        int running = cooldowns.resetAll(target.getPlayer().getUniqueId());

        ok(sender, running == 0
            ? name(target) + " had no cooldowns running."
            : "Reset " + running + " cooldowns for " + name(target) + ".");
        if (running > 0) {
            tell(sender, target, "Your cooldowns were reset.");
        }
    }

    // ---- tab completion ----

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        boolean takesSkill = name.equals("load") || name.equals("remove");

        List<String> options = new ArrayList<>();
        boolean offerPlayers = false;

        if (args.length == 1) {
            if (takesSkill) {
                options.addAll(AbilityRegistry.keys());
            }
            offerPlayers = true;
        } else if (args.length == 2 && takesSkill) {
            offerPlayers = true;
        }

        if (offerPlayers) {
            options.addAll(onlineNames.get());
            options.add("me");
        }

        String typed = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream()
            .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(typed))
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    // ---- helpers ----

    private BattlePlayer requirePlayer(CommandSender sender, String name) {
        BattlePlayer player = players.resolve(name, sender);
        if (player == null) {
            throw new IllegalArgumentException("Player '" + name + "' is not online.");
        }
        return player;
    }

    private static String name(BattlePlayer player) {
        return player.getPlayer().getName();
    }

    private static void usage(CommandSender sender, String usage) {
        sender.sendMessage(Component.text("Usage: " + usage, NamedTextColor.YELLOW));
    }

    private static void ok(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.GREEN));
    }

    private static void error(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.RED));
    }

    /** Lets the affected player know, unless they are the one who ran the command. */
    private static void tell(CommandSender sender, BattlePlayer target, String message) {
        if (target.getPlayer() != sender) {
            target.getPlayer().sendMessage(Component.text(message, NamedTextColor.GRAY));
        }
    }
}
