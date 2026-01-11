package com.hfturay.battleplugin.ability.abilities.genericabilities;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.projectileservice.ParticleEffectCallback;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CubeEffect;


public class BrickParticleEffect implements ParticleEffectCallback{

    @Override
    public void playParticleEffect(VirtualProjectile virtualProjectile) {
        EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();
        CubeEffect brick = new CubeEffect(effectManager);
        
        brick.particle = Particle.DUST;
        brick.color = Color.ORANGE;
        brick.particles = 20;
        brick.particleSize = 0.1f;

        brick.setLocation(virtualProjectile.getLocation());
        brick.edgeLength = 0.15f;

        brick.period = 1;
        brick.iterations = 1;

        brick.outlineOnly = false;

        brick.start();
       
    }
         
}
