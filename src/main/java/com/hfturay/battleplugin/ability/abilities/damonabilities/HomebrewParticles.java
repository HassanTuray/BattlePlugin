package com.hfturay.battleplugin.ability.abilities.damonabilities;

import org.bukkit.Color;
import org.bukkit.Particle;

import com.hfturay.battleplugin.particleservice.DirectSpec;
import com.hfturay.battleplugin.particleservice.ParticleSpec;

/** A tight green ball of dust following the brew. */
final class HomebrewParticles {
    private HomebrewParticles() {}

    static final ParticleSpec SPEC = DirectSpec.of(Particle.DUST)
        .dust(1.0f, Color.GREEN)
        .shape(DirectSpec.Shape.POINT)
        .count(12)
        .spread(0.05, 0.05, 0.05)
        .build();

    /** A green ring that sweeps out to the full 5-block splash radius over 10 ticks. */
    static final ParticleSpec IMPACT = DirectSpec.of(Particle.DUST)
        .dust(1.2f, Color.GREEN)
        .shape(DirectSpec.Shape.RING)
        .count(36)
        .expanding(0.1, 5.0, 10)
        .offset(0, 0.65, 0)
        .build();
}
