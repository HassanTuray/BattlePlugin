package com.hfturay.battleplugin.ability.abilities.nathanabilities;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.projectileservice.MotionFunction;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

public class OrbThrowRepeater extends ScheduledAbilityTask  {
    private VirtualProjectile projectile;
    private Location launchLocation;
    private double turnStrength = 0.75;
    private boolean homing = false;
    
    public OrbThrowRepeater(int duration, VirtualProjectile projectile, Location launchLocation) {
        super(duration);
        
        this.projectile = projectile;
        this.launchLocation = launchLocation;
    }

    @Override
    public boolean update() {
        int ticksRemaining = projectile.getRemainingTicks();

        if (ticksRemaining <= 0) {
            this.cancel();
            return super.update();
        }

        double distanceTraveled = projectile.getLocation().toVector().subtract(launchLocation.toVector()).length();
        Player player = projectile.getOwner().getPlayer();
        Location playerLocation = player.getLocation().add(0, 1.5, 0);

        if (distanceTraveled >= 15 && !homing) {
            
            MotionFunction homingFunction = new MotionFunction() {

                @Override
                public Vector apply(Vector previousVelocity, int tick) {
                    if(player.isDead() || player == null || !player.isValid()) {
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

            OrbThrowCallback callback = (OrbThrowCallback) projectile.getProjectileHitCallback();
            callback.setDamage(callback.getDamage() * 2);
        }

        if (homing && playerLocation.clone().toVector().subtract(projectile.getLocation().toVector()).length() < 1) {
            OrbThrowCallback callback = (OrbThrowCallback) projectile.getProjectileHitCallback();
            callback.setDamage(3);
            projectile.cancel();
            this.cancel();
        }

        return super.update();
    }

    @Override
    public void run() {
        
    }
}
