package com.hfturay.battleplugin.ability.abilities.nathanabilities;

import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.projectileservice.ProjectileHitCallback;
import com.hfturay.battleplugin.projectileservice.ProjectileHitResult;

public class OrbThrowCallback implements ProjectileHitCallback {
    private double damage;

    /** @param damage health points per hit, already read from abilities.yml (the repeater raises it when the orb starts homing) */
    public OrbThrowCallback(double damage) {
        this.damage = damage;
    }

    @Override
    public void onHit(ProjectileHitResult result, BattlePlayer owner) {
        Entity entity = result.getHitEntity();

        if (entity == null) {
            return;
        }

        if (entity instanceof Damageable) {
            BattlePlugin.getInstance().getDamageService().damage(owner, entity, damage);
        }
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public double getDamage() {
        return this.damage;
    }
}
