package com.hfturay.battleplugin.combatservice;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

/**
 * Every push and launch goes through here.
 *
 * Entity.setVelocity throws if any part of the vector is NaN or infinite, and the "push away from a point"
 * maths produces NaN when the target is exactly on that point (normalising a zero vector). These helpers
 * handle that case, and skip entities that are dead or gone.
 */
public final class Knockback {
    private Knockback() { }

    /** Replace the entity's velocity. */
    public static boolean set(Entity entity, Vector velocity) {
        if (!usable(entity) || !finite(velocity)) {
            return false;
        }
        entity.setVelocity(velocity);
        return true;
    }

    /** Add to the entity's current velocity. */
    public static boolean add(Entity entity, Vector change) {
        if (!usable(entity) || !finite(change)) {
            return false;
        }
        entity.setVelocity(entity.getVelocity().add(change));
        return true;
    }

    /** Push away from a point with the given strength, in 3D. Straight up if it is exactly on the point. */
    public static boolean away(Location from, Entity entity, double strength) {
        if (!usable(entity)) {
            return false;
        }
        return set(entity, direction(from, entity).multiply(strength));
    }

    /** Push away horizontally, then set the vertical speed to a fixed lift (the "blast" feel). */
    public static boolean awayWithLift(Location from, Entity entity, double horizontal, double lift) {
        if (!usable(entity)) {
            return false;
        }
        Vector push = direction(from, entity).multiply(horizontal);
        push.setY(lift);
        return set(entity, push);
    }

    /**
     * Launch straight up so the entity rises about `blocks` blocks, with no sideways push at all (any sideways
     * movement it had is cancelled).
     */
    public static boolean straightUp(Entity entity, double blocks) {
        return set(entity, new Vector(0, upwardSpeedFor(blocks), 0));
    }

    /**
     * The upward speed (blocks per tick) that makes a mob or player rise `blocks` blocks before falling. Worked out
     * with the game's own falling rule (each tick: move, then speed = (speed - 0.08) * 0.98), so the height is exact
     * rather than guessed: 5 blocks needs about 0.91.
     */
    public static double upwardSpeedFor(double blocks) {
        if (!(blocks > 0)) {
            return 0;
        }
        double low = 0, high = 10;
        for (int i = 0; i < 50; i++) {
            double mid = (low + high) / 2;
            if (peakHeight(mid) < blocks) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return high;
    }

    /** How high an upward speed carries a mob or player, by the game's falling rule. */
    static double peakHeight(double speed) {
        double y = 0;
        while (speed > 0) {
            y += speed;
            speed = (speed - 0.08) * 0.98;
        }
        return y;
    }

    /** Unit vector from a point to the entity. Straight up if they are on the same spot. */
    static Vector direction(Location from, Entity entity) {
        Vector delta = entity.getLocation().toVector().subtract(from.toVector());
        if (delta.lengthSquared() < 1.0E-8) {
            return new Vector(0, 1, 0);
        }
        return delta.normalize();
    }

    private static boolean usable(Entity entity) {
        return entity != null && entity.isValid();
    }

    private static boolean finite(Vector v) {
        return v != null && Double.isFinite(v.getX()) && Double.isFinite(v.getY()) && Double.isFinite(v.getZ());
    }
}
