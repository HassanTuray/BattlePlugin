package com.hfturay.battleplugin.shikigamiservice;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import com.destroystokyo.paper.entity.Pathfinder;

/**
 * Chases its target if it has one, otherwise stays within followDistance of its owner. If it gets farther than
 * leashDistance from the owner (or in another world) it is teleported to them so it is never left behind.
 */
public final class FollowAndChase implements Movement {
    private final double followDistance;
    private final double leashDistance;
    private final double speed;

    public FollowAndChase(double followDistance, double leashDistance, double speed) {
        this.followDistance = followDistance;
        this.leashDistance = leashDistance;
        this.speed = speed;
    }

    @Override
    public void move(BrainShikigami self) {
        Mob mob = self.mob();
        Player owner = self.getOwner().getPlayer();
        Pathfinder pathfinder = mob.getPathfinder();

        boolean sameWorld = mob.getWorld().equals(owner.getWorld());
        if (!sameWorld || mob.getLocation().distanceSquared(owner.getLocation()) > leashDistance * leashDistance) {
            pathfinder.stopPathfinding();
            mob.teleport(owner.getLocation());
            return;
        }

        LivingEntity target = self.target();
        if (target != null) {
            pathfinder.moveTo(target, speed);
        } else if (mob.getLocation().distanceSquared(owner.getLocation()) > followDistance * followDistance) {
            pathfinder.moveTo(owner, speed);
        } else {
            pathfinder.stopPathfinding();
        }
    }
}
