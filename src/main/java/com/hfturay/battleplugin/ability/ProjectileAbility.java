package com.hfturay.battleplugin.ability;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.particleservice.ParticleSpec;
import com.hfturay.battleplugin.projectileservice.MotionFunction;
import com.hfturay.battleplugin.projectileservice.ProjectileHitCallback;
import com.hfturay.battleplugin.projectileservice.ProjectileService;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

public abstract class ProjectileAbility extends Ability {
    protected double hitbox = 0;
    protected double speed = 0;
    protected int maxTicks = 0;
    protected VirtualProjectile virtualProjectile;
    protected ProjectileHitCallback callback;
    protected MotionFunction motionFunction;

    /** What the projectile looks like in flight. Plain data, safe to share; null means no visuals. */
    protected ParticleSpec particles;

    public ProjectileAbility(BattlePlayer owner) {
        super(owner);
    }

    protected void launch() {
        BattlePlugin.getInstance().getProjectileService().add(virtualProjectile);
    }
}
