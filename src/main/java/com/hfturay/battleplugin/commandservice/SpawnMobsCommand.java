package com.hfturay.battleplugin.commandservice;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * /spawnmobs [type] [count] [radius]: spawns mobs on the ground around you, for testing abilities against a crowd.
 * With no type they are random zombies, skeletons and wither skeletons; a type (zombie, skeleton, creeper...) spawns
 * only that. Default 1000 mobs in a 50 block radius. They are spawned 50 a tick so the
 * server does not freeze, and are placed on the highest block of a random spot (so not inside caves or buildings).
 */
public final class SpawnMobsCommand implements TabExecutor {
    private static final EntityType[] TYPES = { EntityType.ZOMBIE, EntityType.SKELETON, EntityType.WITHER_SKELETON };
    private static final int PER_TICK = 50;
    private static final int MAX_COUNT = 5000;

    private final Plugin plugin;

    public SpawnMobsCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }

        // an optional mob name first (zombie), then the count and radius
        EntityType only = null;
        int next = 0;
        if (args.length > 0 && !args[0].matches("[0-9.]+")) {
            try {
                only = EntityType.valueOf(args[0].toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException e) {
                only = null;
            }
            if (only == null || !only.isSpawnable() || !only.isAlive()) {
                sender.sendMessage("'" + args[0] + "' is not a mob I can spawn. Try zombie, skeleton, wither_skeleton, creeper...");
                return true;
            }
            next = 1;
        }

        int count;
        double radius;
        try {
            count = args.length > next ? Integer.parseInt(args[next]) : 1000;
            radius = args.length > next + 1 ? Double.parseDouble(args[next + 1]) : 50;
        } catch (NumberFormatException e) {
            sender.sendMessage("Usage: /spawnmobs [type] [count] [radius]");
            return true;
        }
        if (count < 1 || count > MAX_COUNT || !(radius > 0)) {
            sender.sendMessage("Count must be 1 to " + MAX_COUNT + " and the radius above 0.");
            return true;
        }

        World world = player.getWorld();
        Location center = player.getLocation();
        int total = count;
        double reach = radius;
        EntityType fixedType = only;

        new BukkitRunnable() {
            int spawned = 0;

            @Override
            public void run() {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (int i = 0; i < PER_TICK && spawned < total; i++, spawned++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double distance = reach * Math.sqrt(random.nextDouble());       // even spread over the disc
                    double x = center.getX() + Math.cos(angle) * distance;
                    double z = center.getZ() + Math.sin(angle) * distance;
                    double y = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 1;
                    world.spawnEntity(new Location(world, x, y, z), fixedType != null ? fixedType : TYPES[random.nextInt(TYPES.length)]);
                }
                if (spawned >= total) {
                    cancel();
                    if (player.isOnline()) {
                        player.sendMessage("Spawned " + total + " mobs within " + (int) reach + " blocks.");
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        sender.sendMessage("Spawning " + count + (only == null ? " zombies, skeletons and wither skeletons" : " " + only.name().toLowerCase(java.util.Locale.ROOT)) + "...");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return List.of("zombie", "skeleton", "wither_skeleton", "creeper", "1000");
        }
        return args.length <= 3 ? List.of(args.length == 2 ? "1000" : "50") : List.of();
    }
}
