package com.hfturay.battleplugin.ability.abilities.aliabilities;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.projectileservice.ProjectileHitCallback;
import com.hfturay.battleplugin.projectileservice.ProjectileHitResult;
import com.hfturay.battleplugin.targetingservice.AOE;

public class PharahRocketCallback implements ProjectileHitCallback {
    private double radius = 5;
    private double damage = 4;
    private double horizForce = 1.2;
    private double lift = 0.6;

    @Override
    public void onHit(ProjectileHitResult result, BattlePlayer owner) {
        List<Entity> targets = AOE.getTargets(owner, result.getHitLocation(), radius);
        
        BattlePlugin.getInstance().getWorld().spawnParticle(Particle.EXPLOSION, result.getHitLocation().clone(), 1);
        Vector knockback;
        Location center = result.getHitLocation();

        for (Entity entity : targets) {
            if (entity instanceof Damageable) {
                ((Damageable)entity).damage(damage);
            }

            knockback = entity.getLocation().toVector().subtract(center.toVector()).normalize().multiply(horizForce);
            knockback.setY(lift);
            
            entity.setVelocity(knockback);
        }
    }
}
