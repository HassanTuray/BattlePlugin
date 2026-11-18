package com.hfturay.battleplugin.ability.abilities.muhammadabilities;

import com.hfturay.battleplugin.ability.ProjectileAbility;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

import com.hfturay.battleplugin.ability.Activation;

/** Every number comes from abilities.yml (PipeBomb section), read each time a bomb is thrown. */
@RequiresSettings({ "cooldown", "speed", "hitbox", "max-ticks", "gravity", "radius", "damage", "knockback" })
public class PipeBomb extends ProjectileAbility {
    public PipeBomb(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Pipebomb");
        this.particles = MuhammadParticles.PIPE_BOMB;
    }

    @Override
    protected Activation onActivate() {
        double gravity = setting("gravity");

        VirtualProjectile virtualProjectile = new VirtualProjectile (
            setting("speed"),
            settingInt("max-ticks"),
            owner,
            setting("hitbox"),
            new PipeBombCallBack(setting("radius"), setting("damage"), setting("knockback")),
            particles
        );

        this.virtualProjectile = virtualProjectile;
        virtualProjectile.setMotionFunction((v, t) -> v.setY(v.getY() - gravity));
        launch();
        return Activation.USED;
    }

}
