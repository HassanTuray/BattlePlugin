package com.hfturay.battleplugin.ability.abilities.genericabilities;

import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.projectileservice.ProjectileHitCallback;
import com.hfturay.battleplugin.projectileservice.ProjectileHitResult;

public class BrickCallback implements ProjectileHitCallback{

    @Override
    public void onHit(ProjectileHitResult result, BattlePlayer owner) {
        Entity entity = result.getHitEntity();

        if (entity instanceof LivingEntity) {
            ((Damageable) entity).damage(3);
        }
    }
}
