package com.hfturay.battleplugin.ability.abilities.shikigamiabilities;

import org.bukkit.Particle;

import com.hfturay.battleplugin.particleservice.DirectSpec;
import com.hfturay.battleplugin.particleservice.ParticleSpec;

/** Pink!'s particle effects, one constant per effect. Its block-based effects are in PinkVisuals. */
final class PinkParticles {
    private PinkParticles() {}

    /**
     * The lit fuse: a couple of small flames at the tip of the bomb's fuse, every tick it flies. In world space
     * (orient false), so the flame stays on top however the bomb is travelling.
     */
    static final ParticleSpec BOMB_FUSE = DirectSpec.of(Particle.FLAME)
        .shape(DirectSpec.Shape.POINT)
        .count(2)
        .spread(0.03, 0.03, 0.03)
        .speed(0.01)
        .offset(0, PinkVisuals.FUSE_TIP * PinkVisuals.BOMB_SCALE, 0)
        .orient(false)
        .build();
}
