package com.hfturay.battleplugin.shikigamiservice;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.destroystokyo.paper.entity.Pathfinder;

/**
 * Like FollowAndChase, but it never leaves its owner's side to fight: it goes towards its target only as far as
 * guardRange from the owner. A target farther away than that is faced from the edge of that range (the side towards
 * the target), so a shikigami with a ranged attack can still reach it. With no target it stays within
 * followDistance of the owner. Farther than leashDistance from the owner (or in another world) it is teleported back.
 */
public final class GuardOwner implements Movement {
    /** Close enough to the spot it is heading for: it stops there instead of shuffling about. */
    private static final double ARRIVED = 1.5;

    private final double guardRange;
    private final double followDistance;
    private final double leashDistance;
    private final double speed;

    public GuardOwner(double guardRange, double followDistance, double leashDistance, double speed) {
        this.guardRange = guardRange;
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
        if (target == null) {
            if (mob.getLocation().distanceSquared(owner.getLocation()) > followDistance * followDistance) {
                pathfinder.moveTo(owner, speed);
            } else {
                pathfinder.stopPathfinding();
            }
            return;
        }

        Location goal = withinGuardRange(owner.getLocation(), target.getLocation());
        if (mob.getLocation().distanceSquared(goal) <= ARRIVED * ARRIVED) {
            pathfinder.stopPathfinding();
            mob.lookAt(target);
        } else {
            pathfinder.moveTo(goal, speed);
        }
    }

    /** The target's spot, or if that is too far from the owner, the point guardRange from the owner towards it. */
    private Location withinGuardRange(Location owner, Location target) {
        Vector flat = target.toVector().subtract(owner.toVector()).setY(0);
        if (flat.lengthSquared() <= guardRange * guardRange) {
            return target;
        }
        flat.normalize().multiply(guardRange);
        return owner.clone().add(flat.getX(), 0, flat.getZ());
    }
}
