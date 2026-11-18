package com.hfturay.battleplugin.ability.abilities.genericabilities;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.projectileservice.ProjectileHitCallback;
import com.hfturay.battleplugin.projectileservice.ProjectileHitResult;

public class BrickCallback implements ProjectileHitCallback {
    private final double damage;

    /** @param damage health points, already read from abilities.yml */
    public BrickCallback(double damage) {
        this.damage = damage;
    }

    @Override
    public void onHit(ProjectileHitResult result, BattlePlayer owner) {
        // Crumbs of orange concrete where it landed, whether it hit a player or a wall.
        BattlePlugin.getInstance().getParticleService().burst(owner, GenericParticles.BRICK_IMPACT, result.getHitLocation());

        Entity entity = result.getHitEntity();

        if (entity instanceof LivingEntity) {
            BattlePlugin.getInstance().getDamageService().damage(owner, entity, damage);
        }
    }
}
