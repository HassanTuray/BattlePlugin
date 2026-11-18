package com.hfturay.battleplugin.projectileservice;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.particleservice.ParticleHandle;
import com.hfturay.battleplugin.particleservice.ParticleSpec;
import com.hfturay.battleplugin.targetingservice.Targets;
import com.hfturay.battleplugin.visualservice.VisualAnchor;
import com.hfturay.battleplugin.visualservice.VisualHandle;
import com.hfturay.battleplugin.visualservice.VisualSpec;

/**
 * A projectile that moves in code instead of being a real entity.
 *
 * Each tick it travels along a line from where it was to where it will be, and everything that line crosses is
 * checked, nearest first. A fast projectile can therefore not skip over a thin target or a wall (it used to be
 * teleported and only the end point was checked), and it starts at the owner's eyes without hitting the owner.
 */
public class VirtualProjectile {
    private final BattlePlayer owner;
    private final World world;
    private Location location;
    private Vector direction;
    private MotionFunction motionFunction;
    private Vector velocity;
    private final int maxTicks;
    private int ticksLived = 0;
    private final ProjectileHitCallback callback;
    private double hitbox;

    // Visuals are a spec (plain data) played by the ParticleService when the projectile launches.
    // Every projectile gets its own running copy, so two in flight never share or cancel each other's particles.
    private final ParticleSpec particles;
    private ParticleHandle particleHandle;

    // An optional solid body (blocks) shown by the VisualService. Removed in finish(), so it is gone the same
    // moment the projectile is, whether it hit something, ran out of range or was cancelled.
    private VisualSpec visual;
    private VisualHandle visualHandle;

    private boolean finished = false;

    private boolean stopOnHitPlayer = true;
    private boolean stopOnHitBlock = true;
    private boolean hitPlayers = true;
    private boolean hitBlocks = true;

    public VirtualProjectile(
        Location start,
        Vector direction,
        double speed,
        int maxTicks,
        BattlePlayer owner,
        double hitbox,
        ProjectileHitCallback callback,
        ParticleSpec particles)
    {
        this.location = start.clone();
        this.world = start.getWorld();
        this.direction = direction.clone().normalize();
        this.maxTicks = maxTicks;
        this.owner = owner;
        this.motionFunction = (v, t) -> v;
        this.callback = callback;
        this.velocity = this.direction.clone().multiply(speed);
        this.particles = particles;
        this.hitbox = hitbox;
    }

    /** Starts at the owner's eyes, flying where they look. */
    public VirtualProjectile(
        double speed,
        int maxTicks,
        BattlePlayer owner,
        double hitbox,
        ProjectileHitCallback callback,
        ParticleSpec particles)
    {
        this(owner.getPlayer().getEyeLocation(), owner.getPlayer().getEyeLocation().getDirection(),
            speed, maxTicks, owner, hitbox, callback, particles);
    }

    /** Called by the ProjectileService when the projectile starts flying. Starts its visuals. */
    public void onLaunch() {
        if (particles != null) {
            particleHandle = BattlePlugin.getInstance().getParticleService().follow(owner, particles, this);
        }
        if (visual != null) {
            visualHandle = BattlePlugin.getInstance().getVisualService().follow(owner, visual, new VisualAnchor() {
                @Override public Location location() { return location; }
                @Override public Vector direction() { return direction; }
                @Override public boolean isValid() { return !finished; }
            });
        }
    }

    /** A solid body for this projectile, shown while it flies. Set before the projectile is launched. */
    public void setVisual(VisualSpec visual) {
        this.visual = visual;
    }

    /** One tick of flight. Returns true when the projectile is over. */
    public boolean tick() {
        if (finished) {
            return true;
        }

        ticksLived++;
        velocity = motionFunction.apply(velocity, ticksLived);

        double distance = velocity.length();
        if (distance > 1.0E-9 && Double.isFinite(distance)) {
            if (travel(velocity.clone(), distance)) {
                return true;
            }
        }

        if (ticksLived >= maxTicks) {
            finish();
            return true;
        }
        return false;
    }

    /** One thing the flight line crossed, with how far along the step it was. */
    private record Hit(double distance, Entity entity, Block block, Location point) { }

    /** Moves along this tick's step, handling hits in order. Returns true if the projectile stopped. */
    private boolean travel(Vector step, double distance) {
        Location start = location.clone();
        Vector origin = start.toVector();
        Vector unit = step.clone().multiply(1.0 / distance);
        Location end = start.clone().add(step);

        List<Hit> hits = new ArrayList<>();

        if (hitBlocks) {
            RayTraceResult blockHit = world.rayTraceBlocks(start, unit, distance, FluidCollisionMode.NEVER, true);
            if (blockHit != null && blockHit.getHitBlock() != null) {
                Vector at = blockHit.getHitPosition();
                hits.add(new Hit(origin.distance(at), null, blockHit.getHitBlock(), at.toLocation(world)));
            }
        }

        if (hitPlayers) {
            BoundingBox area = BoundingBox.of(start, end).expand(hitbox);
            for (Entity entity : world.getNearbyEntities(area, e -> Targets.isTargetable(owner, e))) {
                RayTraceResult entityHit = entity.getBoundingBox().expand(hitbox).rayTrace(origin, unit, distance);
                if (entityHit != null) {
                    Vector at = entityHit.getHitPosition();
                    hits.add(new Hit(origin.distance(at), entity, null, at.toLocation(world)));
                }
            }
        }

        hits.sort(Comparator.comparingDouble(Hit::distance));

        for (Hit hit : hits) {
            boolean stops = hit.entity() != null ? stopOnHitPlayer : stopOnHitBlock;

            // A projectile that stops is reported where it hit. One that keeps going is reported where it
            // ends up this tick, as it always was (the wave and orb abilities rely on that).
            location = stops ? hit.point() : end.clone();

            ProjectileHitResult result = hit.entity() != null
                ? new ProjectileHitResult(hit.entity(), location, this)
                : new ProjectileHitResult(hit.block(), location, this);
            callback.onHit(result, owner);

            if (finished) {
                return true;     // the callback ended it
            }
            if (stops) {
                cancel();
                return true;
            }
        }

        location = end;
        return false;
    }

    public double getHitbox() {
        return hitbox;
    }

    public void setHitbox(double hitbox) {
        this.hitbox = hitbox;
    }

    public Location getLocation() {
        return this.location;
    }

    public void setStopOnHitPlayer(boolean stop) {
        this.stopOnHitPlayer = stop;
    }

    public void setStopOnHitBlock(boolean stop) {
        this.stopOnHitBlock = stop;
    }

    public void setHitPlayers(boolean hitPlayers) {
        this.hitPlayers = hitPlayers;
    }

    public void setHitBlocks(boolean hitBlocks) {
        this.hitBlocks = hitBlocks;
    }

    public Vector getVelocity() {
        return this.velocity;
    }

    public void setVelocity(Vector velocity) {
        this.velocity = velocity;
    }

    public void setMotionFunction(MotionFunction function) {
        this.motionFunction = function;
    }

    public ProjectileHitCallback getProjectileHitCallback() {
        return this.callback;
    }

    public Vector getDirection() {
        return direction.clone();
    }

    public int getRemainingTicks() {
        return finished ? 0 : this.maxTicks - this.ticksLived;
    }

    public boolean isFinished() {
        return finished;
    }

    public BattlePlayer getOwner() {
        return this.owner;
    }

    /** Ends the projectile now. No callback runs. Safe to call any number of times. */
    public void cancel() {
        finish();
    }

    private void finish() {
        finished = true;
        ticksLived = maxTicks;
        stopParticles();
    }

    private void stopParticles() {
        if (particleHandle != null) {
            particleHandle.cancel();
        }
        if (visualHandle != null) {
            visualHandle.cancel();
        }
    }
}
