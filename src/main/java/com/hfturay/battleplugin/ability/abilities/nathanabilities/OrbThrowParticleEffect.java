package com.hfturay.battleplugin.ability.abilities.nathanabilities;

import org.bukkit.Color;
import org.bukkit.Particle;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.projectileservice.ParticleEffectCallback;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.AtomEffect;

public class OrbThrowParticleEffect implements ParticleEffectCallback {

    @Override
    public void playParticleEffect(VirtualProjectile virtualProjectile) {
        EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();
        AtomEffect effect = new AtomEffect(effectManager);

        effect.setLocation(virtualProjectile.getLocation());

        effect.particleOrbital = Particle.DUST;
        effect.colorOrbital = Color.FUCHSIA;
        effect.particleNucleus = Particle.DUST;
        effect.colorNucleus = Color.PURPLE; 

        effect.period = 1;
        effect.iterations = 3;

        effect.radiusNucleus = 0.1f;
        effect.radius = 0.3f;

        effect.particleSize = 0.5f;

        effect.particlesNucleus = 20;
        effect.particlesOrbital = 20;
        effect.orbitals = 2;

        effect.angularVelocity = Math.PI / 32;
        effect.orient = true;

        effect.start();
    }
}
