package com.hfturay.battleplugin.ability.abilities.muhammadabilities;

import org.bukkit.Location;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.projectileservice.ProjectileHitCallback;
import com.hfturay.battleplugin.projectileservice.ProjectileHitResult;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

public class MeltdownOfWavesCallback implements ProjectileHitCallback {
    // may need to attempt to get the current position of the projectile rather than the hit position
    private double alignmentThreshold = 0.45;
    private double pushStrength = 0.5;
    private double lift = 0.05;
    private double damage = 1;

    @Override
    public void onHit(ProjectileHitResult result, BattlePlayer owner) {
        VirtualProjectile projectile = result.getVirtualProjectile();
        Location projectileLocation = projectile.getLocation();
        Vector projectileDirection = projectile.getDirection().setY(0).normalize();

        Entity target = result.getHitEntity();

        Vector vectorToEntity = target.getLocation().toVector().subtract(projectileLocation.toVector());
        vectorToEntity.setY(0).normalize();

        double alignment = vectorToEntity.dot(projectileDirection);
        
        BattlePlugin.getInstance().getLogger().info(Double.toString(alignment));
        if (Math.abs(alignment) < alignmentThreshold) {
            return;
        }

        Vector push = projectileDirection.clone().multiply(pushStrength);
        push.add(new Vector(0, lift, 0));

        target.setVelocity(push);

        if (target instanceof Damageable) {
            ((Damageable) target).damage(damage);
        }
    }
}
