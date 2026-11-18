package com.hfturay.battleplugin.ability.abilities.genericabilities;

import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.ProjectileAbility;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

/** Throws a brick that arcs downward. Every number comes from abilities.yml (Brick section). */
@RequiresSettings({ "cooldown", "speed", "hitbox", "max-ticks", "gravity", "damage" })
public class Brick extends ProjectileAbility {

    public Brick(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Brick");
    }

    @Override
    protected Activation onActivate() {
        this.speed = setting("speed");
        this.hitbox = setting("hitbox");
        this.maxTicks = settingInt("max-ticks");
        double gravity = setting("gravity");
        this.motionFunction = (v, t) -> v.setY(v.getY() - gravity);
        this.callback = new BrickCallback(setting("damage"));     // no particles: the brick is a solid block (GenericVisuals.BRICK)

        VirtualProjectile virtualProjectile = new VirtualProjectile(
            speed,
            maxTicks,
            owner,
            hitbox,
            callback,
            particles
        );

        // The brick is a block. It is removed the moment the projectile ends, however it ends.
        virtualProjectile.setVisual(GenericVisuals.BRICK);

        this.virtualProjectile = virtualProjectile;
        virtualProjectile.setMotionFunction(motionFunction);
        launch();

        return Activation.USED;
    }
}
