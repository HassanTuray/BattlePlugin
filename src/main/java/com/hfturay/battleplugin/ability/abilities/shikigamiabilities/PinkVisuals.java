package com.hfturay.battleplugin.ability.abilities.shikigamiabilities;

import org.bukkit.Material;

import com.hfturay.battleplugin.visualservice.BlockModel;
import com.hfturay.battleplugin.visualservice.VisualSpec;

/** Pink!'s block-based effects, one constant per effect. Its particle effects are in PinkParticles. */
final class PinkVisuals {
    private PinkVisuals() {}

    /** How far above the bomb's middle the tip of the fuse is, before scaling; PinkParticles puts the flame there. */
    static final double FUSE_TIP = 0.47;
    static final double BOMB_SCALE = 1.2;

    /**
     * The cartoon bomb: a round black ball (a cube, the same cube tipped 45 degrees, and a wide and a tall slab, which
     * together read as round), a grey metal cap on top, a short fuse leaning out of the cap, and a white shine on its
     * front. About 0.6 blocks across after the 1.2x scale. It stays upright (level) while it flies, only turning to
     * face the way it is going. The projectile is at the middle of the ball.
     */
    static final VisualSpec BOMB = BlockModel.builder()
        .scale(BOMB_SCALE)
        .level()
        .part(Material.BLACK_CONCRETE, 0, 0, 0, 0.44, 0.44, 0.44)            // the ball
        .part(Material.BLACK_CONCRETE, 0, 0, 0, 0.44, 0.44, 0.44, 45)
        .part(Material.BLACK_CONCRETE, 0, 0, 0, 0.50, 0.32, 0.50)            // rounds out the sides
        .part(Material.BLACK_CONCRETE, 0, 0, 0, 0.32, 0.50, 0.32)            // and the top and bottom
        .part(Material.GRAY_CONCRETE, 0, 0.27, 0, 0.18, 0.08, 0.18)          // the cap
        .part(Material.LIGHT_GRAY_CONCRETE, 0, 0.32, 0, 0.12, 0.04, 0.12)    // its rim
        .part(Material.BROWN_CONCRETE, 0, 0.40, -0.02, 0.035, 0.14, 0.035, -25)   // the fuse, leaning back
        .part(Material.WHITE_CONCRETE, -0.10, 0.08, 0.26, 0.07, 0.07, 0.02)  // the shine, just in front of the ball
        .part(Material.WHITE_CONCRETE, -0.03, 0.13, 0.26, 0.035, 0.035, 0.02)
        .build();
}
