package com.hfturay.battleplugin.ability.abilities.damonabilities;

import org.bukkit.Color;
import org.bukkit.Particle;

import com.hfturay.battleplugin.particleservice.DirectSpec;
import com.hfturay.battleplugin.particleservice.ParticleSpec;

/**
 * The armed mine: a flat gray disc with a small red button. Played once as a zone for the whole arming time,
 * and drawn every other tick (dust lingers for a moment, so it still reads as solid).
 * Centred on the block (offset 0.5), sitting on top of it.
 */
final class LandMineParticles {
    private LandMineParticles() {}

    private static final ParticleSpec BODY = DirectSpec.of(Particle.DUST)
        .dust(1.0f, Color.GRAY)
        .shape(DirectSpec.Shape.CYLINDER)
        .radius(0.5)
        .height(0.15)
        .layers(2)
        .fill(true)
        .count(12)
        .offset(0.5, 1.0, 0.5)
        .interval(2)
        .build();

    private static final ParticleSpec BUTTON = DirectSpec.of(Particle.DUST)
        .dust(0.5f, Color.RED)
        .shape(DirectSpec.Shape.CYLINDER)
        .radius(0.2)
        .height(0.15)
        .layers(2)
        .count(6)
        .offset(0.5, 1.15, 0.5)
        .interval(2)
        .build();

    static final ParticleSpec SPEC = ParticleSpec.all(BODY, BUTTON);

    /** The blast when the mine goes off. */
    static final ParticleSpec EXPLOSION = DirectSpec.of(Particle.EXPLOSION)
        .shape(DirectSpec.Shape.POINT)
        .count(1)
        .build();
}
