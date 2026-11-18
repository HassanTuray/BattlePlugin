package com.hfturay.battleplugin.ability.abilities.aliabilities;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.projectileservice.ProjectileHitCallback;
import com.hfturay.battleplugin.projectileservice.ProjectileHitResult;
import com.hfturay.battleplugin.targetingservice.AOE;
import com.hfturay.battleplugin.combatservice.Knockback;

public class PharahRocketCallback implements ProjectileHitCallback {
    // No numbers here: PharahRocket reads them from abilities.yml each time a rocket is fired.
    private final double radius;
    private final double damage;
    private final double horizForce;
    private final double lift;

    public PharahRocketCallback(double radius, double damage, double horizForce, double lift) {
        this.radius = radius;
        this.damage = damage;
        this.horizForce = horizForce;
        this.lift = lift;
    }

    @Override
    public void onHit(ProjectileHitResult result, BattlePlayer owner) {
        List<Entity> targets = AOE.getTargets(owner, result.getHitLocation(), radius);

        BattlePlugin.getInstance().getParticleService().burst(owner, AliParticles.PHARAH_ROCKET_EXPLOSION, result.getHitLocation());
        Location center = result.getHitLocation();

        for (Entity entity : targets) {
            if (entity instanceof Damageable) {
                BattlePlugin.getInstance().getDamageService().damage(owner, entity, damage);
            }

            Knockback.awayWithLift(center, entity, horizForce, lift);
        }
    }
}
