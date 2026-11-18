package com.hfturay.battleplugin.ability.abilities.airabilities;

import org.bukkit.Particle;

import com.hfturay.battleplugin.particleservice.DirectSpec;
import com.hfturay.battleplugin.particleservice.ParticleSpec;

/**
 * The air moves' particle effects. Air Dome's swirl is AirDomeSpec; everything here uses the same two particles it
 * does (cloud puffs and the odd large gust), so all the air moves look like one element.
 */
final class AirParticles {
    private AirParticles() {}

    // ---- Sonic Boom ----

    /** The ball of wind the blast is made of while it flies (1 block radius, like its hitbox). */
    static final ParticleSpec BOOM_BODY = ParticleSpec.all(
        DirectSpec.of(Particle.CLOUD)
            .shape(DirectSpec.Shape.SPHERE)
            .radius(1.0)
            .count(18)
            .speed(0.01)
            .build(),
        DirectSpec.of(Particle.GUST)
            .shape(DirectSpec.Shape.POINT)
            .count(1)
            .interval(3)
            .build());

    /** The burst where the blast hits a player or a wall. */
    static final ParticleSpec BOOM_HIT = ParticleSpec.all(
        DirectSpec.of(Particle.CLOUD)
            .shape(DirectSpec.Shape.POINT)
            .count(40)
            .spread(0.5, 0.5, 0.5)
            .speed(0.15)
            .build(),
        DirectSpec.of(Particle.GUST)
            .shape(DirectSpec.Shape.POINT)
            .count(1)
            .build());
}
