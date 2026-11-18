package com.hfturay.battleplugin.ability.abilities.muhammadabilities;

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

public class PipeBombCallBack implements ProjectileHitCallback {
    // No numbers here: PipeBomb reads them from abilities.yml each time a bomb is thrown.
    private final double blastRadius;
    private final double damage;
    private final double knockback;

    public PipeBombCallBack(double blastRadius, double damage, double knockback) {
        this.blastRadius = blastRadius;
        this.damage = damage;
        this.knockback = knockback;
    }

    @Override
    public void onHit(ProjectileHitResult result, BattlePlayer owner) {
        Location location = result.getHitLocation();

        List<Entity> targets = AOE.getTargets(owner, location, blastRadius);

        BattlePlugin.getInstance().getParticleService().burst(owner, MuhammadParticles.PIPE_BOMB_EXPLOSION, location);
        for (Entity entity : targets) {
            if (entity instanceof Damageable) {
                BattlePlugin.getInstance().getDamageService().damage(owner, entity, damage);

                Knockback.away(location, entity, knockback);
            }
        }
    }
}
