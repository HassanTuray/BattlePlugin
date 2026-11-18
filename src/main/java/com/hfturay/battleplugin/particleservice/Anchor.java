package com.hfturay.battleplugin.particleservice;

import java.util.function.Supplier;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

/**
 * Where a particle effect lives. The service asks the anchor for its position every frame, so the same spec can
 * be played at a fixed point, on an entity, on a projectile, or on anything else that can report a location.
 *
 * An anchor that stops being valid (entity died or logged out, projectile finished) ends the effect automatically.
 */
public interface Anchor {

    /** Current position. May be a shared, mutable object: clone it before storing it. */
    Location location();

    /** Current facing / travel direction. */
    default Vector direction() {
        return location().getDirection();
    }

    /** False once the thing this anchor describes is gone. The effect ends the tick this turns false. */
    boolean isValid();

    /** The entity this anchor follows, or null. Lets EffectLib effects follow natively. */
    default Entity entity() {
        return null;
    }

    /** True if location() can change after the effect started. */
    default boolean isMoving() {
        return false;
    }

    // ---- factories ----

    /** A fixed point in the world. The location is copied. */
    static Anchor at(Location location) {
        final Location fixed = location.clone();
        return new Anchor() {
            @Override public Location location() { return fixed; }
            @Override public boolean isValid() { return fixed.getWorld() != null; }
        };
    }

    /** Follows an entity's feet. */
    static Anchor of(Entity entity) {
        return of(entity, 0);
    }

    /** Follows an entity, shifted up by yOffset. (EffectLib effects use their own offsets instead.) */
    static Anchor of(final Entity entity, final double yOffset) {
        return new Anchor() {
            @Override public Location location() { return entity.getLocation().add(0, yOffset, 0); }
            @Override public Vector direction() {
                return entity instanceof LivingEntity living
                        ? living.getEyeLocation().getDirection()
                        : entity.getLocation().getDirection();
            }
            @Override public boolean isValid() { return entity.isValid(); }
            @Override public Entity entity() { return entity; }
            @Override public boolean isMoving() { return true; }
        };
    }

    /** Follows a virtual projectile until it finishes or is cancelled. */
    static Anchor of(final VirtualProjectile projectile) {
        return new Anchor() {
            @Override public Location location() { return projectile.getLocation(); }
            @Override public Vector direction() { return projectile.getDirection(); }
            @Override public boolean isValid() { return projectile.getRemainingTicks() > 0; }
            @Override public boolean isMoving() { return true; }
        };
    }

    /** Follows whatever the supplier returns. Ends when it returns null. Useful for ray-trace end points. */
    static Anchor of(final Supplier<Location> supplier) {
        return new Anchor() {
            @Override public Location location() { return supplier.get(); }
            @Override public boolean isValid() { return supplier.get() != null; }
            @Override public boolean isMoving() { return true; }
        };
    }

    /** No position. For timers that only run code (see ParticleService.delay). */
    static Anchor none() {
        return new Anchor() {
            @Override public Location location() { return null; }
            @Override public Vector direction() { return new Vector(0, 0, 1); }
            @Override public boolean isValid() { return true; }
        };
    }
}
