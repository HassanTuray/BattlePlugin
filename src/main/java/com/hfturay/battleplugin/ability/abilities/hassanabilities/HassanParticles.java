package com.hfturay.battleplugin.ability.abilities.hassanabilities;

import org.bukkit.Color;
import org.bukkit.Particle;

import com.hfturay.battleplugin.particleservice.DirectSpec;
import com.hfturay.battleplugin.particleservice.ParticleSpec;

/** All of Hassan's particle effects, one constant per effect. */
final class HassanParticles {
    private HassanParticles() {}

    // ---- Malware ----

    /** How long the infection line stays up, in ticks (10 = half a second). It follows both ends while it does. */
    static final int MALWARE_LINK_TICKS = 10;

    /**
     * The infection line: a green line from whoever passed Malware on (the caster, or an infected entity it spread
     * from) to the one it just infected, the same green as the virus. Play with ParticleService.beam(...).
     */
    static final ParticleSpec MALWARE_LINK = DirectSpec.of(Particle.DUST)
        .dust(0.8f, Color.fromRGB(70, 220, 70))
        .shape(DirectSpec.Shape.LINE)
        .count(5)
        .build();

    // ---- Flying Raijin ----

    /** The marking arrow in flight: a few bright sparks trailing just behind the ring at the end of the kunai. */
    static final ParticleSpec RAIJIN_ARROW = DirectSpec.of(Particle.END_ROD)
        .shape(DirectSpec.Shape.POINT)
        .count(2)
        .spread(0.05, 0.05, 0.05)
        .orient(true)
        .offset(0, 0, -0.8)
        .build();

    /** A faint white puff trailing the soccer ball. */
    static final ParticleSpec BALL_TRAIL = DirectSpec.of(Particle.CLOUD)
        .shape(DirectSpec.Shape.POINT)
        .count(1)
        .spread(0.05, 0.05, 0.05)
        .build();

    /** The poof of white smoke left behind where the caster was standing. */
    static final ParticleSpec RAIJIN_POOF = ParticleSpec.all(
        DirectSpec.of(Particle.CLOUD)
            .shape(DirectSpec.Shape.POINT)
            .count(30)
            .spread(0.35, 0.5, 0.35)
            .offset(0, 1, 0)
            .build(),
        DirectSpec.of(Particle.DUST)
            .dust(1.6f, Color.WHITE)
            .shape(DirectSpec.Shape.POINT)
            .count(25)
            .spread(0.4, 0.6, 0.4)
            .offset(0, 1, 0)
            .build());
}
