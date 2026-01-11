package com.hfturay.battleplugin.projectileservice;

import com.hfturay.battleplugin.battleplayer.BattlePlayer;

public interface ProjectileHitCallback {
    void onHit(ProjectileHitResult result, BattlePlayer owner);
}
