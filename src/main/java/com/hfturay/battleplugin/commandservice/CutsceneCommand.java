package com.hfturay.battleplugin.commandservice;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.hfturay.battleplugin.cutsceneservice.CutsceneService;
import com.hfturay.battleplugin.cutsceneservice.Scenes;

/** /cutscene test plays the test scene for you and everyone within 30 blocks. /cutscene stop ends it. */
public final class CutsceneCommand implements TabExecutor {
    private static final double RADIUS = 30;

    private final CutsceneService cutscenes;

    public CutsceneCommand(CutsceneService cutscenes) {
        this.cutscenes = cutscenes;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }
        String sub = args.length == 0 ? "" : args[0].toLowerCase();
        if (sub.equals("stop")) {
            sender.sendMessage(cutscenes.cancel(player.getUniqueId()) ? "Cutscene stopped." : "You are not in a cutscene.");
            return true;
        }
        if (sub.equals("test")) {
            Location anchor = player.getLocation();
            List<Player> viewers = player.getWorld().getNearbyPlayers(anchor, RADIUS).stream().toList();
            boolean started = cutscenes.play(Scenes.test(), anchor, viewers, () -> player.sendMessage("Cutscene finished."));
            if (!started) {
                sender.sendMessage("Could not start (you may already be in a cutscene).");
            }
            return true;
        }
        sender.sendMessage("Usage: /cutscene test  or  /cutscene stop");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return args.length == 1 ? List.of("test", "stop") : List.of();
    }
}
