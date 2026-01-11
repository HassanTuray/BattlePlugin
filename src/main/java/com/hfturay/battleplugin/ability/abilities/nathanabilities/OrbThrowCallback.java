package com.hfturay.battleplugin.ability.abilities.nathanabilities;

import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;

import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.projectileservice.ProjectileHitCallback;
import com.hfturay.battleplugin.projectileservice.ProjectileHitResult;

public class OrbThrowCallback implements ProjectileHitCallback {
    public double damage = 4;

    @Override
    public void onHit(ProjectileHitResult result, BattlePlayer owner) {
        Entity entity = result.getHitEntity();

        if (entity == null) {
            return;
        }

        if (entity instanceof Damageable) {
            ((Damageable) entity).damage(damage);
        }
    }

    public void setDamage(double damage) {
        this.damage = damage;        
    }

    public double getDamage() {
        return this.damage;
    }
}
