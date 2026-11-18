package com.hfturay.battleplugin.ability.abilities.damonabilities;

import org.bukkit.Particle;

import com.hfturay.battleplugin.particleservice.DirectSpec;
import com.hfturay.battleplugin.particleservice.ParticleSpec;

/** Firework sparks flickering around the player for the length of the buff. */
final class SwitchOnALazerParticles {
    private SwitchOnALazerParticles() {}

    static final ParticleSpec SPARKS = DirectSpec.of(Particle.FIREWORK)
        .shape(DirectSpec.Shape.POINT)
        .count(1)
        .spread(0.6, 0.6, 0.6)
        .offset(0, 1, 0)
        .build();
}
