package com.hfturay.battleplugin.ability.abilities.damonabilities;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.projectileservice.ParticleEffectCallback;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.SphereEffect;

public class HomebrewParticleEffect implements ParticleEffectCallback {
    private double brewBodyRadius = 0.05;
    private int brewBodyNumParticles = 50;

    private EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();
    private SphereEffect brewBody = new SphereEffect(effectManager);

    @Override
    public void playParticleEffect(VirtualProjectile virtualProjectile) {
        Location projectileLocation = virtualProjectile.getLocation().clone();

        brewBody.setLocation(projectileLocation);
        brewBody.particle = Particle.DUST;
        brewBody.color = Color.GREEN;
        brewBody.radius = brewBodyRadius;
        brewBody.particles = brewBodyNumParticles;
        brewBody.iterations = 1;
        brewBody.particleData = 0.01f;
        brewBody.period = 1;
        
        brewBody.start();
    }
    
}
