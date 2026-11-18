package com.hfturay.battleplugin.targetingservice;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import com.hfturay.battleplugin.battleplayer.BattlePlayer;

/** Area targeting. Uses the world the area is actually in, not a fixed one. */
public class AOE {

    /** Everything the attacker can hit within a sphere of the given radius. */
    public static List<Entity> getTargets(BattlePlayer battlePlayer, Location center, double radius) {
        return collect(battlePlayer, center, radius, radius, radius, radius);
    }

    /**
     * Everything the attacker can hit within the sphere, that is also inside the box of half-sizes x, y, z
     * around the centre (a flattened or stretched area).
     */
    public static List<Entity> getTargets(BattlePlayer battlePlayer, Location center, double radius, double x, double y, double z) {
        return collect(battlePlayer, center, radius, x, y, z);
    }

    private static List<Entity> collect(BattlePlayer battlePlayer, Location center, double radius, double x, double y, double z) {
        List<Entity> targets = new ArrayList<>();
        World world = center.getWorld();
        if (world == null) {
            return targets;
        }

        double radiusSquared = radius * radius;
        for (Entity entity : world.getNearbyEntities(center, x, y, z, e -> Targets.isTargetable(battlePlayer, e))) {
            if (entity.getLocation().distanceSquared(center) > radiusSquared) {
                continue;
            }
            targets.add(entity);
        }
        return targets;
    }
}
