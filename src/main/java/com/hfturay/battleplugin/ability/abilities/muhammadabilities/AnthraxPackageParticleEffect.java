package com.hfturay.battleplugin.ability.abilities.muhammadabilities;

import org.bukkit.Color;
import org.bukkit.Particle;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.projectileservice.ParticleEffectCallback;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.SphereEffect;

public class AnthraxPackageParticleEffect implements ParticleEffectCallback {
    private EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();

    @Override
    public void playParticleEffect(VirtualProjectile virtualProjectile) {
        SphereEffect dragonBallBlack = new SphereEffect(effectManager);
        SphereEffect dragonBallGreen = new SphereEffect(effectManager);
       
        dragonBallBlack.setLocation(virtualProjectile.getLocation());

        dragonBallBlack.iterations = 2;
        dragonBallBlack.period = 1;

        dragonBallBlack.particle = Particle.DUST; 
        dragonBallBlack.color = Color.BLACK;
        dragonBallBlack.particles = 25;
        dragonBallBlack.particleSize = 0.2f;

        dragonBallBlack.radius = 0.1;

        dragonBallGreen.setLocation(virtualProjectile.getLocation());

        dragonBallGreen.iterations = 2;
        dragonBallGreen.period = 1;

        dragonBallGreen.particle = Particle.DUST; 
        dragonBallGreen.color = Color.LIME;
        dragonBallGreen.particles = 25;
        dragonBallGreen.particleSize = 0.2f;

        dragonBallGreen.radius = 0.1;

        dragonBallBlack.start();
        dragonBallGreen.start();
    } 
}
