package com.hfturay.battleplugin.ability.abilities.genericabilities;

import org.bukkit.Material;
import org.bukkit.Particle;

import com.hfturay.battleplugin.particleservice.DirectSpec;
import com.hfturay.battleplugin.particleservice.ParticleSpec;

/**
 * The generic moves' particle effects, one constant per effect. (The jump puff shared with the Damon and Nathan
 * leaps is still in LeapParticles; it will join this file when the generic kit is reorganised.)
 */
final class GenericParticles {
    private GenericParticles() {}

    // ---- Brick ----

    /**
     * Orange concrete crumbs flying out where the brick lands: the same fragments you see when you break the block.
     * count = how many crumbs, spread = how far apart they start, speed = how hard they fly outward.
     */
    static final ParticleSpec BRICK_IMPACT = DirectSpec.of(Particle.BLOCK)
        .block(Material.ORANGE_CONCRETE.createBlockData())
        .shape(DirectSpec.Shape.POINT)
        .count(30)
        .spread(0.2, 0.2, 0.2)
        .speed(0.12)
        .build();
}
