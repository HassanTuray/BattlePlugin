package com.hfturay.battleplugin.ability.abilities.damonabilities;

import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.ProjectileAbility;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

/**
 * Lobs a potion that arcs downward and, where it lands, gives everyone nearby a pile of bad effects.
 * Every number comes from abilities.yml (Homebrew section).
 */
@RequiresSettings({ "cooldown", "speed", "max-ticks", "hitbox", "gravity", "duration-ticks", "amplifier", "radius" })
public class Homebrew extends ProjectileAbility {

    public Homebrew(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Homebrew");
    }

    @Override
    protected Activation onActivate() {
        this.speed = setting("speed");
        this.maxTicks = settingInt("max-ticks");
        this.hitbox = setting("hitbox");
        double gravity = setting("gravity");
        this.motionFunction = (v, t) -> v.setY(v.getY() - gravity);
        this.particles = HomebrewParticles.SPEC;
        this.callback = new HomebrewCallback(settingInt("duration-ticks"), settingInt("amplifier"), setting("radius"));

        VirtualProjectile virtualProjectile = new VirtualProjectile(
            speed,
            maxTicks,
            owner,
            hitbox,
            callback,
            particles
        );

        this.virtualProjectile = virtualProjectile;
        virtualProjectile.setMotionFunction(motionFunction);
        launch();
        return Activation.USED;
    }
}
