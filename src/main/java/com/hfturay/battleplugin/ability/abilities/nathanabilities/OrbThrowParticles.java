package com.hfturay.battleplugin.ability.abilities.nathanabilities;

import org.bukkit.Color;
import org.bukkit.Particle;

import com.hfturay.battleplugin.particleservice.EffectLibSpec;
import com.hfturay.battleplugin.particleservice.ParticleSpec;

import de.slikey.effectlib.effect.AtomEffect;

/** A purple atom (nucleus + two orbitals) that follows the orb. A fresh effect per orb, drawn every 2 ticks. */
final class OrbThrowParticles {
    private OrbThrowParticles() {}

    static final ParticleSpec SPEC = EffectLibSpec.of((effects, ctx) -> {
        AtomEffect atom = new AtomEffect(effects);
        atom.particleOrbital = Particle.DUST;
        atom.colorOrbital = Color.FUCHSIA;
        atom.particleNucleus = Particle.DUST;
        atom.colorNucleus = Color.PURPLE;
        atom.period = 2;
        atom.iterations = ctx.iterationsFor(atom.period, 1);
        atom.radiusNucleus = 0.1f;
        atom.radius = 0.3f;
        atom.particleSize = 0.5f;
        atom.particlesNucleus = 20;
        atom.particlesOrbital = 20;
        atom.orbitals = 2;
        atom.angularVelocity = Math.PI / 32;
        atom.orient = true;
        return atom;
    });
}
