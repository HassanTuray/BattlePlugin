package com.hfturay.battleplugin.ability.abilities.waterabilities;

import java.util.LinkedHashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.tempblockservice.BlockPos;
import com.hfturay.battleplugin.tempblockservice.TempBlocks;

/**
 * The look of a flying water block: real water in every block it passed through this tick, which removes itself after
 * a few ticks. Shared by every water move that sends a block through the air.
 */
final class WaterTrail {
    private WaterTrail() {}

    /** Edge-to-edge sampling step along the path, in blocks. Half a block never skips a cell. */
    private static final double SAMPLE = 0.5;

    /** Never fill the caster's own space: anything this close to their eyes (squared) is left alone. */
    private static final double CASTER_CLEARANCE_SQ = 2.25;

    /** Places water from `from` (exclusive) to `to` (inclusive). The returned set expires after `ticks`. */
    static TempBlocks leave(BattlePlayer owner, Location from, Location to, int ticks) {
        Vector step = to.toVector().subtract(from.toVector());
        int samples = Math.max(1, (int) Math.ceil(step.length() / SAMPLE));
        Location eye = owner.getPlayer().getEyeLocation();

        Set<BlockPos> cells = new LinkedHashSet<>();
        for (int i = 1; i <= samples; i++) {
            Location at = from.clone().add(step.clone().multiply((double) i / samples));
            if (at.distanceSquared(eye) > CASTER_CLEARANCE_SQ) {
                cells.add(BlockPos.of(at));
            }
        }
        return BattlePlugin.getInstance().getTempBlockService().place(cells, Material.WATER.createBlockData(), ticks);
    }
}
