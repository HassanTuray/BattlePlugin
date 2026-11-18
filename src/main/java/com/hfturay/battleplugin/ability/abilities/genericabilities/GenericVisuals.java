package com.hfturay.battleplugin.ability.abilities.genericabilities;

import org.bukkit.Material;

import com.hfturay.battleplugin.visualservice.TumbleModel;
import com.hfturay.battleplugin.visualservice.VisualSpec;

/** All of the generic moves' block-based effects, one constant per effect. */
final class GenericVisuals {
    private GenericVisuals() {}

    // ---- Brick ----

    /**
     * The brick: one orange concrete block shaped like a brick, 0.24 wide, 0.15 tall and 0.5 long (about the
     * proportions of a real one), tumbling through the air.
     *
     * tumbling(x, y, z, wobble) is degrees per tick on each of the brick's own axes (20 ticks = 1 second):
     *   x 22 = end over end, about 1.2 turns a second, the main motion of a thrown brick
     *   y  6 = a slow flat spin
     *   z 10 = a roll along its length
     * wobble 0.35 lets each of those speeds drift between 65% and 135% of itself, smoothly and differently for
     * every throw, so no two bricks tumble the same way. Set the three speeds to 0 to stop it tumbling.
     */
    static final VisualSpec BRICK = TumbleModel.of(Material.ORANGE_CONCRETE, 0.24, 0.15, 0.5)
        .tumbling(22, 6, 10, 0.35);
}
