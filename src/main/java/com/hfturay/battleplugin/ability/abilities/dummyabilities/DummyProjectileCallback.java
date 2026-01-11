package com.hfturay.battleplugin.ability.abilities.dummyabilities;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.projectileservice.ProjectileHitCallback;
import com.hfturay.battleplugin.projectileservice.ProjectileHitResult;

public class DummyProjectileCallback implements ProjectileHitCallback{

    @Override
    public void onHit(ProjectileHitResult result, BattlePlayer owner) {
        if (result.hasHitEntity()) {
            Entity entity = result.getHitEntity();
        }

        if (result.hasHitBlock()) {
            Block block = result.getHitBlock();
        }
    }
    
}
