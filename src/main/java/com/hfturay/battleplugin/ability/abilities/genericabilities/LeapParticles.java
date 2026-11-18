package com.hfturay.battleplugin.ability.abilities.genericabilities;

import org.bukkit.Particle;

import com.hfturay.battleplugin.particleservice.DirectSpec;
import com.hfturay.battleplugin.particleservice.ParticleSpec;

/** The dust puff at a player's feet when they launch. Shared by Leap, Disengage and Strong Leap. */
public final class LeapParticles {
    private LeapParticles() {}

    public static final ParticleSpec SPEC = ParticleSpec.all(
        DirectSpec.of(Particle.CLOUD)
            .shape(DirectSpec.Shape.POINT)
            .count(10)
            .spread(0.35, 0.08, 0.35)
            .speed(0.05)
            .offset(0, 0.1, 0)
            .build(),
        DirectSpec.of(Particle.SMOKE)
            .shape(DirectSpec.Shape.POINT)
            .count(8)
            .spread(0.3, 0.08, 0.3)
            .speed(0.02)
            .offset(0, 0.1, 0)
            .build());
}
