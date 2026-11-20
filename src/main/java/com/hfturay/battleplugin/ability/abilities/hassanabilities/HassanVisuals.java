package com.hfturay.battleplugin.ability.abilities.hassanabilities;

import org.bukkit.Material;

import com.hfturay.battleplugin.visualservice.BlockModel;
import com.hfturay.battleplugin.visualservice.VisualSpec;

/** Hassan's block-based effects, one constant per effect. */
final class HassanVisuals {
    private HassanVisuals() {}

    // ---- Joga Bonito ----

    /**
     * The soccer ball: two white cubes, one turned 45 degrees so together they make a rounded, many-sided body, with
     * black patches poking out of it. About 0.4 blocks across. It does not spin.
     */
    static final VisualSpec SOCCER_BALL = BlockModel.builder()
        .part(Material.WHITE_CONCRETE, 0, 0, 0, 0.34, 0.34, 0.34)
        .part(Material.WHITE_CONCRETE, 0, 0, 0, 0.34, 0.34, 0.34, 45)
        .part(Material.BLACK_CONCRETE, 0, 0.19, 0, 0.14, 0.04, 0.14)       // top
        .part(Material.BLACK_CONCRETE, 0, 0, 0.19, 0.14, 0.14, 0.04)       // front
        .part(Material.BLACK_CONCRETE, 0, 0, -0.19, 0.14, 0.14, 0.04)      // back
        .part(Material.BLACK_CONCRETE, 0.19, 0, 0, 0.04, 0.14, 0.14)       // right
        .part(Material.BLACK_CONCRETE, -0.19, 0, 0, 0.04, 0.14, 0.14)      // left
        .build();

    // ---- Flying Raijin ----

    private static final Material BLADE = Material.NETHERITE_BLOCK;           // dark metal
    private static final Material HANDLE = Material.IRON_BLOCK;
    private static final Material RING = Material.NETHERITE_BLOCK;

    /** Where the three blades start along the kunai (z), just in front of the handle. */
    private static final double BLADE_BASE = -0.05;

    /** The ring at the end of the handle: its middle, and its radius. */
    private static final double RING_Z = -0.60;
    private static final double RING_RADIUS = 0.13;

    /**
     * The marking arrow, shaped like Minato's three-pronged kunai: dark metal, a long middle blade with a shorter
     * blade either side of it pointing 30 degrees away, a handle, and an empty ring at the end of the handle. It is
     * flat, lying in the up-down plane, and points nose first along its flight. About 1.2 blocks long.
     *
     * Parts are described in the model's own space (+z is the way it points). The side blades and the ring are
     * stretched blocks tipped by tiltDegrees: a block tipped by an angle points (-sin, cos) in (up, forward).
     */
    static final VisualSpec RAIJIN_ARROW = buildKunai();

    private static VisualSpec buildKunai() {
        BlockModel.Builder kunai = BlockModel.builder();

        // the middle blade: a wide part, then a narrow tip
        kunai.part(BLADE, 0, 0, BLADE_BASE + 0.175, 0.03, 0.11, 0.35);
        kunai.part(BLADE, 0, 0, BLADE_BASE + 0.45, 0.03, 0.05, 0.20);

        // the two side blades, 30 degrees either side of the middle one, from the same base
        double length = 0.40;
        double angle = Math.toRadians(30);
        for (int side = -1; side <= 1; side += 2) {
            double tilt = side * 30;                                           // positive = nose down
            double up = -Math.sin(angle) * side * length / 2;
            double forward = BLADE_BASE + Math.cos(angle) * length / 2;
            kunai.part(BLADE, 0, up, forward, 0.03, 0.08, length, tilt);
        }

        // the handle, with a thicker grip in the middle of it
        kunai.part(HANDLE, 0, 0, -0.26, 0.05, 0.05, 0.42);
        kunai.part(HANDLE, 0, 0, -0.26, 0.07, 0.07, 0.20);

        // the ring: eight short pieces around a circle, each turned to run along it, leaving the middle empty
        int pieces = 8;
        double chord = 2 * RING_RADIUS * Math.sin(Math.PI / pieces) + 0.035;
        for (int i = 0; i < pieces; i++) {
            double around = i * 360.0 / pieces;
            double radians = Math.toRadians(around);
            kunai.part(RING, 0, RING_RADIUS * Math.cos(radians), RING_Z + RING_RADIUS * Math.sin(radians),
                0.035, 0.035, chord, around);
        }

        return kunai.build();
    }
}
