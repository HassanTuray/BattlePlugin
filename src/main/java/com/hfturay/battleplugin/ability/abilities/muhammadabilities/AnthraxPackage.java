package com.hfturay.battleplugin.ability.abilities.muhammadabilities;

import com.hfturay.battleplugin.ability.ProjectileAbility;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

import com.hfturay.battleplugin.ability.Activation;

/** Every number comes from abilities.yml (AnthraxPackage section), read each time a package is thrown. */
@RequiresSettings({ "cooldown", "speed", "hitbox", "max-ticks", "gravity",
    "duration-ticks", "interval-ticks", "radius", "x-bound", "y-bound", "z-bound", "damage",
    "effect-ticks", "slow-amplifier", "poison-amplifier", "wither-amplifier",
    "zombie-count", "zombie-health", "zombie-lifetime-ticks", "zombie-resistance" })
public class AnthraxPackage extends ProjectileAbility {

    /** Everything the cloud and its zombies need once the package lands. */
    public record Cloud(int durationTicks, int intervalTicks, double radius, double xBound, double yBound, double zBound,
                        double damage, int effectTicks, int slowAmplifier, int poisonAmplifier, int witherAmplifier,
                        int zombieCount, double zombieHealth, int zombieLifetimeTicks, int zombieResistance) { }

    public AnthraxPackage(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Anthrax Package");
        this.particles = MuhammadParticles.ANTHRAX_PACKAGE;
    }

    @Override
    protected Activation onActivate() {
        double gravity = setting("gravity");

        Cloud cloud = new Cloud(
            settingInt("duration-ticks"),
            Math.max(1, settingInt("interval-ticks")),     // 0 would divide by zero
            setting("radius"),
            setting("x-bound"), setting("y-bound"), setting("z-bound"),
            setting("damage"),
            settingInt("effect-ticks"),
            settingInt("slow-amplifier"), settingInt("poison-amplifier"), settingInt("wither-amplifier"),
            settingInt("zombie-count"), setting("zombie-health"), settingInt("zombie-lifetime-ticks"), settingInt("zombie-resistance"));

        VirtualProjectile virtualProjectile = new VirtualProjectile (
            setting("speed"),
            settingInt("max-ticks"),
            owner,
            setting("hitbox"),
            new AnthraxPackageCallBack(this, cloud),
            particles
        );

        this.virtualProjectile = virtualProjectile;

        virtualProjectile.setMotionFunction((v, t) -> v.setY(v.getY() - gravity));
        launch();
        return Activation.USED;
    }

}
