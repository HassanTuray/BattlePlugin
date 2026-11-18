package com.hfturay.battleplugin.ability.abilities.waterabilities;

import org.bukkit.Particle;

import com.hfturay.battleplugin.particleservice.DirectSpec;
import com.hfturay.battleplugin.particleservice.ParticleSpec;

/** All of the water moves' particle effects, one constant per effect. */
final class WaterParticles {
    private WaterParticles() {}

    // ---- Water Prison ----

    /** The splash where a water block hits its target. */
    static final ParticleSpec PRISON_HIT = DirectSpec.of(Particle.SPLASH)
        .shape(DirectSpec.Shape.POINT)
        .count(40)
        .spread(0.4, 0.5, 0.4)
        .speed(0.1)
        .build();

    // ---- Water Barrage ----

    /** The splash where a blast of water hits a player or a wall. */
    static final ParticleSpec BARRAGE_HIT = DirectSpec.of(Particle.SPLASH)
        .shape(DirectSpec.Shape.POINT)
        .count(30)
        .spread(0.3, 0.3, 0.3)
        .speed(0.1)
        .build();

    // ---- Water Prison (continued) ----

    /** The burst as the cube of water forms around the target. */
    static final ParticleSpec PRISON_FORM = DirectSpec.of(Particle.SPLASH)
        .shape(DirectSpec.Shape.POINT)
        .count(120)
        .spread(1.2, 1.2, 1.2)
        .speed(0.15)
        .build();
}
