package com.hfturay.battleplugin.ability.abilities.nathanabilities;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.projectileservice.MotionFunction;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

public class OrbThrowRepeater extends ScheduledAbilityTask {
    private final VirtualProjectile projectile;
    private final Location launchLocation;
    private final OrbThrowCallback callback;
    private final double homingDistance;
    private final double turnStrength;
    private final double homingDamageMultiplier;
    private final double catchRadius;
    private boolean homing = false;

    /** Every number comes from abilities.yml (OrbThrow section), read when the orb was thrown. */
    public OrbThrowRepeater(int duration, VirtualProjectile projectile, Location launchLocation, Ability ability,
                            OrbThrowCallback callback, double homingDistance, double turnStrength,
                            double homingDamageMultiplier, double catchRadius) {
        super(duration, ability);

        this.projectile = projectile;
        this.launchLocation = launchLocation;
        this.callback = callback;
        this.homingDistance = homingDistance;
        this.turnStrength = turnStrength;
        this.homingDamageMultiplier = homingDamageMultiplier;
        this.catchRadius = catchRadius;
    }

    @Override
    protected void onTick() {
        if (projectile.getRemainingTicks() <= 0) {
            finishNow();    // the orb expired
            return;
        }

        double distanceTraveled = projectile.getLocation().toVector().subtract(launchLocation.toVector()).length();
        Player player = projectile.getOwner().getPlayer();
        Location playerLocation = player.getLocation().add(0, 1.5, 0);

        if (distanceTraveled >= homingDistance && !homing) {

            MotionFunction homingFunction = new MotionFunction() {

                @Override
                public Vector apply(Vector previousVelocity, int tick) {
                    if (player.isDead() || player == null || !player.isValid()) {
                        return previousVelocity;
                    }

                    Location projectileLocation = projectile.getLocation();
                    Location playerLocation = player.getLocation().add(0, 1.5, 0);

                    Vector homingDir = playerLocation.clone().toVector().subtract(projectileLocation.toVector()).normalize();

                    Vector currentDir = previousVelocity.clone().normalize();
                    Vector newDir = currentDir.multiply(1.0 - turnStrength).add(homingDir.multiply(turnStrength)).normalize();

                    double speed = previousVelocity.length();

                    return newDir.multiply(speed);
                }
            };

            projectile.setMotionFunction(homingFunction);
            homing = true;

            callback.setDamage(callback.getDamage() * homingDamageMultiplier);
        }

        if (homing && playerLocation.clone().toVector().subtract(projectile.getLocation().toVector()).length() < catchRadius) {
            projectile.cancel();
            finishNow();
        }
    }

    // Owner left or died, or the ability was removed: the orb goes with them.
    @Override
    protected void onCancel() {
        projectile.cancel();
    }
}
