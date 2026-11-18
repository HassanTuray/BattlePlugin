package com.hfturay.battleplugin.ability.abilities.aliabilities;

import org.bukkit.Material;

import com.hfturay.battleplugin.visualservice.BeamModel;
import com.hfturay.battleplugin.visualservice.BlockModel;
import com.hfturay.battleplugin.visualservice.VisualSpec;

/** All of Ali's block-based effects, one constant per effect. His particle effects are in AliParticles. */
final class AliVisuals {
    private AliVisuals() {}

    // ---- Pharah Rocket ----

    /**
     * The rocket's body, made of stretched blocks. The projectile is at the nose tip (z = 0) and the rocket extends
     * behind it, so it flies nose first. Sizes are in blocks before the 1.6x scale, so the rocket is about 2 blocks long.
     * The flame is still a particle (AliParticles.PHARAH_ROCKET_FLAME), sitting just behind the engine.
     */
    static final VisualSpec PHARAH_ROCKET_BODY = BlockModel.builder()
        .scale(1.6)
        .part(Material.RED_CONCRETE,   0, 0, -0.15,  0.30, 0.30, 0.30)    // nose
        .part(Material.WHITE_CONCRETE, 0, 0, -0.70,  0.36, 0.36, 0.80)    // body
        .part(Material.GRAY_CONCRETE,  0, 0, -1.175, 0.26, 0.26, 0.15)    // engine
        .part(Material.RED_CONCRETE,   0, 0, -0.95,  0.05, 0.80, 0.35)    // fin, upright
        .part(Material.RED_CONCRETE,   0, 0, -0.95,  0.80, 0.05, 0.35)    // fin, flat
        .build();

    // ---- Laser ----

    /**
     * The laser beam: one thin light blue concrete block stretched from the muzzle to wherever the beam ends.
     * 0.12 is how thick it is, in blocks. It rolls around its own length, 40 degrees every tick (about 1.7 turns
     * a second); change the number in spinning(...) to go faster or slower, or remove it to stop the spin.
     * (The purple swirl around it is AliParticles.LASER_SWIRL.)
     */
    static final VisualSpec LASER_BODY = BeamModel.of(Material.LIGHT_BLUE_CONCRETE, 0.12).spinning(40);
}
