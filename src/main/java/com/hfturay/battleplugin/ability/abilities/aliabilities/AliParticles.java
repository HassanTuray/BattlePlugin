package com.hfturay.battleplugin.ability.abilities.aliabilities;

import org.bukkit.Color;
import org.bukkit.Particle;

import com.hfturay.battleplugin.particleservice.DirectSpec;
import com.hfturay.battleplugin.particleservice.ParticleSpec;

/** All of Ali's particle effects, one constant per effect. His block-based effects are in AliVisuals. */
final class AliParticles {
    private AliParticles() {}

    // ---- Pharah Rocket ----

    /**
     * The rocket's exhaust flame, 2.1 blocks behind the nose, just past the engine. orient(true) makes "behind"
     * follow the rocket's direction of travel. (The rocket's body is blocks: see AliVisuals.PHARAH_ROCKET_BODY.)
     */
    static final ParticleSpec PHARAH_ROCKET_FLAME = DirectSpec.of(Particle.FLAME)
        .shape(DirectSpec.Shape.POINT)
        .count(4)
        .spread(0.05, 0.05, 0.05)
        .orient(true)
        .offset(0, 0, -2.1)
        .build();

    /** The explosion puff where the rocket lands. */
    static final ParticleSpec PHARAH_ROCKET_EXPLOSION = DirectSpec.of(Particle.EXPLOSION)
        .shape(DirectSpec.Shape.POINT)
        .count(1)
        .build();

    // ---- Laser ----

    /**
     * Light purple dust swirling around the laser beam (the beam itself is AliVisuals.LASER_BODY). A spiral is
     * redrawn every tick between the muzzle and the end of the beam and turns as it goes.
     * radius = how far from the beam it swirls, turns = full turns per block, count = particles per block.
     * Two shades of purple alternate along it.
     */
    static final ParticleSpec LASER_SWIRL = DirectSpec.of(Particle.DUST)
        .dust(0.8f, Color.fromRGB(200, 160, 255), Color.fromRGB(225, 200, 255))
        .shape(DirectSpec.Shape.HELIX)
        .radius(0.35)
        .turns(0.5)
        .count(4)
        .build();

    // ---- Personal Space (BigBang) ----

    /** Personal Space: a white sphere expanding from the caster. */
    static final ParticleSpec BIG_BANG_BURST = DirectSpec.of(Particle.DUST)
        .dust(1.5f, Color.WHITE)
        .shape(DirectSpec.Shape.SPHERE)
        .count(80)
        .expanding(1.0, 5.0, 10)
        .offset(0, 1, 0)
        .build();

    // ---- Cheese Sandwich and Tangerines ----

    /** Cheese Sandwich: a quick black shield flash that follows the caster. */
    static final ParticleSpec SHIELD = DirectSpec.of(Particle.DUST)
        .dust(3.5f, Color.BLACK)
        .shape(DirectSpec.Shape.SPHERE)
        .count(40)
        .radius(2.1)
        .lifetime(5)
        .offset(0, 1, 0)
        .build();
}
