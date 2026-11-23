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
    private static final double WIDTH = 0.1;        // side to side

    /**
     * Damascus Slash: a white crescent standing upright, its peak (the middle of the arc) leading the way it flies and
     * both tips curving back behind it, like the trace a sword leaves on a vertical swing. It is thickest in the middle
     * and thins to points at both tips. The projectile is at the peak. About 2.1 blocks from tip to tip.
     *
     * The arc is PIECES short white blocks around a circle in the up-forward plane: each one sits on the circle and is
     * tipped (tilt) to run along it.
     */
    static final VisualSpec DAMASCUS_SLASH = buildSlash();

    private static VisualSpec buildSlash() {
        BlockModel.Builder slash = BlockModel.builder();
        double step = 2 * SPAN / (PIECES - 1);
        double chord = 2 * RADIUS * Math.sin(Math.toRadians(step / 2)) + 0.05;     // a little overlap, so no gaps
        for (int i = 0; i < PIECES; i++) {
            double angle = -SPAN + i * step;                           // 0 at the peak, positive going up
            double radians = Math.toRadians(angle);
            double y = RADIUS * Math.sin(radians);
            double z = RADIUS * Math.cos(radians) - RADIUS;            // the peak at the projectile, the tips curving back
            double edge = Math.abs(angle) / SPAN;                      // 0 at the peak, 1 at a tip
            double thickness = MIN_THICKNESS + (MAX_THICKNESS - MIN_THICKNESS) * (1 - edge * edge);
            // tipped back by the angle, so each piece runs along the curve (a positive tilt would lean it forward)
            slash.part(Material.WHITE_CONCRETE, 0, y, z, WIDTH, chord, thickness, -angle);
        }
        return slash.build();
    }
}
