package com.hfturay.battleplugin.ability.abilities.muhammadabilities;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.projectileservice.ParticleEffectCallback;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.WaveEffect;

public class MeltdownOfWavesParticleEffect implements ParticleEffectCallback{

    @Override
    public void playParticleEffect(VirtualProjectile virtualProjectile) {
        Location location = virtualProjectile.getLocation();
        EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();
        WaveEffect wave = new WaveEffect(effectManager);

        wave.autoOrient = true;
        // wave.velocity = location.clone().getDirection();
        wave.setLocation(location.clone().add(0, -0.75, 0));
        wave.mainParticle = Particle.DUST;
        wave.cloudParticle = Particle.DUST;
        wave.color = Color.BLACK;
        wave.cloudColor = Color.YELLOW;

        wave.period = 1;
        wave.iterations = 1;

        wave.height = 4;
        wave.width = 3;
        wave.heightBack = 0.5F;
        wave.rows = 20;
        wave.lengthFront = 1.5F;
        wave.lengthBack = 4;
        wave.depthFront = 1.5F;

        wave.particleSize = 1.2F;
        wave.particlesFront = 15;
        wave.particlesBack = 15;
        
        wave.start();
    }
    
}
