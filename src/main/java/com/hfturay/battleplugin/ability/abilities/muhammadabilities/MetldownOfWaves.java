package com.hfturay.battleplugin.ability.abilities.muhammadabilities;

import com.hfturay.battleplugin.ability.ProjectileAbility;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

import com.hfturay.battleplugin.ability.Activation;

/** Every number comes from abilities.yml (MeltdownOfWaves section), read each time the wave is fired. */
@RequiresSettings({ "cooldown", "speed", "hitbox", "max-ticks", "damage", "lift", "silence-ticks",
    "front-offset", "back-reach", "pull-gain", "max-push-speed" })
public class MetldownOfWaves extends ProjectileAbility {
    public MetldownOfWaves(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Meltdown of Waves");
        this.particles = MuhammadParticles.MELTDOWN_WAVE;
    }

    @Override
    protected Activation onActivate() {
        MeltdownOfWavesCallback.Settings push = new MeltdownOfWavesCallback.Settings(
            setting("damage"),
            setting("lift"),
            settingInt("silence-ticks"),
            setting("front-offset"),
            setting("back-reach"),
            setting("pull-gain"),
            setting("max-push-speed"));

        VirtualProjectile virtualProjectile = new VirtualProjectile (
            setting("speed"),
            settingInt("max-ticks"),
            owner,
            setting("hitbox"),
            new MeltdownOfWavesCallback(push),
            particles
        );

        this.virtualProjectile = virtualProjectile;

        virtualProjectile.setStopOnHitPlayer(false);
        virtualProjectile.setStopOnHitBlock(false);
        virtualProjectile.setHitBlocks(false);

        launch();
        return Activation.USED;
    }
}
