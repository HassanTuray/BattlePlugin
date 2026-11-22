package com.hfturay.battleplugin.ability.abilities.muhammadabilities;

import org.bukkit.Location;
import org.bukkit.util.Vector;

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

        virtualProjectile.setVisual(MuhammadVisuals.PIPE_BOMB);     // removed the moment the projectile ends

        this.virtualProjectile = virtualProjectile;
        virtualProjectile.setMotionFunction((v, t) -> v.setY(v.getY() - gravity));
        launch();
        return Activation.USED;
    }

    /**
     * A pipe bomb (same look and blast) thrown from anywhere, for others to launch, like Pink! does. The caller gives
     * every number; it falls by gravity every tick. Add it to the ProjectileService to throw it.
     */
    public static VirtualProjectile thrownFrom(Location start, Vector direction, BattlePlayer owner, double speed, int maxTicks,
                                               double hitbox, double gravity, double radius, double damage, double knockback) {
        VirtualProjectile bomb = new VirtualProjectile(start, direction, speed, maxTicks, owner, hitbox,
            new PipeBombCallBack(radius, damage, knockback), MuhammadParticles.PIPE_BOMB);
        bomb.setVisual(MuhammadVisuals.PIPE_BOMB);
        bomb.setMotionFunction((v, t) -> v.setY(v.getY() - gravity));
        return bomb;
    }
}
