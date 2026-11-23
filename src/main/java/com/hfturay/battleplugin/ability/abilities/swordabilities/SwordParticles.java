package com.hfturay.battleplugin.ability.abilities.swordabilities;

import org.bukkit.Particle;

import com.hfturay.battleplugin.particleservice.DirectSpec;
import com.hfturay.battleplugin.particleservice.ParticleSpec;

/** Sword techniques' particle effects, one constant per effect. Their block-based effects are in SwordVisuals. */
final class SwordParticles {
    private SwordParticles() {}

    /** A thin trail of white sparkles shed along the height of the Damascus Slash as it flies. */
    static final ParticleSpec DAMASCUS_SLASH_TRAIL = DirectSpec.of(Particle.END_ROD)
        .shape(DirectSpec.Shape.POINT)
        .count(3)
        .spread(0.05, 0.8, 0.05)
        .offset(-0.15, 0, -0.2)
        .orient(true)
        .build();

    /** A sweep and a crit burst where the slash connects. */
    static final ParticleSpec DAMASCUS_SLASH_HIT = DirectSpec.of(Particle.SWEEP_ATTACK)
        .shape(DirectSpec.Shape.POINT)
        .count(2)
        .spread(0.3, 0.6, 0.3)
        .offset(0, 1, 0)
        .build();
}
