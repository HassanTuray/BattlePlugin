package com.hfturay.battleplugin.ability.abilities.swordabilities;

import org.bukkit.Material;

import com.hfturay.battleplugin.visualservice.BlockModel;
import com.hfturay.battleplugin.visualservice.VisualSpec;

/** Sword techniques' block-based effects, one constant per effect. Their particle effects are in SwordParticles. */
final class SwordVisuals {
    private SwordVisuals() {}

    /** The Damascus Slash crescent: how tall it is (blocks, tip to tip, before scaling) is about 2 * RADIUS * sin(SPAN). */
    private static final double RADIUS = 1.1;
    private static final double SPAN = 75;          // degrees either side of the middle
    private static final int PIECES = 13;
    private static final double MAX_THICKNESS = 0.2;
    private static final double MIN_THICKNESS = 0.04;
    private static final double TIP_SWEEP = 0.35;   // how far back the tips trail behind the middle

    /**
     * Damascus Slash: a white crescent standing upright and facing the way it flies, like the trace a sword leaves on a
     * vertical swing. It is thickest in the middle and thins to points at both tips, and the tips trail back a little.
     * The projectile is at the middle of the arc. About 2.1 blocks from tip to tip.
     *
     * The arc is PIECES short white blocks around a circle: each one sits on the circle and is leaned sideways (roll) to
     * run along it.
     */
    static final VisualSpec DAMASCUS_SLASH = buildSlash();

    private static VisualSpec buildSlash() {
        BlockModel.Builder slash = BlockModel.builder();
        double step = 2 * SPAN / (PIECES - 1);
        double chord = 2 * RADIUS * Math.sin(Math.toRadians(step / 2)) + 0.05;     // a little overlap, so no gaps
        for (int i = 0; i < PIECES; i++) {
            double angle = -SPAN + i * step;
            double radians = Math.toRadians(angle);
            double x = RADIUS * Math.cos(radians) - RADIUS;            // the middle at the projectile, the tips curling to -x
            double y = RADIUS * Math.sin(radians);
            double z = -TIP_SWEEP * (1 - Math.cos(radians)) / (1 - Math.cos(Math.toRadians(SPAN)));
            double edge = Math.abs(angle) / SPAN;                      // 0 in the middle, 1 at a tip
            double thickness = MIN_THICKNESS + (MAX_THICKNESS - MIN_THICKNESS) * (1 - edge * edge);
            slash.part(Material.WHITE_CONCRETE, x, y, z, thickness, chord, 0.08, 0, angle);
        }
        return slash.build();
    }
}
