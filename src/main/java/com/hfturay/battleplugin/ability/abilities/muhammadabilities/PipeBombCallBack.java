package com.hfturay.battleplugin.ability.abilities.muhammadabilities;

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

public class PipeBombCallBack implements ProjectileHitCallback {
    private double blastRadius = 6;

    @Override
    public void onHit(ProjectileHitResult result, BattlePlayer owner) {
        Location location = result.getHitLocation();
        Vector knockback = null;

        List<Entity> targets = AOE.getTargets(owner, location, blastRadius);

        BattlePlugin.getInstance().getWorld().spawnParticle(Particle.EXPLOSION, location.clone().add(0, 1,0), 2);
        for (Entity entity : targets) {
            if (entity instanceof Damageable) {
                ((Damageable) entity).damage(4);

                knockback = entity.getLocation().toVector().subtract(location.toVector()).normalize().multiply(1.2);
                entity.setVelocity(knockback);
            }
        }
    }
}
