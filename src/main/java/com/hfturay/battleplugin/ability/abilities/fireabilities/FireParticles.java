package com.hfturay.battleplugin.ability.abilities.fireabilities;

import org.bukkit.Particle;

import com.hfturay.battleplugin.particleservice.DirectSpec;
import com.hfturay.battleplugin.particleservice.ParticleSpec;

/** All of the fire moves' particle effects, one constant per effect. */
final class FireParticles {
    private FireParticles() {}

    // ---- Blast Off ----

    /** Flames and smoke streaming from the flier's feet (behind them: the pose is lying forward). */
    static final ParticleSpec BLAST_OFF_FEET = ParticleSpec.all(
        DirectSpec.of(Particle.FLAME)
            .shape(DirectSpec.Shape.POINT)
            .count(8)
            .spread(0.2, 0.15, 0.2)
            .speed(0.03)
            .orient(true)
            .offset(0, 0.3, -0.9)
            .build(),
        DirectSpec.of(Particle.SMOKE)
            .shape(DirectSpec.Shape.POINT)
            .count(3)
            .spread(0.2, 0.15, 0.2)
            .speed(0.02)
            .orient(true)
            .offset(0, 0.3, -1.0)
            .build());
}
