package com.hfturay.battleplugin.ability.abilities.aliabilities;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.particleservice.Anchor;
import com.hfturay.battleplugin.particleservice.ParticleHandle;
import com.hfturay.battleplugin.targetingservice.HitScan;
import com.hfturay.battleplugin.targetingservice.Targets;
import com.hfturay.battleplugin.visualservice.VisualAnchor;
import com.hfturay.battleplugin.visualservice.VisualHandle;

public class LaserRepeater extends ScheduledAbilityTask {
    private BattlePlayer owner;
    private double damage;
    private double range;

    // Where the beam currently ends. Refreshed once per tick here, so the particle service can read it for free.
    private Location beamEnd;
    private ParticleHandle beam;       // the purple swirl
    private VisualHandle body;         // the light blue beam itself

    public LaserRepeater(int duration, Ability ability, BattlePlayer owner, double damage, double range) {
        super(duration, ability);

        this.owner = owner;
        this.damage = damage;
        this.range = range;
    }

    @Override
    protected void onTick() {
        FluidCollisionMode neverCollide = FluidCollisionMode.NEVER;
        Entity target = HitScan.HitScanEntitiesQuietly(owner, range, neverCollide);
        updateBeam();

        if (target instanceof Damageable) {
            BattlePlugin.getInstance().getDamageService().damage(owner, target, damage);
        }
    }

    private void updateBeam() {
        Player player = owner.getPlayer();
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();

        RayTraceResult result = player.getWorld().rayTrace(
            eye,
            direction,
            range,
            FluidCollisionMode.NEVER,
            false,
            0.4,
            // Only things the laser can actually hit end the beam. (It used to stop at ANY entity, which
            // included the beam's own block display, so the beam was cut off right at the muzzle.)
            entity -> Targets.isTargetable(owner, entity)
        );

        Vector hitPosition = result != null ? result.getHitPosition() : null;
        beamEnd = hitPosition != null
            ? hitPosition.toLocation(player.getWorld())
            : eye.clone().add(direction.multiply(range));

        if (beam == null) {
            // One beam for the whole duration. Both ends are live: the muzzle follows the player's eyes,
            // the end follows beamEnd. (The old effect was built once at join, so its start never moved.)
            beam = BattlePlugin.getInstance().getParticleService().beam(
                owner,
                AliParticles.LASER_SWIRL,
                Anchor.of(() -> player.isValid() ? muzzle(player) : null),
                Anchor.of(() -> beamEnd),
                getTicksRemaining());

            // The solid beam: starts at the muzzle, points at beamEnd and is as long as the gap between them.
            body = BattlePlugin.getInstance().getVisualService().follow(owner, AliVisuals.LASER_BODY, new VisualAnchor() {
                @Override public Location location() { return muzzle(player); }
                @Override public Vector direction() { return beamEnd.toVector().subtract(muzzle(player).toVector()); }
                @Override public double length() { return beamEnd.distance(muzzle(player)); }
                @Override public boolean isValid() { return beamEnd != null && player.isValid() && isActive(); }
            });
        }
    }

    /** How far in front of the player the beam starts, and how far below the eyes (0.45 is about chest height). */
    private static final double MUZZLE_FORWARD = 0.5;
    private static final double MUZZLE_BELOW_EYES = 0.45;

    private static Location muzzle(Player player) {
        Location eye = player.getEyeLocation();
        return eye.add(eye.getDirection().multiply(MUZZLE_FORWARD)).subtract(0, MUZZLE_BELOW_EYES, 0);
    }

    @Override
    protected void onFinish() {
        stopBeam();
    }

    @Override
    protected void onCancel() {
        stopBeam();
    }

    private void stopBeam() {
        if (beam != null) {
            beam.cancel();
        }
        if (body != null) {
            body.cancel();
        }
    }
}
