package com.hfturay.battleplugin.ability.abilities.muhammadabilities;

import org.bukkit.Material;

import com.hfturay.battleplugin.visualservice.TumbleModel;
import com.hfturay.battleplugin.visualservice.VisualSpec;

/** Muhammad's block-based effects, one constant per effect. */
final class MuhammadVisuals {
    private MuhammadVisuals() {}

    /**
     * The pipe bomb: a thin, stretched iron block (0.14 wide, 0.14 tall, 0.7 long) tumbling end over end through the
     * air like the brick does. tumbling(x, y, z, wobble) is degrees per tick on each of its own axes; the wobble lets
     * every throw tumble a little differently. Set the three speeds to 0 to stop it tumbling.
     */
    static final VisualSpec PIPE_BOMB = TumbleModel.of(Material.IRON_BLOCK, 0.14, 0.14, 0.7)
        .tumbling(30, 8, 12, 0.35);
}
