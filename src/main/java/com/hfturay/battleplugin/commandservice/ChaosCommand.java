package com.hfturay.battleplugin.commandservice;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.hfturay.battleplugin.chaosservice.ChaosService;

/** /chaos [player] shows chaos. /chaos set|add <player> <amount> (admins) changes it, for testing. */
public final class ChaosCommand implements TabExecutor {
    private final ChaosService chaos;

    public ChaosCommand(ChaosService chaos) {
        this.chaos = chaos;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 3 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("add"))) {
            if (!sender.hasPermission("battleplugin.admin")) {
                sender.sendMessage("You need permission to change chaos.");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("No online player named " + args[1] + ".");
                return true;
            }
            double amount;
            try {
                amount = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("'" + args[2] + "' is not a number.");
                return true;
            }
            if (args[0].equalsIgnoreCase("set")) {
                chaos.set(target.getUniqueId(), amount);
            } else {
                chaos.add(target.getUniqueId(), amount);
            }
            sender.sendMessage(target.getName() + " now has " + format(chaos.get(target.getUniqueId())) + " chaos.");
            return true;
        }

        Player target;
        if (args.length >= 1) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage("No online player named " + args[0] + ".");
                return true;
            }
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            sender.sendMessage("Usage: /chaos <player>");
            return true;
        }
        sender.sendMessage(target.getName() + " has " + format(chaos.get(target.getUniqueId())) + " / " + format(chaos.full()) + " chaos.");
        return true;
    }

    private static String format(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format("%.1f", value);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("set", "add"));
            Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
            return options.stream().filter(o -> o.toLowerCase().startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("add"))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).toList();
        }
        return List.of();
    }
}
