package com.hfturay.battleplugin.ability.abilities.damonabilities;

import org.bukkit.Color;
import org.bukkit.Particle;

import com.hfturay.battleplugin.particleservice.DirectSpec;
import com.hfturay.battleplugin.particleservice.ParticleSpec;

/**
 * An orange beam drawn in one go, plus a purple spiral that travels along it at 2.5 blocks per tick.
 * Play with ParticleService.beam(...) from the muzzle to the end point.
 */
final class LazerParticles {
    private LazerParticles() {}

    private static final ParticleSpec LINE = DirectSpec.of(Particle.DUST)
        .dust(0.5f, Color.ORANGE)
        .shape(DirectSpec.Shape.LINE)
        .count(6)
        .build();

    private static final ParticleSpec SPIRAL = DirectSpec.of(Particle.DUST)
        .dust(0.75f, Color.PURPLE)
        .shape(DirectSpec.Shape.HELIX)
        .radius(0.35)
        .turns(1.6)
        .count(6)
        .travel(2.5)
        .build();

    static final ParticleSpec BEAM = ParticleSpec.all(LINE, SPIRAL);
}
