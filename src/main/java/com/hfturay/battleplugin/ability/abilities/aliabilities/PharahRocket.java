package com.hfturay.battleplugin.ability.abilities.aliabilities;

import com.hfturay.battleplugin.ability.ProjectileAbility;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

import com.hfturay.battleplugin.ability.Activation;

/** Every number comes from abilities.yml (PharahRocket section), read each time a rocket is fired. */
@RequiresSettings({ "cooldown", "speed", "max-ticks", "hitbox", "radius", "damage", "horizontal-force", "lift" })
public class PharahRocket extends ProjectileAbility {
    public PharahRocket(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Pharah Rocket");
        this.particles = AliParticles.PHARAH_ROCKET_FLAME;
    }

    @Override
    protected Activation onActivate() {
        VirtualProjectile virtualProjectile = new VirtualProjectile (
            setting("speed"),
            settingInt("max-ticks"),
            owner,
            setting("hitbox"),
            new PharahRocketCallback(
                setting("radius"),
                setting("damage"),
                setting("horizontal-force"),
                setting("lift")),
            particles
        );

        // The rocket's body is blocks. They are removed the moment the projectile ends, however it ends.
        virtualProjectile.setVisual(AliVisuals.PHARAH_ROCKET_BODY);

        this.virtualProjectile = virtualProjectile;

        launch();
        return Activation.USED;
    }
}
