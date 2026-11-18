package com.hfturay.battleplugin.commandservice;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.configservice.AbilityTuning;
import com.hfturay.battleplugin.kitservice.KitConfig;
import com.hfturay.battleplugin.kitservice.KitService;
import com.hfturay.battleplugin.loggerservice.PlayerLogger;

/**
 * /kits reload          re-read config.yml and kits.yml
 * /kits apply <player>  give an online player the kit kits.yml has for them (replaces what they have now)
 * /kits list            the kits that are loaded
 */
public final class KitCommands implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("reload", "apply", "list");
    private static final int MAX_WARNINGS_SHOWN = 5;

    private final BattlePlugin plugin;

    public KitCommands(BattlePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return false;
        }

        try {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "reload" -> reload(sender);
                case "apply" -> apply(sender, args);
                case "list" -> list(sender);
                default -> {
                    return false;
                }
            }
        } catch (RuntimeException e) {
            sender.sendMessage("Something went wrong: " + e.getMessage() + " (details in the console)");
            plugin.getLogger().log(java.util.logging.Level.WARNING, "/kits failed", e);
        }
        return true;
    }

    private void reload(CommandSender sender) {
        plugin.reloadSettings();
        KitService.LoadResult result = plugin.getKitService().reload();

        if (!result.ok()) {
            sender.sendMessage("kits.yml could not be read, so the old kits stay in use: " + result.error());
            return;
        }

        sender.sendMessage("Reloaded config.yml and kits.yml: " + result.kits() + " kits, " + result.players()
            + " players listed, " + result.warnings().size() + " warnings.");
        showWarnings(sender, result.warnings());

        AbilityTuning.LoadResult tuning = plugin.getAbilityTuning().reload();
        if (!tuning.ok()) {
            sender.sendMessage("abilities.yml could not be read, so the old numbers stay in use: " + tuning.error());
            return;
        }
        sender.sendMessage("Reloaded abilities.yml: " + tuning.abilities() + " abilities, " + tuning.values()
            + " values, " + tuning.warnings().size() + " warnings. Applies the next time each ability is used.");
        showWarnings(sender, tuning.warnings());
    }

    private static void showWarnings(CommandSender sender, List<String> warnings) {
        int shown = 0;
        for (String warning : warnings) {
            if (shown++ >= MAX_WARNINGS_SHOWN) {
                sender.sendMessage("... and " + (warnings.size() - MAX_WARNINGS_SHOWN) + " more (see the console)");
                break;
            }
            sender.sendMessage("  " + warning);
        }
    }

    private void apply(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /kits apply <player>");
            return;
        }

        Player target = args[1].equalsIgnoreCase("me") && sender instanceof Player self ? self : Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(args[1] + " is not online.");
            return;
        }
        BattlePlayer battlePlayer = PlayerLogger.getBattlePlayer(target.getUniqueId());
        if (battlePlayer == null) {
            sender.sendMessage(target.getName() + " has no ability data yet. Have them rejoin.");
            return;
        }

        KitConfig.Match match = plugin.getKitService().resolve(target.getUniqueId(), target.getName());
        if (match == null) {
            sender.sendMessage(target.getName() + " is not listed in kits.yml and there is no default kit.");
            return;
        }

        int loaded = plugin.getKitService().apply(match.kit(), battlePlayer, plugin.getAbilityFactory()::create);
        plugin.onLoadoutChanged(battlePlayer);
        sender.sendMessage("Gave " + target.getName() + " the '" + match.kit().name() + "' kit (" + loaded
            + " abilities, matched by " + match.source().name().toLowerCase(Locale.ROOT) + ").");
    }

    private void list(CommandSender sender) {
        var kits = plugin.getKitService().config().kits();
        if (kits.isEmpty()) {
            sender.sendMessage("No kits are loaded.");
            return;
        }
        for (KitConfig.Kit kit : kits.values()) {
            sender.sendMessage(kit.name() + ": " + kit.size() + " abilities");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(SUBCOMMANDS);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("apply")) {
            options.add("me");
            Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
        }

        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        options.removeIf(option -> !option.toLowerCase(Locale.ROOT).startsWith(typed));
        return options;
    }
}
